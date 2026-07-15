package com.diegovaldes.dvcontroltramites;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int SAVE_PDF_REQUEST = 2002;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingPdf;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);
        configureWebView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private void configureWebView(WebView target) {
        WebSettings settings = target.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        target.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        target.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(readAsset("android-enhancements.js"), null);
                view.evaluateJavascript(readAsset("license.js"), null);
                view.evaluateJavascript(readAsset("ui-v1-2.js"), null);
                view.evaluateJavascript(readAsset("ui-v1-3.js"), null);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("appassets.androidplatform.net".equals(uri.getHost()) || "blob".equals(uri.getScheme())) return false;
                openExternal(uri);
                return true;
            }
        });

        target.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException error) {
                    fileCallback = null;
                    return false;
                }
            }
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                Dialog dialog = new Dialog(MainActivity.this);
                WebView child = new WebView(MainActivity.this);
                configureWebView(child);
                dialog.setContentView(child);
                dialog.show();
                if (dialog.getWindow() != null) dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(child);
                resultMsg.sendToTarget();
                return true;
            }
        });
    }

    private final class AndroidBridge {
        @JavascriptInterface
        public void createClientPdf(String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject data = new JSONObject(json);
                    pendingPdf = buildClientPdf(data);
                    String safeName = sanitizeFileName(data.optString("nombre", "cliente"));
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/pdf");
                    intent.putExtra(Intent.EXTRA_TITLE, "Ficha_" + safeName + ".pdf");
                    startActivityForResult(intent, SAVE_PDF_REQUEST);
                } catch (Exception error) {
                    pendingPdf = null;
                    Toast.makeText(MainActivity.this, "No se pudo generar el PDF.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private byte[] buildClientPdf(JSONObject data) throws Exception {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        canvas.drawColor(Color.WHITE);

        paint.setColor(Color.rgb(15, 23, 42));
        canvas.drawRect(0, 0, 595, 92, paint);
        paint.setColor(Color.rgb(212, 175, 55));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawRect(18, 18, 66, 66, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        canvas.drawText("DV", 27, 51, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        canvas.drawText("DV Control de Trámites", 82, 40, paint);
        paint.setTextSize(11);
        paint.setFakeBoldText(false);
        canvas.drawText("Ficha de presentación del cliente", 82, 62, paint);

        float y = 116;
        Bitmap photo = decodeDataUrl(data.optString("photo", null));
        if (photo != null) {
            RectF photoBox = new RectF(424, 112, 559, 282);
            paint.setColor(Color.rgb(229, 231, 235));
            canvas.drawRoundRect(photoBox, 10, 10, paint);
            RectF fitted = fitCenter(photo.getWidth(), photo.getHeight(), photoBox);
            canvas.drawBitmap(photo, null, fitted, paint);
        }

        paint.setColor(Color.rgb(21, 94, 239));
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("DATOS PERSONALES", 36, y, paint);
        y += 23;
        y = drawField(canvas, paint, "Nombre completo", data.optString("nombre", ""), 36, y, 365);
        y = drawField(canvas, paint, "CURP", data.optString("curp", ""), 36, y, 365);
        y = drawField(canvas, paint, "Teléfono / WhatsApp", data.optString("telefono", ""), 36, y, 365);
        y = drawField(canvas, paint, "Contraseña AFORE", data.optString("contrasenaAfore", ""), 36, y, 365);

        y = Math.max(y + 7, 305);
        paint.setColor(Color.rgb(21, 94, 239));
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("INFORMACIÓN DEL TRÁMITE", 36, y, paint);
        y += 23;
        y = drawField(canvas, paint, "Fecha de inicio", data.optString("fechaInicio", ""), 36, y, 523);
        y = drawField(canvas, paint, "Fecha para realizar solicitud", data.optString("fechaSolicitud", ""), 36, y, 523);
        y = drawField(canvas, paint, "Fecha de creación del expediente", formatIso(data.optString("creado", "")), 36, y, 523);

        y += 7;
        paint.setColor(Color.rgb(21, 94, 239));
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("DOCUMENTOS REGISTRADOS", 36, y, paint);
        y += 22;
        JSONArray docs = data.optJSONArray("documentos");
        paint.setTextSize(10.5f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.DKGRAY);
        if (docs == null || docs.length() == 0) {
            canvas.drawText("Sin documentos adjuntos.", 46, y, paint);
            y += 18;
        } else {
            for (int i = 0; i < docs.length() && i < 8; i++) {
                canvas.drawText("• " + docs.optString(i), 46, y, paint);
                y += 17;
            }
        }

        y += 5;
        paint.setColor(Color.rgb(21, 94, 239));
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("NOTAS", 36, y, paint);
        y += 20;
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(10.5f);
        paint.setFakeBoldText(false);
        drawWrappedText(canvas, paint, data.optString("notas", "Sin notas."), 36, y, 523, 14, 4);

        paint.setColor(Color.LTGRAY);
        canvas.drawLine(36, 794, 559, 794, paint);
        paint.setColor(Color.GRAY);
        paint.setTextSize(9);
        canvas.drawText("Generado por DV Control de Trámites - Diego Valdes Guerrero", 36, 813, paint);

        document.finishPage(page);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.writeTo(output);
        document.close();
        return output.toByteArray();
    }

    private float drawField(Canvas canvas, Paint paint, String label, String value, float x, float y, float width) {
        paint.setColor(Color.GRAY);
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(true);
        canvas.drawText(label, x, y, paint);
        paint.setColor(Color.rgb(31, 41, 55));
        paint.setTextSize(11.5f);
        paint.setFakeBoldText(false);
        String text = value == null || value.trim().isEmpty() ? "—" : value.trim();
        drawWrappedText(canvas, paint, text, x, y + 15, width, 14, 2);
        return y + 44;
    }

    private void drawWrappedText(Canvas canvas, Paint paint, String text, float x, float y, float width, float lineHeight, int maxLines) {
        String[] words = text.replace('\n', ' ').trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(test) > width && line.length() > 0) {
                canvas.drawText(line.toString(), x, y + lines * lineHeight, paint);
                lines++;
                if (lines >= maxLines) return;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0 && lines < maxLines) canvas.drawText(line.toString(), x, y + lines * lineHeight, paint);
    }

    private Bitmap decodeDataUrl(String value) {
        try {
            if (value == null || !value.contains(",")) return null;
            String encoded = value.substring(value.indexOf(',') + 1);
            byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    private RectF fitCenter(float sourceWidth, float sourceHeight, RectF box) {
        float scale = Math.min(box.width() / sourceWidth, box.height() / sourceHeight);
        float width = sourceWidth * scale;
        float height = sourceHeight * scale;
        float left = box.centerX() - width / 2f;
        float top = box.centerY() - height / 2f;
        return new RectF(left, top, left + width, top + height);
    }

    private String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value == null ? "cliente" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9_-]+", "_")
                .replaceAll("_+", "_");
        if (normalized.isEmpty()) normalized = "cliente";
        return normalized.substring(0, Math.min(normalized.length(), 60));
    }

    private String formatIso(String value) {
        if (value == null || value.length() < 10) return value == null || value.isEmpty() ? "—" : value;
        String[] parts = value.substring(0, 10).split("-");
        return parts.length == 3 ? parts[2] + "/" + parts[1] + "/" + parts[0] : value;
    }

    private String readAsset(String name) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(name)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception error) {
            return "";
        }
    }

    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException ignored) { }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        if (requestCode == SAVE_PDF_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingPdf != null) {
                try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                    if (output == null) throw new IllegalStateException("Sin acceso al archivo");
                    output.write(pendingPdf);
                    output.flush();
                    Toast.makeText(this, "PDF guardado correctamente.", Toast.LENGTH_LONG).show();
                } catch (Exception error) {
                    Toast.makeText(this, "No se pudo guardar el PDF.", Toast.LENGTH_LONG).show();
                }
            }
            pendingPdf = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
