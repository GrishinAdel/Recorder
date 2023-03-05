package com.adelvanchik.myrecords.screens

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.adelvanchik.myrecords.R
import com.adelvanchik.myrecords.database.RecordDatabase
import com.adelvanchik.myrecords.database.RecordDatabaseDao
import com.adelvanchik.myrecords.database.RecordingItem
import com.adelvanchik.myrecords.databinding.ActivityMainBinding
import com.adelvanchik.myrecords.recycleview.ListRecordAdapter
import com.adelvanchik.myrecords.services.RecordService
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var listRecordAdapter: ListRecordAdapter

    private val viewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding ?: throw RuntimeException("ActivityMainBinding == null")

    private var database: RecordDatabaseDao? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = RecordDatabase.getInstance(this).recordDatabaseDao

        checkWorkRecordingService()
        actionButtonClickListener()
        setupRecycleView()
        observers()

        createChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name)
        )
        implementChangeRecordingMethodFromListRecordAdapter()
    }

    private fun implementChangeRecordingMethodFromListRecordAdapter() {
        listRecordAdapter.changeRecording = { recordItem, name ->
            editRecordingWithAlertDialog(recordItem, name)
        }
    }

    private fun editRecordingWithAlertDialog(
        recordItem: RecordingItem,
        name: String,
    ) {
        Log.e("onBindViewHolder", "Starting")
        val mDialogView = LayoutInflater.from(this).inflate(
            R.layout.alert_dialog_for_save_record, null
        )

        val mAlertDialog = AlertDialog.Builder(this)
            .setView(mDialogView).show()

        val btnDelete: Button = mDialogView.findViewById(R.id.btn_delete)
        val btnSave: Button = mDialogView.findViewById(R.id.btn_save)
        val editText: EditText = mDialogView.findViewById(R.id.et_alert_dialog)
        btnDelete.setOnClickListener {
            mAlertDialog.dismiss()
            viewModel.deleteRecording(recordItem.id)
        }
        btnSave.setOnClickListener {
            mAlertDialog.dismiss()
            viewModel.changeNameRecording(recordItem, editText.text.toString())
        }
        editText.setText(name)
    }

    private fun setupRecycleView() {
        listRecordAdapter = ListRecordAdapter()
        binding.rvRecords.adapter = listRecordAdapter
    }


    private fun observers() {
        viewModel.elapsedTime.observe(this) {
            if (it != "00:00" && it != "00:00:00") binding.tvTimeRecording.text = it
            else binding.tvTimeRecording.text = ""
        }
        viewModel.recordDatabase.observe(this) {
            listRecordAdapter.submitList(it)
        }
    }

    private fun actionButtonClickListener() {
        binding.btnActionMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_RECORD_AUDIO)
            } else {
                if (isServiceRunning()) {
                    stopRecordingAndTime()
                } else {
                    startRecordingAndTime()
                }
            }
        }
    }

    private fun startRecordingAndTime() {
        onRecord(true)
        viewModel.startTimer()
    }

    private fun stopRecordingAndTime() {
        onRecord(false)
        viewModel.stopTimer()
    }

    private fun checkWorkRecordingService() {
        if (!isServiceRunning()) {
            viewModel.resetTimer()
        } else {
            binding.btnActionMic.setImageResource(R.drawable.ic_stop_record)
        }
    }

    private fun onRecord(start: Boolean) {
        val serviceIntent = Intent(this@MainActivity, RecordService::class.java)
        if (start) {
            binding.btnActionMic.setImageResource(R.drawable.ic_stop_record)
            Toast.makeText(this, R.string.recording_start, Toast.LENGTH_SHORT).show()

            val folder =
                File(this.getExternalFilesDir(null)?.absolutePath
                    .toString() + "/MyRecord")
            if (!folder.exists()) {
                folder.mkdir()
            }
            startService(serviceIntent)
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            binding.btnActionMic.setImageResource(R.drawable.ic_mic_white)
            stopService(serviceIntent)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecordingAndTime()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.recording_permissions),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }

    private fun createChannel(channelId: String, channelName: String) {
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        }
        val notificationManager = this.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.adelvanchik.myrecords.services.RecordService" == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {

        private const val MY_PERMISSIONS_RECORD_AUDIO = 123

    }


}