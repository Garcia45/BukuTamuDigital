package com.kelompok3.bukutamu.ui.guest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kelompok3.bukutamu.R
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.databinding.ItemGuestBinding
import com.kelompok3.bukutamu.util.DateUtils
import java.util.Locale

/**
 * Adapter RecyclerView untuk menampilkan daftar tamu sesuai PRD 8.3 (FR-14, FR-15) & 11.4.
 * Menggunakan latar permukaan terkelompok yang menyesuaikan posisi item (single, top, middle, bottom)
 * sehingga kartu daftar tamu membungkus jumlah item sebenarnya secara presisi tanpa menyisakan ruang kosong.
 */
class GuestAdapter(
    private val onItemClick: (Guest) -> Unit,
    private val onDeleteClick: (Guest) -> Unit
) : ListAdapter<Guest, GuestAdapter.GuestViewHolder>(GuestDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val binding = ItemGuestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuestViewHolder, position: Int) {
        holder.bind(getItem(position), position, itemCount)
    }

    inner class GuestViewHolder(
        private val binding: ItemGuestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(guest: Guest, position: Int, totalCount: Int) {
            binding.tvGuestInitial.text = extractInitials(guest.nama)
            binding.tvGuestName.text = guest.nama
            binding.tvGuestPurpose.text = guest.keperluan
            binding.tvGuestAddress.text = guest.alamat
            binding.tvGuestTimestamp.text = DateUtils.formatTimestamp(guest.timestamp)

            val isLastItem = position == totalCount - 1
            val bgRes = when {
                totalCount <= 1 -> R.drawable.bg_grouped_row_single
                position == 0 -> R.drawable.bg_grouped_row_top
                isLastItem -> R.drawable.bg_grouped_row_bottom
                else -> R.drawable.bg_grouped_row_middle
            }
            binding.root.setBackgroundResource(bgRes)
            binding.dividerBottom.visibility = if (isLastItem) View.GONE else View.VISIBLE

            // Ketuk baris membuka Form Edit (FR-20, PRD 11.4)
            binding.root.setOnClickListener {
                onItemClick(guest)
            }

            // Tekan lama pada baris memunculkan dialog konfirmasi hapus (PRD 11.4)
            binding.root.setOnLongClickListener {
                onDeleteClick(guest)
                true
            }

            // Tombol hapus aksesibel (48dp target) memunculkan dialog konfirmasi hapus (FR-21)
            binding.btnDeleteGuest.setOnClickListener {
                onDeleteClick(guest)
            }
        }

        private fun extractInitials(name: String): String {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(1).uppercase(Locale.ROOT)
                else -> (parts[0].take(1) + parts[1].take(1)).uppercase(Locale.ROOT)
            }
        }
    }

    private object GuestDiffCallback : DiffUtil.ItemCallback<Guest>() {
        override fun areItemsTheSame(oldItem: Guest, newItem: Guest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Guest, newItem: Guest): Boolean {
            return oldItem == newItem
        }
    }
}
