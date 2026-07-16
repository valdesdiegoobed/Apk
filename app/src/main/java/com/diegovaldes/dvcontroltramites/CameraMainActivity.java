package com.diegovaldes.dvcontroltramites;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class CameraMainActivity extends MainActivity {
    private static final int CAMERA_REQUEST = 3101;
    private static final int FILE_REQUEST = 3102;

    private ValueCallback<Uri[]> pendingFileCallback;
    private Uri pendingCameraUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = findWebView(findViewById(android.R.id.content));
        if (webView != null) installFileChooser(webView);
    }

    private WebView findWebView(View view) {
        if (view instanceof WebView) return (WebView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void installFileChooser(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
                pendingFileCallback = callback;

                if (params.isCaptureEnabled() && acceptsImage(params.getAcceptTypes())) {
                    return openCameraDirectly();
                }

                try {
                    startActivityForResult(params.createIntent(), FILE_REQUEST);
                    return true;
                } catch (ActivityNotFoundException error) {
                    pendingFileCallback = null;
                    Toast.makeText(CameraMainActivity.this, "No se encontró una aplicación para elegir el archivo.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
    }

    private boolean acceptsImage(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) return true;
        for (String type : acceptTypes) {
            if (type == null || type.isEmpty() || type.startsWith("image/")) return true;
        }
        return false;
    }

    private boolean openCameraDirectly() {
        try {
            File directory = new File(getCacheDir(), "camera");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("No se pudo crear la carpeta temporal");
            File image = File.createTempFile("dv_foto_", ".jpg", directory);
            pendingCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", image);

            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(camera, CAMERA_REQUEST);
            return true;
        } catch (Exception error) {
            pendingCameraUri = null;
            if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
            Toast.makeText(this, "No se pudo abrir la cámara del teléfono.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CAMERA_REQUEST) {
            if (pendingFileCallback != null) {
                Uri[] result = resultCode == RESULT_OK && pendingCameraUri != null
                        ? new Uri[]{pendingCameraUri}
                        : null;
                pendingFileCallback.onReceiveValue(result);
            }
            pendingFileCallback = null;
            pendingCameraUri = null;
            return;
        }

        if (requestCode == FILE_REQUEST) {
            if (pendingFileCallback != null) {
                pendingFileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            }
            pendingFileCallback = null;
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}