package com.vaguer.pdfeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.vaguer.pdfeditor.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var libraryAdapter: PdfLibraryAdapter
    private var pendingFile: File? = null

    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        openEditor(uri, null, PdfLibraryStore.queryName(this, uri))
    }

    private val importToLibrary = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching { PdfLibraryStore.importUri(this, uri) }
            .onSuccess { file ->
                toast("Guardado en PDF VAGUER")
                refreshLibrary()
                openLibrary(file)
            }
            .onFailure { toast(it.message ?: "No se pudo guardar el PDF") }
    }

    private val mergePdfs = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.size < 2) return@registerForActivityResult toast("Selecciona al menos 2 PDF")
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

        libraryAdapter = PdfLibraryAdapter(::openEntry, ::showEntryMenu)
        b.recyclerLibrary.layoutManager = GridLayoutManager(this, 2)
        b.recyclerLibrary.adapter = libraryAdapter

        b.btnOpen.setOnClickListener { openPdf.launch(arrayOf("application/pdf")) }
        b.btnImportLibrary.setOnClickListener { importToLibrary.launch(arrayOf("application/pdf")) }
        b.btnMerge.setOnClickListener { mergePdfs.launch(arrayOf("application/pdf")) }
        b.btnSplit.setOnClickListener { pickSplit.launch(arrayOf("application/pdf")) }
        b.btnImages.setOnClickListener { pickImages.launch(arrayOf("image/*")) }
        b.edtLibrarySearch.doAfterTextChanged { refreshEmptyState(it?.toString().orEmpty()) }

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            openEditor(intent.data!!, null, PdfLibraryStore.queryName(this, intent.data!!))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLibrary()
    }

    private fun refreshLibrary() {
        val entries = PdfLibraryStore.list(this)
        val query = b.edtLibrarySearch.text?.toString().orEmpty()
        libraryAdapter.submit(entries, query)
        b.txtLibraryCount.text = when (entries.size) {
            0 -> "0 documentos"
            1 -> "1 documento"
            else -> "${entries.size} documentos"
        }
        refreshEmptyState(query)
    }

    private fun refreshEmptyState(query: String) {
        libraryAdapter.filter(query)
        b.txtEmpty.visibility = if (libraryAdapter.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEntry(entry: PdfLibraryStore.Entry) = openLibrary(entry.file)

    private fun openLibrary(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        openEditor(uri, file.absolutePath, file.name)
    }

    private fun openEditor(uri: Uri, libraryPath: String?, displayName: String?) {
        startActivity(Intent(this, ModernPdfActivity::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(ModernPdfActivity.EXTRA_LIBRARY_PATH, libraryPath)
            putExtra(ModernPdfActivity.EXTRA_DISPLAY_NAME, displayName ?: "Documento.pdf")
        })
    }

    private fun showEntryMenu(entry: PdfLibraryStore.Entry, anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Abrir")
            menu.add("Compartir")
            menu.add("Renombrar")
            menu.add("Eliminar")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Abrir" -> openEntry(entry)
                    "Compartir" -> shareEntry(entry)
                    "Renombrar" -> renameEntry(entry)
                    "Eliminar" -> confirmDelete(entry)
                }
                true
            }
            show()
        }
    }

    private fun shareEntry(entry: PdfLibraryStore.Entry) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", entry.file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Compartir PDF"))
    }

    private fun renameEntry(entry: PdfLibraryStore.Entry) {
        val input = EditText(this).apply {
            setText(entry.displayName.removeSuffix(".pdf"))
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle("Renombrar PDF")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                runCatching { PdfLibraryStore.rename(this, entry, input.text.toString()) }
                    .onSuccess { refreshLibrary() }
                    .onFailure { toast("No se pudo renombrar") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(entry: PdfLibraryStore.Entry) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar de PDF VAGUER")
            .setMessage("¿Quieres eliminar “${entry.displayName}” de la biblioteca? El archivo original externo no se modifica.")
            .setPositiveButton("Eliminar") { _, _ ->
                if (PdfLibraryStore.delete(entry)) refreshLibrary() else toast("No se pudo eliminar")
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
