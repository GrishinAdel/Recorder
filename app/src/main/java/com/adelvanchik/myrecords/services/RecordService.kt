package com.adelvanchik.myrecords.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.adelvanchik.myrecords.screens.MainActivity
import com.adelvanchik.myrecords.R
import com.adelvanchik.myrecords.database.RecordDatabase
import com.adelvanchik.myrecords.database.RecordDatabaseDao
import com.adelvanchik.myrecords.database.RecordingItem
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RecordService : Service() {

    private var mFileName: String? = null
    private var mFilePath: String? = null

    private var mRecorder: MediaRecorder? = null

    private var mStartingTimeMillis: Long = 0
    private var mElapsedMillis: Long = 0

    private var mDatabase: RecordDatabaseDao? = null

    private val mJob = Job()
    private val mUiScope = CoroutineScope(Dispatchers.Main + mJob)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mDatabase = RecordDatabase.getInstance(this).recordDatabaseDao
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startRecording()
        return START_NOT_STICKY
    }

    private fun startRecording() {
        setFileNameAndPath()
        mRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(applicationContext)
        } else {
            MediaRecorder()
        }
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mRecorder?.setOutputFile(mFilePath)
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder?.setAudioChannels(1)
        mRecorder?.setAudioEncodingBitRate(192000)

        try {
            mRecorder?.prepare()
            mRecorder?.start()
            mStartingTimeMillis = System.currentTimeMillis()
            startForeground(1, createNotification())
        } catch (e: IOException) {
            Log.e("RecordService", "prepare failed")
        }
    }

    private fun createNotification(): Notification {
        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext,
            getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_mic_white)
            .setContentText(getString(R.string.notification_recording))
            .setOngoing(true)
        mBuilder.setContentIntent(
            PendingIntent.getActivities(
                applicationContext, 0, arrayOf(
                    Intent(
                        applicationContext,
                        MainActivity::class.java
                    )
                ), 0
            )
        )
        return mBuilder.build()
    }

    private fun setFileNameAndPath() {
        var count = 0
        var f: File
        val dateTime = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
            .format(System.currentTimeMillis())

        do {
            mFileName = (getString(R.string.default_file_name)
                    + "_" + dateTime + count)
            mFilePath = application.getExternalFilesDir(null)?.absolutePath
            mFilePath += "/$mFileName"

            count++

            f = File(mFilePath!!)
        } while (f.exists() && !f.isDirectory)
    }

    private fun stopRecording() {

        mRecorder?.stop()
        mElapsedMillis = System.currentTimeMillis() - mStartingTimeMillis
        mRecorder?.release()
        Toast.makeText(this,
            getString(R.string.recording_finish),
            Toast.LENGTH_SHORT
        ).show()


        val correctTime = Date(System.currentTimeMillis())
        val data = SimpleDateFormat("dd.MM.yyyy")
        val time = SimpleDateFormat("HH:mm")

        val recordingItem = RecordingItem(
            name = mFileName.toString(),
            filePath = mFilePath.toString(),
            length = mElapsedMillis,
            time = time.format(correctTime),
            data = data.format(correctTime)
        )

        mRecorder = null

        mUiScope.launch {
            withContext(Dispatchers.IO) {
                mDatabase?.insert(recordingItem)
            }
        }


    }

    override fun onDestroy() {
        if (mRecorder != null) {
            stopRecording()
        }
        super.onDestroy()
    }
}



















