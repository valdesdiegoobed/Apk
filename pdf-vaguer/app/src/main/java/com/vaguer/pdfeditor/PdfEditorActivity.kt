package com.vaguer.pdfeditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.vaguer.pdfeditor.databinding.ActivityPdfEditorBinding
import java.io.File
import java.io.FileInputStream
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.max

class PdfEditorActivity : AppCompatActivity() {
    private enum class TextAlignMode { LEFT, CENTER, RIGHT }

    private lateinit var b: ActivityPdfEditorBinding
    private lateinit var working: File
    private var pageIndex = 0
    private var pageCount = 0
    private var selection: TextProbe.Selection? = null
    private var actionMode: ActionMode? = null
    private var alignMode = TextAlignMode.LEFT
    private var selectionGuideLeft: Float? = null
    private var selectionGuideRight: Float? = null
    private var renderedBitmap: Bitmap? = null

    private val undoStack = ArrayDeque<File>()
    private val redoStack = ArrayDeque<File>()
    private val historyLimit = 20

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        addImageAtTap(uri)
    }

    private val saveAs = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { out -> FileInputStream(working).use { it.copyTo(out) } }
            toast("Archivo guardado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPdfEditorBinding.inflate(layoutInflater)
        setContentView(b.root)

        val source = intent.data ?: return finish()
        working = File(cacheDir, "trabajo_${System.currentTimeMillis()}.pdf")
        PdfOps.copyUriToFile(this, source, working)

        bindButtons()
        updateAlignmentUi()
        updateHistoryUi()
        render()
    }

    override fun onDestroy() {
        super.onDestroy()
        undoStack.forEach { it.delete() }
        redoStack.forEach { it.delete() }
    }

    private fun bindButtons() {
        b.btnPrev.setOnClickListener {
            if (pageIndex > 0) {
                clearSelection()
                pageIndex--
                render()
            }
        }
        b.btnNext.setOnClickListener {
            if (pageIndex + 1 < pageCount) {
                clearSelection()
                pageIndex++
                render()
            }
        }
        b.btnUndo.setOnClickListener { undoChange() }
        b.btnRedo.setOnClickListener { redoChange() }
        b.btnSelect.setOnClickListener {
            clearSelection()
            b.pdfView.startRangeSelection()
            toast("Arrastra sobre el texto. Después puedes ajustar los puntos azules.")
        }
        b.btnEdit.setOnClickListener {
            if (selection == null) selectNearestText(showMenu = false)
            if (selection != null) editSelectionDialog() else toast("Selecciona texto con arrastre, doble toque o pulsación larga")
        }
        b.btnText.setOnClickListener { addTextDialog() }
        b.btnPaste.setOnClickListener { pasteClipboard() }
        b.btnSearch.setOnClickListener { searchDialog() }

        b.btnAlignLeft.setOnClickListener { setAlignMode(TextAlignMode.LEFT) }
        b.btnAlignCenter.setOnClickListener { setAlignMode(TextAlignMode.CENTER) }
        b.btnAlignRight.setOnClickListener { setAlignMode(TextAlignMode.RIGHT) }
        b.btnNudgeLeft.setOnClickListener { moveSelectionBy(-2.0f) }
        b.btnNudgeRight.setOnClickListener { moveSelectionBy(2.0f) }

        b.btnImage.setOnClickListener {
            if (!hasTap()) toast("Primero toca el lugar de la página donde irá la imagen o firma")
            else pickImage.launch(arrayOf("image/*"))
        }
        b.btnRotate.setOnClickListener {
            clearSelection()
            mutate { doc ->
                val p = doc.getPage(pageIndex)
                p.rotation = ((p.rotation + 90) % 360)
            }
        }
        b.btnDelete.setOnClickListener {
            if (pageCount <= 1) return@setOnClickListener toast("El PDF debe conservar al menos una página")
            AlertDialog.Builder(this).setTitle("Eliminar página ${pageIndex + 1}?")
                .setPositiveButton("Eliminar") { _, _ ->
                    clearSelection()
                    val oldCount = pageCount
                    val ok = mutate { doc -> doc.removePage(pageIndex) }
                    if (ok && pageIndex >= oldCount - 1) {
                        pageIndex = (pageIndex - 1).coerceAtLeast(0)
                        render()
                    }
                }.setNegativeButton("Cancelar", null).show()
        }
        b.btnReorder.setOnClickListener { reorderDialog() }
        b.btnSave.setOnClickListener { saveAs.launch("PDF_editado.pdf") }
        b.btnShare.setOnClickListener { share(null) }
        b.btnWhatsApp.setOnClickListener { shareWhatsApp() }

        b.pdfView.onSingleTapPositioned = { clearSelection() }
        b.pdfView.onTextSelectionGesture = { selectNearestText(showMenu = true) }
        b.pdfView.onRangeSelected = { left, top, right, bottom ->
            selectDraggedRange(left, top, right, bottom)
        }
        b.pdfView.onSelectionAdjusted = { left, top, right, bottom ->
            selectDraggedRange(left, top, right, bottom)
        }
    }

    private fun setAlignMode(mode: TextAlignMode) {
        alignMode = mode
        updateAlignmentUi()
    }

    private fun updateAlignmentUi() {
        b.btnAlignLeft.alpha = if (alignMode == TextAlignMode.LEFT) 1f else 0.55f
        b.btnAlignCenter.alpha = if (alignMode == TextAlignMode.CENTER) 1f else 0.55f
        b.btnAlignRight.alpha = if (alignMode == TextAlignMode.RIGHT) 1f else 0.55f
    }

    private fun updateHistoryUi() {
        b.btnUndo.isEnabled = undoStack.isNotEmpty()
        b.btnRedo.isEnabled = redoStack.isNotEmpty()
        b.btnUndo.alpha = if (undoStack.isNotEmpty()) 1f else 0.35f
        b.btnRedo.alpha = if (redoStack.isNotEmpty()) 1f else 0.35f
    }

    private fun render() {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(working, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount
            if (pageCount == 0) return
            pageIndex = pageIndex.coerceIn(0, pageCount - 1)
            val page = renderer.openPage(pageIndex)
            val maxW = 1800
            val scale = maxW.toFloat() / page.width
            val bmp = Bitmap.createBitmap(maxW, (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderedBitmap = bmp
            b.pdfView.setImageBitmap(bmp)
            b.txtPage.text = "Página ${pageIndex + 1} / $pageCount"
        } catch (e: Exception) {
            toast("No se pudo mostrar el PDF: ${e.message}")
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    private fun hasTap() = b.pdfView.normalizedTapX != null && b.pdfView.normalizedTapY != null

    private fun selectNearestText(showMenu: Boolean): Boolean {
        val nx = b.pdfView.normalizedTapX ?: return false
        val ny = b.pdfView.normalizedTapY ?: return false
        var found: TextProbe.Selection? = null
        var boxWidth = 1f
        var boxHeight = 1f

        runCatching {
            PDDocument.load(working).use { doc ->
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox
                boxWidth = box.width
                boxHeight = box.height
                found = TextProbe.nearestWord(doc, pageIndex, nx * box.width, ny * box.height)
            }
        }

        val sel = found ?: run {
            clearSelection()
            return false
        }

        selection = sel
        selectionGuideLeft = sel.x
        selectionGuideRight = sel.x + sel.width
        setSelectionHighlight(sel, boxWidth, boxHeight)
        if (showMenu) showSelectionMenu()
        return true
    }

    private fun selectDraggedRange(left: Float, top: Float, right: Float, bottom: Float) {
        var found: TextProbe.Selection? = null
        var boxWidth = 1f
        var boxHeight = 1f

        runCatching {
            PDDocument.load(working).use { doc ->
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox
                boxWidth = box.width
                boxHeight = box.height
                found = TextProbe.selectRange(
                    doc,
                    pageIndex,
                    left * box.width,
                    top * box.height,
                    right * box.width,
                    bottom * box.height
                )
            }
        }

        val sel = found ?: run {
            clearSelection()
            return
        }

        selection = sel
        selectionGuideLeft = left * boxWidth
        selectionGuideRight = right * boxWidth
        setSelectionHighlight(sel, boxWidth, boxHeight)
        showSelectionMenu()
    }

    private fun setSelectionHighlight(sel: TextProbe.Selection, boxWidth: Float, boxHeight: Float) {
        val top = ((sel.yTop - sel.height * 1.15f) / boxHeight).coerceIn(0f, 1f)
        val bottom = ((sel.yTop + sel.height * 0.25f) / boxHeight).coerceIn(0f, 1f)
        val left = (sel.x / boxWidth).coerceIn(0f, 1f)
        val right = ((sel.x + sel.width) / boxWidth).coerceIn(0f, 1f)
        b.pdfView.setSelection(RectF(left, top, right, bottom))
    }

    private fun refreshSelectionHighlight() {
        val sel = selection ?: return
        runCatching {
            PDDocument.load(working).use { doc ->
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox
                setSelectionHighlight(sel, box.width, box.height)
            }
        }
    }

    private fun showSelectionMenu() {
        actionMode?.finish()
        actionMode = b.pdfView.startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(0, 1, 0, "Editar")
                menu.add(0, 2, 1, "Copiar")
                menu.add(0, 3, 2, "Cortar")
                menu.add(0, 4, 3, "Pegar")
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    1 -> editSelectionDialog()
                    2 -> copySelection()
                    3 -> cutSelection()
                    4 -> pasteClipboard()
                    else -> return false
                }
                mode.finish()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                if (actionMode === mode) actionMode = null
            }
        }, ActionMode.TYPE_FLOATING)
    }

    private fun clearSelection() {
        selection = null
        selectionGuideLeft = null
        selectionGuideRight = null
        actionMode?.finish()
        actionMode = null
        b.pdfView.clearSelection()
    }

    private fun copySelection() {
        val sel = selection ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Texto PDF", sel.text))
    }

    private fun cutSelection() {
        val sel = selection ?: return
        copySelection()
        eraseSelection(sel)
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return toast("No hay texto copiado")
        if (clip.itemCount == 0) return toast("No hay texto copiado")
        val value = clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
        if (value.isBlank()) return toast("No hay texto para pegar")

        val sel = selection
        if (sel != null) {
            replaceSelection(value, sel.fontSize)
            return
        }

        if (!hasTap()) return toast("Toca primero el lugar donde quieres pegar el texto")
        val size = nearbyFontSizeAtTap()
        addText(value, size)
    }

    private fun nearbyFontSizeAtTap(): Float {
        val nx = b.pdfView.normalizedTapX ?: return 11f
        val ny = b.pdfView.normalizedTapY ?: return 11f
        return runCatching {
            PDDocument.load(working).use { doc ->
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox
                FontProbe.nearest(doc, pageIndex, nx * box.width, ny * box.height)?.size ?: 11f
            }
        }.getOrDefault(11f)
    }

    private fun editSelectionDialog() {
        val sel = selection ?: return
        val text = EditText(this).apply {
            setText(sel.text)
            selectAll()
        }
        val size = EditText(this).apply {
            hint = "Tamaño (pt)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.1f", sel.fontSize))
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 0)
            addView(text)
            addView(size)
        }
        val alignmentLabel = when (alignMode) {
            TextAlignMode.LEFT -> "Izquierda"
            TextAlignMode.CENTER -> "Centro"
            TextAlignMode.RIGHT -> "Derecha"
        }

        AlertDialog.Builder(this)
            .setTitle("Editar texto")
            .setMessage("Fuente detectada: ${sel.fontName}\nAlineación: $alignmentLabel")
            .setView(layout)
            .setPositiveButton("Aplicar") { _, _ ->
                val value = text.text.toString()
                val pt = size.text.toString().replace(',', '.').toFloatOrNull() ?: sel.fontSize
                replaceSelection(value, pt)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addTextDialog() {
        if (!hasTap()) return toast("Primero toca la posición donde quieres colocar el texto")

        val probe = runCatching {
            PDDocument.load(working).use { doc ->
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox
                val x = b.pdfView.normalizedTapX!! * box.width
                val yTop = b.pdfView.normalizedTapY!! * box.height
                FontProbe.nearest(doc, pageIndex, x, yTop)
            }
        }.getOrNull()

        val text = EditText(this).apply { hint = "Texto" }
        val size = EditText(this).apply {
            hint = "Tamaño (pt)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.1f", probe?.size ?: 11f))
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 0)
            addView(text)
            addView(size)
        }
        val fontInfo = probe?.let { "\nFuente cercana detectada: ${it.name}" } ?: "\nSin fuente cercana: se usará Helvetica"
        val alignmentLabel = when (alignMode) {
            TextAlignMode.LEFT -> "Izquierda"
            TextAlignMode.CENTER -> "Centro"
            TextAlignMode.RIGHT -> "Derecha"
        }
        AlertDialog.Builder(this)
            .setTitle("Agregar texto")
            .setMessage("Conservar formato original está activo cuando es posible.$fontInfo\nAlineación: $alignmentLabel")
            .setView(layout)
            .setPositiveButton("Agregar") { _, _ ->
                val value = text.text.toString()
                val pt = size.text.toString().replace(',', '.').toFloatOrNull() ?: 11f
                if (value.isBlank()) return@setPositiveButton
                addText(value, pt)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun searchDialog() {
        val input = EditText(this).apply {
            hint = "Texto a buscar"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle("Buscar en PDF")
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) searchText(q)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun searchText(query: String) {
        runCatching {
            PDDocument.load(working).use { doc ->
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                val order = ((pageIndex until doc.numberOfPages) + (0 until pageIndex)).distinct()
                for (i in order) {
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    val text = stripper.getText(doc)
                    if (text.contains(query, ignoreCase = true)) {
                        clearSelection()
                        pageIndex = i
                        render()
                        toast("Encontrado en página ${i + 1}")
                        return
                    }
                }
            }
            toast("No se encontró “$query”")
        }.onFailure { toast("No se pudo buscar: ${it.message}") }
    }

    private fun addText(value: String, fontSize: Float) {
        val nx = b.pdfView.normalizedTapX ?: return
        val ny = b.pdfView.normalizedTapY ?: return
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val anchorX = nx * box.width
            val yTop = ny * box.height
            val y = box.height - yTop
            val probe = FontProbe.nearest(doc, pageIndex, anchorX, yTop)
            val preferred = probe?.font ?: PDType1Font.HELVETICA
            val width = textWidth(preferred, fontSize, value)
            val x = when (alignMode) {
                TextAlignMode.LEFT -> anchorX
                TextAlignMode.CENTER -> anchorX - width / 2f
                TextAlignMode.RIGHT -> anchorX - width
            }.coerceIn(0f, (box.width - width).coerceAtLeast(0f))
            writeText(doc, page, x, y, preferred, fontSize, value)
        }
        clearSelection()
    }

    private fun replaceSelection(value: String, fontSize: Float) {
        val sel = selection ?: return
        val guideLeft = selectionGuideLeft ?: sel.x
        val guideRight = selectionGuideRight ?: (sel.x + sel.width)
        val bg = sampleBackgroundColor(sel)
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val baseline = box.height - sel.yTop
            val probe = FontProbe.nearest(doc, pageIndex, sel.x, sel.yTop)
            val preferred = probe?.font ?: PDType1Font.HELVETICA
            coverText(doc, page, sel, baseline, bg)
            if (value.isNotBlank()) {
                val width = textWidth(preferred, fontSize, value)
                val rawX = when (alignMode) {
                    TextAlignMode.LEFT -> guideLeft
                    TextAlignMode.CENTER -> guideLeft + ((guideRight - guideLeft) - width) / 2f
                    TextAlignMode.RIGHT -> guideRight - width
                }
                val x = rawX.coerceIn(0f, (box.width - width).coerceAtLeast(0f))
                writeText(doc, page, x, baseline, preferred, fontSize, value)
            }
        }
        clearSelection()
    }

    private fun moveSelectionBy(dx: Float) {
        val sel = selection ?: return toast("Selecciona primero el texto que quieres mover")
        val bg = sampleBackgroundColor(sel)
        var newX = sel.x
        val ok = mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val baseline = box.height - sel.yTop
            val probe = FontProbe.nearest(doc, pageIndex, sel.x, sel.yTop)
            val preferred = probe?.font ?: PDType1Font.HELVETICA
            val width = textWidth(preferred, sel.fontSize, sel.text)
            newX = (sel.x + dx).coerceIn(0f, (box.width - width).coerceAtLeast(0f))
            coverText(doc, page, sel, baseline, bg)
            writeText(doc, page, newX, baseline, preferred, sel.fontSize, sel.text)
        }

        if (ok) {
            val actualDx = newX - sel.x
            selection = sel.copy(x = newX)
            selectionGuideLeft = (selectionGuideLeft ?: sel.x) + actualDx
            selectionGuideRight = (selectionGuideRight ?: (sel.x + sel.width)) + actualDx
            refreshSelectionHighlight()
        }
    }

    private fun eraseSelection(sel: TextProbe.Selection) {
        val bg = sampleBackgroundColor(sel)
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val baseline = box.height - sel.yTop
            coverText(doc, page, sel, baseline, bg)
        }
        clearSelection()
    }

    private fun sampleBackgroundColor(sel: TextProbe.Selection): Int {
        val bmp = renderedBitmap ?: return Color.WHITE
        var boxW = 1f
        var boxH = 1f
        runCatching {
            PDDocument.load(working).use { doc ->
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox
                boxW = box.width
                boxH = box.height
            }
        }
        if (boxW <= 1f || boxH <= 1f) return Color.WHITE

        val left = ((sel.x / boxW) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val right = (((sel.x + sel.width) / boxW) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val top = (((sel.yTop - sel.height * 1.20f) / boxH) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val bottom = (((sel.yTop + sel.height * 0.30f) / boxH) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val pad = max(3, (bmp.width / 600f).toInt())
        val samples = mutableListOf<Int>()

        fun add(x: Int, y: Int) {
            if (x !in 0 until bmp.width || y !in 0 until bmp.height) return
            val c = bmp.getPixel(x, y)
            if (Color.alpha(c) < 220) return
            val lum = (Color.red(c) + Color.green(c) + Color.blue(c)) / 3
            if (lum < 55) return
            samples += c
        }

        val stepX = max(1, (right - left).coerceAtLeast(1) / 40)
        var x = left
        while (x <= right) {
            add(x, top - pad)
            add(x, bottom + pad)
            x += stepX
        }
        val stepY = max(1, (bottom - top).coerceAtLeast(1) / 20)
        var y = top
        while (y <= bottom) {
            add(left - pad, y)
            add(right + pad, y)
            y += stepY
        }
        if (samples.size < 4) return Color.WHITE

        val rs = samples.map { Color.red(it) }.sorted()
        val gs = samples.map { Color.green(it) }.sorted()
        val bs = samples.map { Color.blue(it) }.sorted()
        val mid = samples.size / 2
        return Color.rgb(rs[mid], gs[mid], bs[mid])
    }

    private fun coverText(doc: PDDocument, page: PDPage, sel: TextProbe.Selection, baseline: Float, backgroundColor: Int) {
        val pad = 1.2f
        val h = (sel.height * 1.28f).coerceAtLeast(sel.fontSize * 1.08f)
        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.setNonStrokingColor(Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor))
            cs.addRect((sel.x - pad).coerceAtLeast(0f), baseline - pad, sel.width + pad * 2f, h + pad * 2f)
            cs.fill()
        }
    }

    private fun textWidth(font: PDFont, fontSize: Float, value: String): Float {
        return try {
            (font.getStringWidth(value) / 1000f * fontSize).coerceAtLeast(1f)
        } catch (_: Exception) {
            (value.length * fontSize * 0.55f).coerceAtLeast(1f)
        }
    }

    private fun writeText(
        doc: PDDocument,
        page: PDPage,
        x: Float,
        y: Float,
        preferred: PDFont,
        fontSize: Float,
        value: String
    ) {
        fun writeWith(font: PDFont, text: String) {
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.setNonStrokingColor(0, 0, 0)
                cs.beginText()
                cs.setFont(font, fontSize)
                cs.newLineAtOffset(x, y)
                cs.showText(text)
                cs.endText()
            }
        }

        try {
            writeWith(preferred, value)
        } catch (_: Exception) {
            val safe = value.map { if (it.code in 32..255) it else '?' }.joinToString("")
            writeWith(PDType1Font.HELVETICA, safe)
        }
    }

    private fun addImageAtTap(uri: Uri) {
        val nx = b.pdfView.normalizedTapX ?: return
        val ny = b.pdfView.normalizedTapY ?: return
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box = page.cropBox ?: page.mediaBox
            val image = PDImageXObject.createFromByteArray(doc, bytes, "insertada")
            val targetW = box.width * 0.30f
            val targetH = targetW * image.height / image.width.toFloat()
            val x = (nx * box.width).coerceAtMost(box.width - targetW)
            val yTop = ny * box.height
            val y = (box.height - yTop - targetH).coerceAtLeast(0f)
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.drawImage(image, x, y, targetW, targetH)
            }
        }
        clearSelection()
    }

    private fun reorderDialog() {
        val input = EditText(this).apply {
            hint = "Ejemplo: 1,3,2,4"
            setText((1..pageCount).joinToString(","))
        }
        AlertDialog.Builder(this)
            .setTitle("Nuevo orden de páginas")
            .setMessage("Escribe los números de página separados por comas. También puedes omitir una página para eliminarla.")
            .setView(input)
            .setPositiveButton("Aplicar") { _, _ ->
                val order = input.text.toString().split(',').mapNotNull { it.trim().toIntOrNull() }
                if (order.isEmpty() || order.any { it !in 1..pageCount }) return@setPositiveButton toast("Orden no válido")
                val before = snapshotCurrent("undo")
                runCatching {
                    clearSelection()
                    val temp = File(cacheDir, "reorder_${System.nanoTime()}.pdf")
                    PDDocument.load(working).use { src ->
                        PDDocument().use { dst ->
                            order.forEach { dst.importPage(src.getPage(it - 1)) }
                            dst.save(temp)
                        }
                    }
                    temp.copyTo(working, overwrite = true)
                    temp.delete()
                    pushUndoSnapshot(before)
                    pageIndex = 0
                    render()
                }.onFailure {
                    before.delete()
                    toast(it.message ?: "No se pudo reordenar")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun snapshotCurrent(prefix: String): File {
        val f = File(cacheDir, "${prefix}_${System.nanoTime()}.pdf")
        working.copyTo(f, overwrite = true)
        return f
    }

    private fun pushUndoSnapshot(snapshot: File) {
        undoStack.addLast(snapshot)
        while (undoStack.size > historyLimit) undoStack.removeFirst().delete()
        while (redoStack.isNotEmpty()) redoStack.removeLast().delete()
        updateHistoryUi()
    }

    private fun mutate(block: (PDDocument) -> Unit): Boolean {
        val before = snapshotCurrent("undo")
        return runCatching {
            val temp = File(cacheDir, "mut_${System.nanoTime()}.pdf")
            PDDocument.load(working).use { doc ->
                block(doc)
                doc.save(temp)
            }
            temp.copyTo(working, overwrite = true)
            temp.delete()
            pushUndoSnapshot(before)
            render()
            true
        }.getOrElse {
            before.delete()
            toast("No se pudo aplicar el cambio: ${it.message}")
            false
        }
    }

    private fun undoChange() {
        if (undoStack.isEmpty()) return
        clearSelection()
        val current = snapshotCurrent("redo")
        val previous = undoStack.removeLast()
        current.let { redoStack.addLast(it) }
        previous.copyTo(working, overwrite = true)
        previous.delete()
        while (redoStack.size > historyLimit) redoStack.removeFirst().delete()
        pageIndex = pageIndex.coerceAtLeast(0)
        render()
        updateHistoryUi()
    }

    private fun redoChange() {
        if (redoStack.isEmpty()) return
        clearSelection()
        val current = snapshotCurrent("undo")
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        next.copyTo(working, overwrite = true)
        next.delete()
        while (undoStack.size > historyLimit) undoStack.removeFirst().delete()
        pageIndex = pageIndex.coerceAtLeast(0)
        render()
        updateHistoryUi()
    }

    private fun share(targetPackage: String?) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", working)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
        runCatching { startActivity(if (targetPackage == null) Intent.createChooser(intent, "Compartir PDF") else intent) }
            .onFailure { if (targetPackage != null) share(null) else toast("No se pudo compartir") }
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

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
