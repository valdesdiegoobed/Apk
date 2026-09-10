package com.vaguer.pdfeditor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
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
        b.btnPrev.setOnClickListener { if (pageIndex > 0) { pageIndex--; render() } }
        b.btnNext.setOnClickListener { if (pageIndex + 1 < pageCount) { pageIndex++; render() } }
        b.btnText.setOnClickListener { addTextDialog() }
        b.btnImage.setOnClickListener {
            if (!hasTap()) toast("Primero toca el lugar de la página donde irá la imagen o firma")
            else pickImage.launch(arrayOf("image/*"))
        }
        b.btnRotate.setOnClickListener { mutate { doc ->
            val p = doc.getPage(pageIndex)
            p.rotation = ((p.rotation + 90) % 360)
        } }
        b.btnDelete.setOnClickListener {
            if (pageCount <= 1) return@setOnClickListener toast("El PDF debe conservar al menos una página")
            AlertDialog.Builder(this).setTitle("Eliminar página ${pageIndex + 1}?")
                .setPositiveButton("Eliminar") { _, _ ->
                    mutate { doc -> doc.removePage(pageIndex) }
                    if (pageIndex >= pageCount - 1) pageIndex = (pageIndex - 1).coerceAtLeast(0)
                }.setNegativeButton("Cancelar", null).show()
        }
        b.btnReorder.setOnClickListener { reorderDialog() }
        b.btnSave.setOnClickListener { saveAs.launch("PDF_editado.pdf") }
        b.btnShare.setOnClickListener { share(null) }
        b.btnWhatsApp.setOnClickListener { shareWhatsApp() }
        b.pdfView.setOnClickListener { toast("Posición seleccionada") }
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

    private fun addTextDialog() {
        if (!hasTap()) return toast("Primero toca la posición donde quieres colocar el texto")

        val doc = PDDocument.load(working)
        val page = doc.getPage(pageIndex)
        val box = page.cropBox ?: page.mediaBox
        val x = b.pdfView.normalizedTapX!! * box.width
        val yTop = b.pdfView.normalizedTapY!! * box.height
        val probe = FontProbe.nearest(doc, pageIndex, x, yTop)
        doc.close()

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

            fun writeWith(font: PDFont, text: String) {
                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
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
            try { startActivity(intentFor("com.whatsapp.w4b")) }
            catch (_: Exception) { share(null) }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
