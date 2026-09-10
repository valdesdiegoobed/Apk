package com.vaguer.pdfeditor

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vaguer.pdfeditor.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private var pendingFile: File? = null

    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        startActivity(Intent(this, ModernPdfActivity::class.java).setData(uri))
    }

    private val mergePdfs = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.size < 2) {
            Toast.makeText(this, "Selecciona al menos 2 PDF", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        runCatching {
            val out = File(cacheDir, "unido_${System.currentTimeMillis()}.pdf")
            PdfOps.merge(this, uris, out)
            pendingFile = out
            createPdf.launch("PDF_unido.pdf")
        }.onFailure { toast(it.message ?: "No se pudieron unir") }
    }

    private val pickSplit = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        val input = EditText(this).apply { hint = "Ejemplo: 1-3,5,7" }
        AlertDialog.Builder(this)
            .setTitle("Páginas a extraer")
            .setView(input)
            .setPositiveButton("Continuar") { _, _ ->
                val pages = parsePages(input.text.toString())
                if (pages.isEmpty()) return@setPositiveButton toast("Indica páginas válidas")
                runCatching {
                    val out = File(cacheDir, "extraido_${System.currentTimeMillis()}.pdf")
                    PdfOps.extractPages(this, uri, pages, out)
                    pendingFile = out
                    createPdf.launch("PDF_extraido.pdf")
                }.onFailure { toast(it.message ?: "No se pudo dividir") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        runCatching {
            val out = File(cacheDir, "imagenes_${System.currentTimeMillis()}.pdf")
            PdfOps.imagesToPdf(this, uris, out)
            pendingFile = out
            createPdf.launch("Imagenes.pdf")
        }.onFailure { toast(it.message ?: "No se pudo crear PDF") }
    }

    private val createPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val file = pendingFile
        if (uri != null && file != null) {
            contentResolver.openOutputStream(uri)?.use { out -> FileInputStream(file).use { it.copyTo(out) } }
            toast("PDF guardado")
        }
        pendingFile = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnOpen.setOnClickListener { openPdf.launch(arrayOf("application/pdf")) }
        b.btnMerge.setOnClickListener { mergePdfs.launch(arrayOf("application/pdf")) }
        b.btnSplit.setOnClickListener { pickSplit.launch(arrayOf("application/pdf")) }
        b.btnImages.setOnClickListener { pickImages.launch(arrayOf("image/*")) }

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            startActivity(Intent(this, ModernPdfActivity::class.java).setData(intent.data))
        }
    }

    private fun parsePages(s: String): List<Int> {
        val out = mutableListOf<Int>()
        s.split(',').map { it.trim() }.forEach { token ->
            if ('-' in token) {
                val parts = token.split('-')
                val a = parts.getOrNull(0)?.toIntOrNull()
                val z = parts.getOrNull(1)?.toIntOrNull()
                if (a != null && z != null && a > 0 && z >= a) out += (a..z)
            } else token.toIntOrNull()?.takeIf { it > 0 }?.let { out += it }
        }
        return out
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
