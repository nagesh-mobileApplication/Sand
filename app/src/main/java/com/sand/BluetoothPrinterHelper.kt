package com.sand

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.*
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.set
import java.io.OutputStream
import java.util.*
import kotlin.experimental.or

class BluetoothPrinterHelper(private val context: Context) {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun printBill(
        orderId: String,
        tripNo: String,
        customerName: String,
        customerMobile: String,
        sandQuantity: String,
        dispatchDate: String,
        driverName: String,
        driverMobile: String,
        vehicleNo: String,
        address: String,
        qrCodeLink: String
    ) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth is not available or not enabled", Toast.LENGTH_SHORT).show()
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices.find {
            it.name.contains("MLP 3120_2E89", ignoreCase = true) || it.name.contains("MLP 3120_3155", ignoreCase = true) || it.name.contains("Printer", ignoreCase = true)
        }

        if (device == null) {
            Toast.makeText(context, "No paired printer found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uuid: UUID = device.uuids[0].uuid
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            val outputStream: OutputStream = socket.outputStream

            val line = "------------------------------------------------"
            val headerBitmap = createHeaderBitmap(context)
            val headerBytes = convertBitmapToMonochromeEscPos(headerBitmap)

            outputStream.write("\n\n".toByteArray())

            // -------------------- Consumer Copy --------------------
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center align
            outputStream.write(headerBytes)
            outputStream.write("Consumer Copy\n\n".toByteArray())

            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left align
            outputStream.write("$line\n".toByteArray())
            val consumerDetails = buildString {
                appendLine(formatKeyValue("Order Id", orderId))
                appendLine(formatKeyValue("Trip No", tripNo))
                appendLine(formatKeyValue("Customer Name", customerName))
                appendLine(formatKeyValue("Customer Mobile", customerMobile))
                appendLine(formatKeyValue("Sand Quantity:", sandQuantity))
                appendLine(formatKeyValue("Sand Supply Point Name", "Narayananellore MSP"))
                appendLine(formatKeyValue("Dispatch Date", dispatchDate))
                appendLine(formatKeyValue("Driver Name", driverName))
                appendLine(formatKeyValue("Driver Mobile", driverMobile))
                appendLine(formatKeyValue("Vehicle No", vehicleNo))
            }

            outputStream.write((consumerDetails + "\n").toByteArray())
            printCenteredAddress(outputStream, address)
            outputStream.write("$line\n\n".toByteArray())

            // -------------------- Driver Copy --------------------
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center align
            outputStream.write(headerBytes)
            outputStream.write("Driver Copy\n\n".toByteArray())

            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left align
            outputStream.write("$line\n".toByteArray())
            val driverDetails = buildString {
                appendLine(formatKeyValue("Order Id", orderId))
                appendLine(formatKeyValue("Trip No", tripNo))
                appendLine(formatKeyValue("Customer Name", customerName))
                appendLine(formatKeyValue("Customer Mobile", customerMobile))
                appendLine(formatKeyValue("Sand Quantity:", sandQuantity))
                appendLine(formatKeyValue("Sand Supply Point Name", "Narayananellore MSP"))
                appendLine(formatKeyValue("Dispatch Date", dispatchDate))
                appendLine(formatKeyValue("Driver Name", driverName))
                appendLine(formatKeyValue("Driver Mobile", driverMobile))
                appendLine(formatKeyValue("Vehicle No", vehicleNo))
            }
            outputStream.write((driverDetails + "\n").toByteArray())
            printCenteredAddress(outputStream, address)
            outputStream.write("$line\n\n".toByteArray())

            // -------------------- QR Code --------------------
            val qrBitmap = generateQRCode(qrCodeLink, 200)
            val qrBytes = convertBitmapToMonochromeEscPos(qrBitmap)
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center align
            outputStream.write(qrBytes)
            outputStream.write("\n\n".toByteArray())

            // -------------------- Thank You --------------------
            outputStream.write(byteArrayOf(0x1B, 0x21, 0x20)) // Double height font
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center align
            outputStream.write("Thank You\n\n".toByteArray(Charsets.UTF_8))
            outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // Reset font

            outputStream.flush()
            outputStream.close()
            socket.close()

            Toast.makeText(context, "Printed Successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Print Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createHeaderBitmap(context: Context): Bitmap {
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ap_sand)
        val resizedLogo = logo.scale(100, 60, filter = true)

        val paintLarge = Paint().apply {
            isAntiAlias = true
            textSize = 30f
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }

        val paintSmall = Paint().apply {
            isAntiAlias = true
            textSize = 15f
            color = Color.BLACK
            typeface = Typeface.DEFAULT
        }

        val text1 = "AP SAND"
        val text2 = "MANAGEMENT SYSTEM"
        val text1Width = paintLarge.measureText(text1)
        val text2Width = paintSmall.measureText(text2)
        val textWidth = maxOf(text1Width, text2Width).toInt()
        val spacing = 4
        val width = resizedLogo.width + spacing + textWidth
        val height = maxOf(resizedLogo.height, 70)

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(resizedLogo, 0f, (height - resizedLogo.height) / 2f, null)

        val textX = resizedLogo.width + spacing.toFloat()
        canvas.drawText(text1, textX, 30f, paintLarge)
        canvas.drawText(text2, textX, 52f, paintSmall)

        return bitmap
    }

    private fun convertBitmapToMonochromeEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val imageData = ByteArray(height * bytesPerRow)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val gray = (r + g + b) / 3
                if (gray < 128) {
                    imageData[y * bytesPerRow + (x / 8)] =
                        (imageData[y * bytesPerRow + (x / 8)] or (0x80 shr (x % 8)).toByte())
                }
            }
        }

        val result = ByteArray(8 + imageData.size)
        result[0] = 0x1D
        result[1] = 0x76
        result[2] = 0x30
        result[3] = 0x00
        result[4] = (bytesPerRow and 0xFF).toByte()
        result[5] = ((bytesPerRow shr 8) and 0xFF).toByte()
        result[6] = (height and 0xFF).toByte()
        result[7] = ((height shr 8) and 0xFF).toByte()
        System.arraycopy(imageData, 0, result, 8, imageData.size)

        return result
    }

    private fun generateQRCode(content: String, size: Int = 250): Bitmap {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bmp
    }

    private fun printCenteredAddress(outputStream: OutputStream, address: String) {
        val fullAddress = "Address: $address"
        val addressLines = splitTextByLength(fullAddress, 42)
        for (line in addressLines) {
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center align
            outputStream.write("$line\n".toByteArray())
        }
        outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Reset to left
    }

    private fun splitTextByLength(text: String, maxLength: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if ((currentLine + word).length <= maxLength) {
                currentLine += if (currentLine.isEmpty()) word else " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun formatKeyValue(label: String, value: String, totalWidth: Int = 42): String {
        val formattedLabel = label.padEnd(20) // Adjust label width
        val spaceForValue = totalWidth - formattedLabel.length
        val alignedValue = value.padStart(spaceForValue.coerceAtLeast(0))
        return formattedLabel + alignedValue
    }
}
