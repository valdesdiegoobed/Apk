package com.vaguer.pdfeditor

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfLibraryStore {
    data class Entry(val file: File, val displayName: String, val modified: Long)

    private fun dir(context: Context): File = File(context.filesDir, "pdf_library").apply { mkdirs() }

    fun list(context: Context): List<Entry> = dir(context)
        .listFiles { f -> f.isFile && f.extension.equals("pdf", true) }
        ?.map { Entry(it, displayName(it), it.lastModified()) }
        ?.sortedByDescending { it.modified }
        ?: emptyList()

    fun save(context: Context, source: File, preferredName: String, overwritePath: String? = null): File {
        val target = overwritePath?.let { File(it) }?.takeIf { it.parentFile?.absolutePath == dir(context).absolutePath }
            ?: uniqueFile(context, preferredName)
        target.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(target, false).use { output -> input.copyTo(output) }
        }
        target.setLastModified(System.currentTimeMillis())
        return target
    }

    fun importUri(context: Context, uri: Uri): File {
        val name = queryName(context, uri) ?: "Documento_${System.currentTimeMillis()}.pdf"
        val target = uniqueFile(context, name)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "No se pudo leer el PDF" }
            FileOutputStream(target).use { out -> input.copyTo(out) }
        }
        return target
    }

    fun rename(context: Context, entry: Entry, newName: String): File {
        val clean = cleanName(newName)
        val target = uniqueFile(context, clean, ignore = entry.file)
        if (target.absolutePath == entry.file.absolutePath) return entry.file
        if (!entry.file.renameTo(target)) {
            FileInputStream(entry.file).use { input -> FileOutputStream(target).use { input.copyTo(it) } }
            entry.file.delete()
        }
        target.setLastModified(System.currentTimeMillis())
        return target
    }

    fun delete(entry: Entry): Boolean = entry.file.delete()

    fun queryName(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else uri.lastPathSegment?.substringAfterLast('/')
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/')
        } finally {
            cursor?.close()
        }
    }

    private fun uniqueFile(context: Context, rawName: String, ignore: File? = null): File {
        val folder = dir(context)
        val clean = cleanName(rawName)
        val base = clean.removeSuffix(".pdf")
        var candidate = File(folder, clean)
        if (candidate.absolutePath == ignore?.absolutePath) return candidate
        var n = 2
        while (candidate.exists() && candidate.absolutePath != ignore?.absolutePath) {
            candidate = File(folder, "$base ($n).pdf")
            n++
        }
        return candidate
    }

    private fun cleanName(raw: String): String {
        val base = raw.trim().ifBlank { "Documento" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(110)
        return if (base.endsWith(".pdf", true)) base else "$base.pdf"
    }

    private fun displayName(file: File): String = file.name
}
