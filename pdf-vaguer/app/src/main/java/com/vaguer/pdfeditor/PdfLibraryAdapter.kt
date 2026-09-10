package com.vaguer.pdfeditor

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaguer.pdfeditor.databinding.ItemLibraryPdfBinding
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class PdfLibraryAdapter(
    private val onOpen: (PdfLibraryStore.Entry) -> Unit,
    private val onMore: (PdfLibraryStore.Entry) -> Unit
) : RecyclerView.Adapter<PdfLibraryAdapter.Holder>() {

    private val all = mutableListOf<PdfLibraryStore.Entry>()
    private val shown = mutableListOf<PdfLibraryStore.Entry>()
    private val executor = Executors.newFixedThreadPool(2)

    fun submit(entries: List<PdfLibraryStore.Entry>, query: String = "") {
        all.clear(); all.addAll(entries)
        filter(query)
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        shown.clear()
        shown += if (q.isBlank()) all else all.filter { it.displayName.lowercase().contains(q) }
        notifyDataSetChanged()
    }

    fun isEmpty(): Boolean = shown.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemLibraryPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = shown.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(shown[position])

    inner class Holder(private val b: ItemLibraryPdfBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: PdfLibraryStore.Entry) {
            b.txtPdfName.text = entry.displayName
            b.txtPdfDate.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.modified))
            b.imgPdfPreview.setImageDrawable(null)
            b.previewPlaceholder.visibility = android.view.View.VISIBLE
            b.root.setOnClickListener { onOpen(entry) }
            b.btnPdfMore.setOnClickListener { onMore(entry) }

            val expectedPath = entry.file.absolutePath
            executor.execute {
                val bitmap = renderThumb(entry.file)
                b.root.post {
                    val current = shown.getOrNull(bindingAdapterPosition)
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && current?.file?.absolutePath == expectedPath && bitmap != null) {
                        b.imgPdfPreview.setImageBitmap(bitmap)
                        b.previewPlaceholder.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    private fun renderThumb(file: File): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) return null
            val page = renderer.openPage(0)
            val width = 300
            val scale = width.toFloat() / page.width.toFloat()
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bmp
        } catch (_: Exception) {
            null
        } finally {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
    }
}
