package com.kelompok3.bukutamu.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kelompok3.bukutamu.R
import com.kelompok3.bukutamu.databinding.ItemDailyRecapBinding
import com.kelompok3.bukutamu.ui.viewmodel.GuestViewModel

/**
 * Adapter untuk menampilkan baris riwayat rekap harian di dalam permukaan terkelompok pada Dashboard.
 */
class DailyRecapAdapter :
    ListAdapter<GuestViewModel.DailyRecap, DailyRecapAdapter.RecapViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecapViewHolder {
        val binding = ItemDailyRecapBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecapViewHolder, position: Int) {
        val isLastItem = position == itemCount - 1
        holder.bind(getItem(position), isLastItem)
    }

    class RecapViewHolder(
        private val binding: ItemDailyRecapBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GuestViewModel.DailyRecap, isLastItem: Boolean) {
            binding.tvRecapDate.text = item.dateText
            binding.tvRecapCount.text = binding.root.context.getString(
                R.string.value_guest_count,
                item.count
            )
            binding.dividerRecap.visibility = if (isLastItem) View.GONE else View.VISIBLE
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GuestViewModel.DailyRecap>() {
        override fun areItemsTheSame(
            oldItem: GuestViewModel.DailyRecap,
            newItem: GuestViewModel.DailyRecap
        ): Boolean = oldItem.dayStartMillis == newItem.dayStartMillis

        override fun areContentsTheSame(
            oldItem: GuestViewModel.DailyRecap,
            newItem: GuestViewModel.DailyRecap
        ): Boolean = oldItem == newItem
    }
}

