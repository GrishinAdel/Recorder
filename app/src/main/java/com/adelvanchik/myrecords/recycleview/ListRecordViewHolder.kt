package com.adelvanchik.myrecords.recycleview

import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adelvanchik.myrecords.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListRecordViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val tvNameRecord: TextView = view.findViewById(R.id.tv_name_record)
    val tvDataTimeRecord: TextView = view.findViewById(R.id.tv_data_time_record)
    val tvLongTimeRecord: TextView = view.findViewById(R.id.tv_long_time_record)
    val tvCorrectTimeRecord: TextView = view.findViewById(R.id.tv_correct_time)
    val tvSlashRecord: TextView = view.findViewById(R.id.tv_slash)
    val btnPlayPauseRecord: FloatingActionButton = view.findViewById(R.id.btn_play_pause_record)
    val sbRecording: SeekBar = view.findViewById(R.id.pb_recording)
    val layout: View = view.findViewById(R.id.layout)
}