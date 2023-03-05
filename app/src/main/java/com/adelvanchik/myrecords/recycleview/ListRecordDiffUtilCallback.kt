package com.adelvanchik.myrecords.recycleview

import androidx.recyclerview.widget.DiffUtil
import com.adelvanchik.myrecords.database.RecordingItem

class ListRecordDiffUtilCallback: DiffUtil.ItemCallback<RecordingItem>() {
    override fun areItemsTheSame(oldItem: RecordingItem, newItem: RecordingItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RecordingItem, newItem: RecordingItem): Boolean {
        return oldItem == newItem
    }
}