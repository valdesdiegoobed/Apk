package com.vaguer.pdfeditor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.pdf.PdfRect
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileInputStream
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.max

class ModernPdfActivity : AppCompatActivity() {

    private lateinit var working: File
    private var viewer: VaguerPdfViewerFragment? = null
    private var currentPage = 0
    private var selectedText: String? = null
    private var selectedBounds: List<PdfRect> = emptyList()

    private lateinit var txtStatus: TextView
    private lateinit var btnUndo: Button
    private lateinit var btnRedo: Button
    private lateinit var btnEdit: Button

    private val undoStack = ArrayDeque<File>()
    private val redoStack = ArrayDeque<File>()
    private val historyLimit = 20

    private val saveAs = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(working).use { input -> input.copyTo(out) }
            }
            toast("Archivo guardado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modern_pdf)

        val source = intent.data ?: return finish()
        working = File(cacheDir, "modern_${System.currentTimeMillis()}.pdf")
        PdfOps.copyUriToFile(this, source, working)

        txtStatus = findViewById(R.id.txtModernStatus)
        btnUndo = findViewById(R.id.btnModernUndo)
        btnRedo = findViewById(R.id.btnModernRedo)
        btnEdit = findViewById(R.id.btnModernEdit)

        findViewById<Button>(R.id.btnModernSearch).setOnClickListener {
            viewer?.activateSearch() ?: toast("Espera a que termine de abrir el PDF")
        }
        btnEdit.setOnClickListener { editSelectedText() }
        btnUndo.setOnClickListener { undoChange() }
        btnRedo.setOnClickListener { redoChange() }
        findViewById<Button>(R.id.btnModernSave).setOnClickListener { saveAs.launch("PDF_editado.pdf") }
        findViewById<Button>(R.id.btnModernShare).setOnClickListener { share(null) }
        findViewById<Button>(R.id.btnModernWhatsApp).setOnClickListener { shareWhatsApp() }
        findViewById<Button>(R.id.btnModernTools).setOnClickListener { openClassicTools() }

        updateButtons()
        showViewer(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        undoStack.forEach { it.delete() }
        redoStack.forEach { it.delete() }
    }

    private fun showViewer(page: Int) {
        selectedText = null
        selectedBounds = emptyList()
        btnEdit.isEnabled = false
        btnEdit.alpha = 0.45f
        currentPage = page.coerceAtLeast(0)

        val fragment = VaguerPdfViewerFragment().apply {
            initialPage = currentPage
            onTextSelection = { text, bounds ->
                selectedText = text
                selectedBounds = bounds
                btnEdit.isEnabled = bounds.isNotEmpty()
                btnEdit.alpha = if (bounds.isNotEmpty()) 1f else 0.45f
                txtStatus.text = if (bounds.isNotEmpty()) {
                    "Seleccionado: ${text.take(55)}${if (text.length > 55) "…" else ""}"
                } else {
                    "Mantén presionado el texto para seleccionarlo"
                }
            }
            onSelectionCleared = {
                selectedText = null
                selectedBounds = emptyList()
                btnEdit.isEnabled = false
                btnEdit.alpha = 0.45f
                txtStatus.text = "Mantén presionado el texto para seleccionarlo"
            }
            onVisiblePageChanged = { currentPage = it }
            onDocumentReady = {
                txtStatus.text = "Mantén presionado el texto para seleccionar con precisión"
            }
            onDocumentError = { error ->
                txtStatus.text = "No se pudo abrir: ${error.message ?: "error del PDF"}"
            }
        }
        viewer = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.pdfModernContainer, fragment)
            .commitNow()
        fragment.documentUri = Uri.fromFile(working)
    }

    private fun editSelectedText() {
        val text = selectedText ?: return toast("Mantén presionado el texto que quieres editar")
        val bounds = selectedBounds
        if (bounds.isEmpty()) return toast("No hay texto seleccionado")
        val page = bounds.first().pageNum
        if (bounds.any { it.pageNum != page }) return toast("Para editar, selecciona texto de una sola página")

        var exact: TextProbe.Selection? = null
        runCatching {
            PDDocument.load(working).use { doc ->
                val left = bounds.minOf { it.left }
                val top = bounds.minOf { it.top }
                val right = bounds.maxOf { it.right }
                val bottom = bounds.maxOf { it.bottom }
                val probed = TextProbe.selectRange(doc, page, left, top, right, bottom)
                exact = if (probed != null) {
                    probed!!.copy(
                        text = text,
                        x = left,
                        width = (right - left).coerceAtLeast(probed!!.width),
                        height = max(probed!!.height, bottom - top)
                    )
                } else null
            }
        }
        val sel = exact ?: return toast("Este texto no puede editarse directamente. Puedes copiarlo o usar Más herramientas.")

        val textInput = EditText(this).apply {
            setText(text)
            selectAll()
        }
        val sizeInput = EditText(this).apply {
            hint = "Tamaño (pt)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.1f", sel.fontSize))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(42, 10, 42, 0)
            addView(textInput)
            addView(sizeInput)
        }
        AlertDialog.Builder(this)
            .setTitle("Editar texto")
            .setMessage("Fuente detectada: ${sel.fontName}\nSe conservará la posición y el formato cuando el PDF lo permita.")
            .setView(box)
            .setPositiveButton("Aplicar") { _, _ ->
                val value = textInput.text.toString()
                val size = sizeInput.text.toString().replace(',', '.').toFloatOrNull() ?: sel.fontSize
                replaceSelection(page, sel, value, size)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun replaceSelection(pageIndex: Int, sel: TextProbe.Selection, value: String, fontSize: Float) {
        val background = sampleBackgroundColor(pageIndex, sel)
        mutate(pageIndex) { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val baseline = box.height - sel.yTop
            val probe = FontProbe.nearest(doc, pageIndex, sel.x, sel.yTop)
            val font = probe?.font ?: PDType1Font.HELVETICA
            coverText(doc, page, sel, baseline, background)
            if (value.isNotBlank()) writeText(doc, page, sel.x, baseline, font, fontSize, value)
        }
    }

    private fun sampleBackgroundColor(pageIndex: Int, sel: TextProbe.Selection): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(working, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val renderPage = renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1))
            val maxW = 1800
            val scale = maxW.toFloat() / renderPage.width.toFloat()
            val bmp = Bitmap.createBitmap(maxW, (renderPage.height * scale).toInt(), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            renderPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            renderPage.close()

            var boxW = 1f
            var boxH = 1f
            PDDocument.load(working).use { doc ->
                val box = doc.getPage(pageIndex).cropBox ?: doc.getPage(pageIndex).mediaBox
                boxW = box.width
                boxH = box.height
            }
            val left = ((sel.x / boxW) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
            val right = (((sel.x + sel.width) / boxW) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
            val top = (((sel.yTop - sel.height * 1.2f) / boxH) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
            val bottom = (((sel.yTop + sel.height * 0.3f) / boxH) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
            val samples = mutableListOf<Int>()
            val pad = max(3, bmp.width / 700)
            fun add(x: Int, y: Int) {
                if (x !in 0 until bmp.width || y !in 0 until bmp.height) return
                val c = bmp.getPixel(x, y)
                val lum = (Color.red(c) + Color.green(c) + Color.blue(c)) / 3
                if (Color.alpha(c) > 220 && lum > 60) samples += c
            }
            val sx = max(1, (right - left).coerceAtLeast(1) / 35)
            var x = left
            while (x <= right) {
                add(x, top - pad)
                add(x, bottom + pad)
                x += sx
            }
            val sy = max(1, (bottom - top).coerceAtLeast(1) / 15)
            var y = top
            while (y <= bottom) {
                add(left - pad, y)
                add(right + pad, y)
                y += sy
            }
            if (samples.size < 4) Color.WHITE else {
                val rs = samples.map { Color.red(it) }.sorted()
                val gs = samples.map { Color.green(it) }.sorted()
                val bs = samples.map { Color.blue(it) }.sorted()
                val mid = samples.size / 2
                Color.rgb(rs[mid], gs[mid], bs[mid])
            }
        } catch (_: Exception) {
            Color.WHITE
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    private fun coverText(doc: PDDocument, page: PDPage, sel: TextProbe.Selection, baseline: Float, color: Int) {
        val pad = 1.0f
        val h = (sel.height * 1.25f).coerceAtLeast(sel.fontSize * 1.05f)
        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.setNonStrokingColor(Color.red(color), Color.green(color), Color.blue(color))
            cs.addRect((sel.x - pad).coerceAtLeast(0f), baseline - pad, sel.width + pad * 2f, h + pad * 2f)
            cs.fill()
        }
    }

    private fun writeText(doc: PDDocument, page: PDPage, x: Float, y: Float, font: PDFont, size: Float, value: String) {
        fun writeWith(useFont: PDFont, text: String) {
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.setNonStrokingColor(0, 0, 0)
                cs.beginText()
                cs.setFont(useFont, size)
                cs.newLineAtOffset(x, y)
                cs.showText(text)
                cs.endText()
            }
        }
        try {
            writeWith(font, value)
        } catch (_: Exception) {
            val safe = value.map { if (it.code in 32..255) it else '?' }.joinToString("")
            writeWith(PDType1Font.HELVETICA, safe)
        }
    }

    private fun snapshot(prefix: String): File {
        val out = File(cacheDir, "${prefix}_${System.nanoTime()}.pdf")
        working.copyTo(out, overwrite = true)
        return out
    }

    private fun mutate(pageToReturn: Int, block: (PDDocument) -> Unit) {
        val before = snapshot("undo_modern")
        runCatching {
            val temp = File(cacheDir, "modern_mut_${System.nanoTime()}.pdf")
            PDDocument.load(working).use { doc ->
                block(doc)
                doc.save(temp)
            }
            temp.copyTo(working, overwrite = true)
            temp.delete()
            undoStack.addLast(before)
            while (undoStack.size > historyLimit) undoStack.removeFirst().delete()
            while (redoStack.isNotEmpty()) redoStack.removeLast().delete()
            updateButtons()
            showViewer(pageToReturn)
        }.onFailure {
            before.delete()
            toast("No se pudo aplicar el cambio: ${it.message}")
        }
    }

    private fun undoChange() {
        if (undoStack.isEmpty()) return
        val current = snapshot("redo_modern")
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        previous.copyTo(working, overwrite = true)
        previous.delete()
        updateButtons()
        showViewer(currentPage)
    }

    private fun redoChange() {
        if (redoStack.isEmpty()) return
        val current = snapshot("undo_modern")
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        next.copyTo(working, overwrite = true)
        next.delete()
        updateButtons()
        showViewer(currentPage)
    }

    private fun updateButtons() {
        btnUndo.isEnabled = undoStack.isNotEmpty()
        btnRedo.isEnabled = redoStack.isNotEmpty()
        btnUndo.alpha = if (undoStack.isNotEmpty()) 1f else 0.35f
        btnRedo.alpha = if (redoStack.isNotEmpty()) 1f else 0.35f
        if (::btnEdit.isInitialized) {
            btnEdit.isEnabled = selectedBounds.isNotEmpty()
            btnEdit.alpha = if (selectedBounds.isNotEmpty()) 1f else 0.45f
        }
    }

    private fun openClassicTools() {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", working)
        startActivity(Intent(this, PdfEditorActivity::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun share(targetPackage: String?) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", working)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
        runCatching {
            startActivity(if (targetPackage == null) Intent.createChooser(intent, "Compartir PDF") else intent)
        }.onFailure {
            if (targetPackage != null) share(null) else toast("No se pudo compartir")
        }
    }

    private fun shareWhatsApp() {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", working)
        fun intentFor(pkg: String) = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(pkg)
        }
        try {
            startActivity(intentFor("com.whatsapp"))
        } catch (_: Exception) {
            try {
                startActivity(intentFor("com.whatsapp.w4b"))
            } catch (_: Exception) {
                share(null)
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
