package com.vaguer.pdfeditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.vaguer.pdfeditor.databinding.ActivityPdfEditorBinding
import java.io.File
import java.io.FileInputStream
import java.util.Locale

class PdfEditorActivity : AppCompatActivity() {
    private lateinit var b: ActivityPdfEditorBinding
    private lateinit var working: File
    private var pageIndex = 0
    private var pageCount = 0
    private var selection: TextProbe.Selection? = null
    private var actionMode: ActionMode? = null

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
        render()
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
        b.btnEdit.setOnClickListener {
            if (selection == null) selectNearestText(showMenu = false)
            if (selection != null) editSelectionDialog() else toast("Toca dos veces o mantén presionado sobre un texto")
        }
        b.btnText.setOnClickListener { addTextDialog() }
        b.btnPaste.setOnClickListener { pasteClipboard() }
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
                    mutate { doc -> doc.removePage(pageIndex) }
                    if (pageIndex >= pageCount - 1) pageIndex = (pageIndex - 1).coerceAtLeast(0)
                }.setNegativeButton("Cancelar", null).show()
        }
        b.btnReorder.setOnClickListener { reorderDialog() }
        b.btnSave.setOnClickListener { saveAs.launch("PDF_editado.pdf") }
        b.btnShare.setOnClickListener { share(null) }
        b.btnWhatsApp.setOnClickListener { shareWhatsApp() }

        b.pdfView.onSingleTapPositioned = { clearSelection() }
        b.pdfView.onTextSelectionGesture = {
            selectNearestText(showMenu = true)
        }
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
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
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
        val top = ((sel.yTop - sel.height * 1.15f) / boxHeight).coerceIn(0f, 1f)
        val bottom = ((sel.yTop + sel.height * 0.25f) / boxHeight).coerceIn(0f, 1f)
        val left = (sel.x / boxWidth).coerceIn(0f, 1f)
        val right = ((sel.x + sel.width) / boxWidth).coerceIn(0f, 1f)
        b.pdfView.setSelection(RectF(left, top, right, bottom))
        if (showMenu) showSelectionMenu()
        return true
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

        AlertDialog.Builder(this)
            .setTitle("Editar texto")
            .setMessage("Fuente detectada: ${sel.fontName}")
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
        AlertDialog.Builder(this)
            .setTitle("Agregar texto")
            .setMessage("Conservar formato original está activo cuando es posible.$fontInfo")
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

    private fun addText(value: String, fontSize: Float) {
        val nx = b.pdfView.normalizedTapX ?: return
        val ny = b.pdfView.normalizedTapY ?: return
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val x = nx * box.width
            val yTop = ny * box.height
            val y = box.height - yTop
            val probe = FontProbe.nearest(doc, pageIndex, x, yTop)
            val preferred = probe?.font ?: PDType1Font.HELVETICA
            writeText(doc, page, x, y, preferred, fontSize, value)
        }
        clearSelection()
    }

    private fun replaceSelection(value: String, fontSize: Float) {
        val sel = selection ?: return
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val baseline = box.height - sel.yTop
            val probe = FontProbe.nearest(doc, pageIndex, sel.x, sel.yTop)
            val preferred = probe?.font ?: PDType1Font.HELVETICA
            coverText(doc, page, sel, baseline)
            if (value.isNotBlank()) writeText(doc, page, sel.x, baseline, preferred, fontSize, value)
        }
        clearSelection()
    }

    private fun eraseSelection(sel: TextProbe.Selection) {
        mutate { doc ->
            val page = doc.getPage(pageIndex)
            val box: PDRectangle = page.cropBox ?: page.mediaBox
            val baseline = box.height - sel.yTop
            coverText(doc, page, sel, baseline)
        }
        clearSelection()
    }

    private fun coverText(doc: PDDocument, page: com.tom_roush.pdfbox.pdmodel.PDPage, sel: TextProbe.Selection, baseline: Float) {
        val pad = 1.5f
        val h = (sel.height * 1.35f).coerceAtLeast(sel.fontSize * 1.15f)
        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.setNonStrokingColor(255, 255, 255)
            cs.addRect((sel.x - pad).coerceAtLeast(0f), baseline - pad, sel.width + pad * 2f, h + pad * 2f)
            cs.fill()
        }
    }

    private fun writeText(
        doc: PDDocument,
        page: com.tom_roush.pdfbox.pdmodel.PDPage,
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
                    pageIndex = 0
                    render()
                }.onFailure { toast(it.message ?: "No se pudo reordenar") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mutate(block: (PDDocument) -> Unit) {
        runCatching {
            val temp = File(cacheDir, "mut_${System.nanoTime()}.pdf")
            PDDocument.load(working).use { doc ->
                block(doc)
                doc.save(temp)
            }
            temp.copyTo(working, overwrite = true)
            temp.delete()
            render()
        }.onFailure { toast("No se pudo aplicar el cambio: ${it.message}") }
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
