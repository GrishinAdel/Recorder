package com.adelvanchik.myrecords.screens

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adelvanchik.myrecords.database.RecordDatabase
import com.adelvanchik.myrecords.database.RecordDatabaseDao
import com.adelvanchik.myrecords.database.RecordingItem
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var mDatabase: RecordDatabaseDao

    private val mJob = Job()
    private val mUiScope = CoroutineScope(Dispatchers.Main + mJob)

    private val second: Long = 100L

    private var prefs = application.getSharedPreferences(
        "com.adelvanchik.myrecords",
        Context.MODE_PRIVATE)

    private val _elapsedTime = MutableLiveData<String>()
    val elapsedTime: LiveData<String>
        get() = _elapsedTime

    private var _recordDatabase: LiveData<MutableList<RecordingItem>>
    val recordDatabase: LiveData<MutableList<RecordingItem>>
        get() = _recordDatabase

    private lateinit var timer: CountDownTimer

    init {
        createTimer()
        mDatabase = RecordDatabase.getInstance(application).recordDatabaseDao
        _recordDatabase = mDatabase.getAllRecords()
    }

    fun changeNameRecording(recordingItem: RecordingItem, name: String) {
        mUiScope.launch {
            withContext(Dispatchers.IO) {
                mDatabase.update(recordingItem.copy(name = name))
            }
        }
    }

    fun deleteRecording(idRecord: Long) {
        mUiScope.launch {
            withContext(Dispatchers.IO) {
                mDatabase.removeRecord(idRecord)
            }
        }
    }


    fun timeFormatter(time: Long): String {
        return if (TimeUnit.MILLISECONDS.toHours(time) % 60 > 0) {
            String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(time) % 60,
                TimeUnit.MILLISECONDS.toMinutes(time) % 60,
                TimeUnit.MILLISECONDS.toSeconds(time) % 60)
        } else  String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(time) % 60,
            TimeUnit.MILLISECONDS.toSeconds(time) % 60)
    }

    fun startTimer() {
        val triggerTime = SystemClock.elapsedRealtime()

        viewModelScope.launch {
            saveTime(triggerTime)
            createTimer()
        }
    }

    fun stopTimer() {
        if (this::timer.isInitialized) {
            timer.cancel()
        }
        resetTimer()
    }

    private fun createTimer() {
        viewModelScope.launch {
            val triggerTime = loadTime()
            timer = object : CountDownTimer(triggerTime, second) {
                override fun onTick(millisUntilFinished: Long) {
                    _elapsedTime.value = timeFormatter(SystemClock.elapsedRealtime() - triggerTime)
                }

                override fun onFinish() {
                    resetTimer()
                }
            }
            timer.start()
        }
    }

    fun resetTimer() {
        _elapsedTime.value = timeFormatter(0)
        viewModelScope.launch { saveTime(0) }
    }

    private suspend fun saveTime(triggerTime: Long) =
        withContext(Dispatchers.IO) {
            prefs.edit().putLong(TRIGGER_TIME, triggerTime).apply()
        }

    private suspend fun loadTime(): Long =
        withContext(Dispatchers.IO) {
            prefs.getLong(TRIGGER_TIME, 0)
        }

    companion object {
        private const val TRIGGER_TIME = "TRIGGER_AT"
    }

}