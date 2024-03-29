package com.example.calllogprovider


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CallLog
import android.view.View
import android.widget.Button

import android.widget.ListView
import android.widget.Toast

import android.widget.SimpleCursorAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import java.text.SimpleDateFormat
import java.util.*

import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    lateinit var btnDatePicker: FloatingActionButton
    private var selectedStartDate: Long = 0
    private var selectedEndDate: Long = 0
    private var numRecords = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectedStartDate = 0
        selectedEndDate = Long.MAX_VALUE
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                Array(1) { Manifest.permission.READ_CALL_LOG },
                101
            )
        } else {
            displayLog()
        }


        btnDatePicker = findViewById(R.id.floatingActionButton)

        val btnExport = findViewById<Button>(R.id.xlsbutton)
        btnExport.setOnClickListener {

            exportCallLogToExcel()
        }


        val btnRefresh = findViewById<Button>(R.id.Refreshbutton)
        btnRefresh.setOnClickListener {
            selectedStartDate = 0
            selectedEndDate = Long.MAX_VALUE
            displayLog()
        }


        btnDatePicker.setOnClickListener {

            val datePicker = MaterialDatePicker.Builder.dateRangePicker().build()
            datePicker.show(supportFragmentManager, "DatePicker")

            // Setting up the event for when ok is clicked
            datePicker.addOnPositiveButtonClickListener { selection ->
                // Toast.makeText(this, "${datePicker.headerText} is selected", Toast.LENGTH_LONG).show()


                val startDate = selection.first
                var endDate = selection.second


                if (startDate != null && endDate != null) {
                    selectedStartDate = startDate
                    endDate += (24 * 60 * 60 * 1000)
                    selectedEndDate = endDate

                    displayLog()
                } else
                    Toast.makeText(this, "Proszę wybrać zakres dat", Toast.LENGTH_SHORT).show()


            }

            // Setting up the event for when cancelled is clicked
            datePicker.addOnNegativeButtonClickListener {
                Toast.makeText(this, "${datePicker.headerText} is cancelled", Toast.LENGTH_LONG)
                    .show()
            }

            // Setting up the event for when back button is pressed
            datePicker.addOnCancelListener {
                Toast.makeText(this, "Date Picker Cancelled", Toast.LENGTH_LONG).show()
            }
        }


    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            displayLog()
        } else if (requestCode == 102 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            exportCallLogToExcel()
        }
    }


    private fun exportCallLogToExcel() {


        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val cols = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )
        val selection = "${CallLog.Calls.DATE} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(selectedStartDate.toString(), selectedEndDate.toString())

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            cols,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )


        if (cursor != null && cursor.moveToFirst()) {
            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet("Call Log")
            val headerRow = sheet.createRow(0)

            // Dodawanie nagłówków do arkusza
            val headerNames = arrayOf(
                "Name", "Number", "Type", "Duration(sek)", "Date"
            )
            for (i in headerNames.indices) {
                headerRow.createCell(i).setCellValue(headerNames[i])
            }

            var rowNum = 1
            do {
                val row = sheet.createRow(rowNum++)
                for (i in cols.indices) {
                    val cell = row.createCell(i)
                    when (cols[i]) {
                        CallLog.Calls.DATE -> {
                            val callDate = cursor.getLong(i)
                            val formattedDate = dateFormat.format(Date(callDate))
                            cell.setCellValue(formattedDate)
                        }

                        CallLog.Calls.TYPE -> {
                            val callType = cursor.getInt(i)
                            val callTypeString = getCallTypeString(callType)
                            cell.setCellValue(callTypeString)

                        }

                        else -> {
                            val value = when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(i).toString()
                                else -> ""
                            }
                            cell.setCellValue(value)
                        }
                    }
                }
            } while (cursor.moveToNext())

            cursor.close()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ActivityCompat.requestPermissions(this, permissions, 102)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        102
                    )
                }
            } else {
                saveExcelFile(workbook)
            }
        }
    }

    private fun saveExcelFile(workbook: HSSFWorkbook) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())
        val fileName = "CallLog_$currentDateTime.xls"

        val externalDir = getExternalFilesDir(null)
        val filePath = File(externalDir, fileName)

        val fileOut = FileOutputStream(filePath)
        workbook.write(fileOut)
        fileOut.close()

        Toast.makeText(this, "Dane eksportowane do pliku Excel", Toast.LENGTH_SHORT).show()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/vnd.ms-excel"
        val fileUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            filePath
        )
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
        startActivity(Intent.createChooser(shareIntent, "Wyślij plik Excel"))
    }


    @SuppressLint("MissingPermission", "Range")
    private fun displayLog() {
        val cols = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION, CallLog.Calls.DATE
        )
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (selectedStartDate > 0 && selectedEndDate > 0) {
            selection = "${CallLog.Calls.DATE} BETWEEN ? AND ?"
            selectionArgs = arrayOf(
                selectedStartDate.toString(),
                selectedEndDate.toString()
            )
        }

        val rs = contentResolver.query(
            CallLog.Calls.CONTENT_URI, cols, selection, selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )






        val from = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )

        val adapter = SimpleCursorAdapter(
            this,
            R.layout.mylayout,
            rs,
            from,
            intArrayOf(
                R.id.textView1,
                R.id.textView2,
                R.id.textView3,
                R.id.textView4,
                R.id.textView5
            ),
            0

        )

        adapter.setViewBinder { view, cursor, columnIndex ->
            if (view.id == R.id.textView1) { // Nazwa kontaktu
                val contactName = cursor.getString(columnIndex)
                (view as TextView).text = contactName
                true
            } else if (view.id == R.id.textView3) {
                val callType = cursor.getInt(columnIndex)
                val callTypeString = getCallTypeString(callType)
                (view as TextView).text = callTypeString
                true
            } else if (view.id == R.id.textView4) {
                val callDuration = cursor.getLong(columnIndex)
                val durationString = "$callDuration sek"
                (view as TextView).text = durationString
                true
            } else if (view.id == R.id.textView5) {
                val callDate = cursor.getLong(columnIndex)
                val formattedDate = dateFormat.format(Date(callDate))
                (view as TextView).text = formattedDate
                true
            } else {
                false
            }

        }

        val listview = findViewById<ListView>(R.id.listView)
        listview.adapter = adapter

        numRecords = adapter.count

        // Ustal widoczność pola tekstowego w zależności od dostępnych danych
        val noDataTextView = findViewById<TextView>(R.id.noDataTextView)
        if (numRecords == 0) {
            noDataTextView.visibility = View.VISIBLE
        } else {
            noDataTextView.visibility = View.GONE
        }

        // Wyświetl Toast z informacją o ilości pobranych rekordów
        val toastText = "Pobrano $numRecords rekordów"
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
    }




    private fun getCallTypeString(callType: Int): String {
        return when (callType) {
            CallLog.Calls.INCOMING_TYPE -> "Przychodzące"
            CallLog.Calls.OUTGOING_TYPE -> "Wychodzące"
            CallLog.Calls.MISSED_TYPE -> "Nieodebrane"
            CallLog.Calls.REJECTED_TYPE -> "Odrzucone"
            CallLog.Calls.BLOCKED_TYPE -> "Zablokowane"
            CallLog.Calls.VOICEMAIL_TYPE -> "Poczta głosowa"
            CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "Odpowiedziane zewnętrznie"
            else -> "Nieznany"

        }

    }
}












