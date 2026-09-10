package com.vaguer.pdfeditor

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.PdfRect
import androidx.pdf.selection.Selection
import androidx.pdf.selection.model.TextSelection
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment
import java.io.File

@OptIn(ExperimentalPdfApi::class)
class VaguerPdfViewerFragment : PdfViewerFragment() {

    var initialPage: Int = 0
    var onTextSelection: ((String, List<PdfRect>) -> Unit)? = null
    var onSelectionCleared: (() -> Unit)? = null
    var onVisiblePageChanged: ((Int) -> Unit)? = null
    var onDocumentReady: (() -> Unit)? = null
    var onDocumentError: ((Throwable) -> Unit)? = null

    private var internalPdfView: PdfView? = null
    private var loaded = false
    private var fallbackStarted = false

    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        internalPdfView = pdfView

        pdfView.addOnSelectionChangedListener(object : PdfView.OnSelectionChangedListener {
            override fun onSelectionChanged(newSelection: Selection?) {
                if (newSelection is TextSelection) {
                    onTextSelection?.invoke(newSelection.text.toString(), newSelection.bounds)
                } else {
                    onSelectionCleared?.invoke()
                }
            }
        })

        pdfView.addOnViewportChangedListener(object : PdfView.OnViewportChangedListener {
            override fun onViewportChanged(
                firstVisiblePage: Int,
                visiblePagesCount: Int,
                pageLocations: android.util.SparseArray<android.graphics.RectF>,
                zoomLevel: Float
            ) {
                onVisiblePageChanged?.invoke(firstVisiblePage)
            }
        })

        pdfView.addOnFirstContentLoadListener {
            if (initialPage > 0) runCatching { pdfView.scrollToPage(initialPage) }
        }
    }

    override fun onLoadDocumentSuccess(document: PdfDocument) {
        super.onLoadDocumentSuccess(document)
        loaded = true
        onDocumentReady?.invoke()
    }

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        loaded = false
        onDocumentError?.invoke(error)
        openCompatibleFallback()
    }

    private fun openCompatibleFallback() {
        if (fallbackStarted || !isAdded) return
        val source = documentUri ?: return
        fallbackStarted = true
        val context = requireContext()
        val safeUri = if (source.scheme.equals("file", ignoreCase = true)) {
            val path = source.path
            if (path.isNullOrBlank()) source else runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
            }.getOrDefault(source)
        } else source

        runCatching {
            startActivity(Intent(context, PdfEditorActivity::class.java).apply {
                data = safeUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
            activity?.finish()
        }
    }

    fun activateSearch() {
        if (loaded) isTextSearchActive = true
    }

    fun scrollToPage(page: Int) {
        internalPdfView?.let { view -> runCatching { view.scrollToPage(page.coerceAtLeast(0)) } }
    }
}
