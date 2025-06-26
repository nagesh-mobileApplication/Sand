package com.sand

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editOrderId: EditText
    private lateinit var editTripNo: EditText
    private lateinit var editCustomerName: EditText
    private lateinit var editCustomerMobile: EditText
    private lateinit var editSandQuantity: EditText
    private lateinit var qrCodeLink: EditText
    private lateinit var editDriverName: EditText
    private lateinit var editDriverMobile: EditText
    private lateinit var editVehicleNo: EditText
    private lateinit var editAddress: EditText
    private lateinit var printButton: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            printBill()
        } else {
            Toast.makeText(this, "Bluetooth permission is required to print", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        editOrderId = findViewById(R.id.editOrderId)
        editTripNo = findViewById(R.id.editTripNo)
        editCustomerName = findViewById(R.id.editCustomerName)
        editCustomerMobile = findViewById(R.id.editCustomerMobile)
        editSandQuantity = findViewById(R.id.editSandQuantity)
        qrCodeLink = findViewById(R.id.qr_code_link)
        editDriverName = findViewById(R.id.editDriverName)
        editDriverMobile = findViewById(R.id.editDriverMobile)
        editVehicleNo = findViewById(R.id.editVehicleNo)
        editAddress = findViewById(R.id.editAddress)
        printButton = findViewById(R.id.printButton)


        printButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+: Check BLUETOOTH_CONNECT permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    printBill()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            } else {
                // Android <12: No need for runtime permission
                printBill()
            }
        }
    }

    private fun printBill() {
        val orderId = editOrderId.text.toString()
        val tripNo = editTripNo.text.toString()
        val customerName = editCustomerName.text.toString()
        val customerMobile = editCustomerMobile.text.toString()
        val sandQuantity = editSandQuantity.text.toString()
        val qrCodeLink = qrCodeLink.text.toString()
        val driverName = editDriverName.text.toString()
        val driverMobile = editDriverMobile.text.toString()
        val vehicleNo = editVehicleNo.text.toString()
        val address = editAddress.text.toString()

        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = formatter.format(calendar.time)

        if (orderId.isEmpty() || tripNo.isEmpty() || customerName.isEmpty() ||
            customerMobile.isEmpty() || sandQuantity.isEmpty() || qrCodeLink.isEmpty() ||
            driverName.isEmpty() || driverMobile.isEmpty() || vehicleNo.isEmpty() || address.isEmpty()
        ) {
            Toast.makeText(this, "Please fill in all fields before printing", Toast.LENGTH_LONG).show()
            return
        }

        try {
            BluetoothPrinterHelper(this).printBill(
                orderId,
                tripNo,
                customerName,
                customerMobile,
                sandQuantity,
                formattedDate,
                driverName,
                driverMobile,
                vehicleNo,
                address,
                qrCodeLink
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Permission denied to access Bluetooth", Toast.LENGTH_LONG).show()
        }
    }
}

