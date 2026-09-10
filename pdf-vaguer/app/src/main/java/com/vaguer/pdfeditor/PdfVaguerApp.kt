package com.vaguer.pdfeditor

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfVaguerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}
