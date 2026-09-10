package com.vaguer.pdfeditor

import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.PdfRect
import androidx.pdf.selection.Selection
import androidx.pdf.selection.model.TextSelection
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment

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

    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        internalPdfView = pdfView

        pdfView.addOnSelectionChangedListener(object : PdfView.OnSelectionChangedListener {
            override fun onSelectionChanged(newSelection: Selection) {
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
    }

    fun activateSearch() {
        if (loaded) isTextSearchActive = true
    }

    fun scrollToPage(page: Int) {
        internalPdfView?.let { view -> runCatching { view.scrollToPage(page.coerceAtLeast(0)) } }
    }
}
