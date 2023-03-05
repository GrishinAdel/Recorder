package com.adelvanchik.myrecords.recycleview

import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import com.adelvanchik.myrecords.R
import com.adelvanchik.myrecords.database.RecordingItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ListRecordAdapter :
    ListAdapter<RecordingItem, ListRecordViewHolder>(ListRecordDiffUtilCallback()) {

    var changeRecording: ((recordItem: RecordingItem, name: String) -> Unit)? = null

    lateinit var timer: CountDownTimer

    private var mMediaPlayer: MediaPlayer? = null
    private var lastPlayItemRecording: RecordingItem? = null
    private var lastPlayRecordingViewHolder: ListRecordViewHolder? = null
    private var correctPositionInRecording = 0
    private var saveTimerSecond = 0L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListRecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.pattern_one_record_for_recycle_view,
            parent,
            false
        )
        return ListRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListRecordViewHolder, position: Int) {
        val record = getItem(position)
        val stringIn = holder.itemView.context.getString(R.string.inString)
        val stringToday = holder.itemView.context.getString(R.string.today)
        with(holder) {
            tvNameRecord.text = record.name
            tvLongTimeRecord.text = timeFormatter(record.length)

            val correctTime = Date(System.currentTimeMillis())
            val data = SimpleDateFormat("dd.MM.yyyy")
            tvDataTimeRecord.text = if (data.format(correctTime) == record.data) {
                "$stringToday $stringIn ${record.time}"
            } else "${record.data} $stringIn ${record.time}"

            layout.setOnClickListener {
                Log.e("onBindViewHolder", "Run")
                Log.e("onBindViewHolder", changeRecording.toString())
                changeRecording?.invoke(record, record.name)
            }
            btnPlayPauseRecord.setOnClickListener {

                resetLastPlayRecording()

                tvSlashRecord.visibility = View.VISIBLE
                tvCorrectTimeRecord.visibility = View.VISIBLE
                sbRecording.visibility = View.VISIBLE

                if (!isThisRecording(holder, record)) {
                    (it as FloatingActionButton).setImageResource(R.drawable.ic_pause_record)
                    playRecord(record.filePath)
                    createTimer(tvCorrectTimeRecord)
                    saveCorrectRecordingInLastRecording(record, holder)
                } else {
                    if (mMediaPlayer!!.isPlaying) {
                        (it as FloatingActionButton)
                            .setImageResource(R.drawable.ic_play_record)
                        pauseRecord()
                    } else {
                        (it as FloatingActionButton)
                            .setImageResource(R.drawable.ic_pause_record)
                        nextPlayRecord()
                        createTimer(tvCorrectTimeRecord)
                    }

                }
                setupProgressBar(record.length)
            }

        }
    }

    private fun ListRecordViewHolder.setupProgressBar(length: Long) {
        sbRecording.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean,
            ) {
                if (fromUser && mMediaPlayer!!.isPlaying) {
                    mMediaPlayer?.seekTo(progress)
                    timer.cancel()
                    saveTimerSecond = SystemClock.elapsedRealtime() - length * progress / 10000
                    createTimer(tvCorrectTimeRecord)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        sbRecording.max = mMediaPlayer!!.duration
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        runnable = Runnable {
            if (mMediaPlayer != null) sbRecording.progress = mMediaPlayer!!.currentPosition
            handler.postDelayed(runnable!!, 10)
        }
        handler.postDelayed(runnable, 10)

        mMediaPlayer?.setOnCompletionListener {
            btnPlayPauseRecord.setImageResource(R.drawable.ic_play_record)
            stopRecord()
            resetLastPlayRecording()
        }
    }

    private fun saveCorrectRecordingInLastRecording(
        record: RecordingItem?,
        holder: ListRecordViewHolder,
    ) {
        lastPlayItemRecording = record
        lastPlayRecordingViewHolder = holder
    }

    private fun playRecord(record: String) {

        if (mMediaPlayer != null) {
            if (mMediaPlayer!!.isPlaying) stopRecord()
        }

        saveTimerSecond = 0L

        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setDataSource(record)
        mMediaPlayer!!.prepare()
        mMediaPlayer!!.start()

    }

    private fun nextPlayRecord() {
        mMediaPlayer!!.seekTo(correctPositionInRecording)
        mMediaPlayer!!.start()
        saveTimerSecond = SystemClock.elapsedRealtime() - saveTimerSecond
    }

    private fun pauseRecord() {
        correctPositionInRecording = mMediaPlayer!!.currentPosition
        if (mMediaPlayer?.isPlaying == true) mMediaPlayer?.pause()
        timer.cancel()
    }

    private fun stopRecord() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
        saveTimerSecond = 0L
        timer.cancel()
    }

    private fun resetLastPlayRecording() {
        if (lastPlayItemRecording != null && lastPlayRecordingViewHolder != null) {
            with(lastPlayRecordingViewHolder!!) {
                tvSlashRecord.visibility = View.INVISIBLE
                tvCorrectTimeRecord.visibility = View.INVISIBLE
                sbRecording.visibility = View.INVISIBLE
                btnPlayPauseRecord.setImageResource(R.drawable.ic_play_record)
            }
        }
    }

    private fun isThisRecording(holder: ListRecordViewHolder, recordItem: RecordingItem): Boolean {
        if (lastPlayItemRecording == null || lastPlayRecordingViewHolder == null) return false
        if (holder == lastPlayRecordingViewHolder && recordItem == lastPlayItemRecording
            && mMediaPlayer != null
        ) {
            return true
        }
        return false

    }

    fun timeFormatter(time: Long): String {
        return if (TimeUnit.MILLISECONDS.toHours(time) % 60 > 0) {
            String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(time) % 60,
                TimeUnit.MILLISECONDS.toMinutes(time) % 60,
                TimeUnit.MILLISECONDS.toSeconds(time) % 60)
        } else String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(time) % 60,
            TimeUnit.MILLISECONDS.toSeconds(time) % 60)
    }

    private fun createTimer(tvCorrectTimeRecord: TextView) {
        val startTimerSecond = loadTime()

        timer = object : CountDownTimer(startTimerSecond, DEFAULT_SECOND) {
            override fun onTick(millisUntilFinished: Long) {
                val time = SystemClock.elapsedRealtime() - startTimerSecond
                tvCorrectTimeRecord.text = timeFormatter(time)
                saveTimerSecond = time
            }

            override fun onFinish() {
            }
        }
        timer.start()
    }

    private fun loadTime(): Long {
        return if (saveTimerSecond == 0L) SystemClock.elapsedRealtime()
        else saveTimerSecond
    }

    companion object {
        private const val DEFAULT_SECOND = 100L
    }

}