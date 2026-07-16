package com.diegovaldes.dvcontroltramites;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.stream.Collectors;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int SAVE_PDF_REQUEST = 2002;
    private static final String BACKUP_NAME = "DV-Auto-Respaldo.json";
    private static final String DOWNLOAD_FOLDER = "Download/DV Control de Tramites";
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingPdf;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        target.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { return assetLoader.shouldInterceptRequest(request.getUrl()); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) { return assetLoader.shouldInterceptRequest(Uri.parse(url)); }
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
                openExternal(uri); return true;
            }
        });
        target.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST); return true; }
                catch (ActivityNotFoundException error) { fileCallback = null; return false; }
            }
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                Dialog dialog = new Dialog(MainActivity.this); WebView child = new WebView(MainActivity.this); configureWebView(child);
                dialog.setContentView(child); dialog.show();
                if (dialog.getWindow() != null) dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj; transport.setWebView(child); resultMsg.sendToTarget(); return true;
            }
        });
    }

    private final class AndroidBridge {
        @JavascriptInterface public void createClientPdf(String json) {
            runOnUiThread(() -> { try {
                JSONObject data = new JSONObject(json); pendingPdf = buildClientPdf(data);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_TITLE, "Ficha_" + sanitizeFileName(data.optString("nombre", "cliente")) + ".pdf"); startActivityForResult(intent, SAVE_PDF_REQUEST);
            } catch (Exception error) { pendingPdf = null; Toast.makeText(MainActivity.this, "No se pudo generar el PDF.", Toast.LENGTH_LONG).show(); } });
        }
        @JavascriptInterface public void saveDataUrl(String dataUrl, String fileName, String mime) {
            runOnUiThread(() -> { try { Uri uri = writeDownload(decodeDataUrlBytes(dataUrl), safeFileName(fileName), mime, false); Toast.makeText(MainActivity.this, uri != null ? "Archivo guardado en Descargas/DV Control de Tramites" : "No se pudo guardar el archivo", Toast.LENGTH_LONG).show(); } catch (Exception e) { Toast.makeText(MainActivity.this, "No se pudo guardar el archivo", Toast.LENGTH_LONG).show(); } });
        }
        @JavascriptInterface public void shareDataUrl(String dataUrl, String fileName, String mime) {
            runOnUiThread(() -> { try {
                Uri uri = writeDownload(decodeDataUrlBytes(dataUrl), safeFileName(fileName), mime, true);
                if (uri == null) throw new IllegalStateException();
                Intent send = new Intent(Intent.ACTION_SEND); send.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime); send.putExtra(Intent.EXTRA_STREAM, uri); send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(send, "Enviar por WhatsApp o compartir"));
            } catch (Exception e) { Toast.makeText(MainActivity.this, "No se pudo compartir el archivo", Toast.LENGTH_LONG).show(); } });
        }
        @JavascriptInterface public void saveAutoBackup(String json) {
            new Thread(() -> { try { writeDownload(json.getBytes(StandardCharsets.UTF_8), BACKUP_NAME, "application/json", false); } catch (Exception ignored) {} }).start();
        }
        @JavascriptInterface public String loadAutoBackup() {
            try { return readLatestBackup(); } catch (Exception ignored) { return ""; }
        }
    }

    private byte[] decodeDataUrlBytes(String value) {
        if (value == null || !value.contains(",")) return new byte[0];
        return Base64.decode(value.substring(value.indexOf(',') + 1), Base64.DEFAULT);
    }
    private String safeFileName(String value) { String n = value == null || value.trim().isEmpty() ? "archivo" : value.trim(); return n.replaceAll("[\\/:*?\"<>|]", "_"); }

    private Uri writeDownload(byte[] bytes, String name, String mime, boolean unique) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!unique) {
                getContentResolver().delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?", new String[]{name, DOWNLOAD_FOLDER + "/"});
            }
            ContentValues values = new ContentValues(); values.put(MediaStore.MediaColumns.DISPLAY_NAME, unique ? System.currentTimeMillis() + "_" + name : name); values.put(MediaStore.MediaColumns.MIME_TYPE, mime); values.put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOAD_FOLDER); values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values); if (uri == null) return null;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) { if (out == null) return null; out.write(bytes); out.flush(); }
            ContentValues done = new ContentValues(); done.put(MediaStore.MediaColumns.IS_PENDING, 0); getContentResolver().update(uri, done, null, null); return uri;
        }
        File dir = new File(getExternalFilesDir(null), "DV Control de Tramites"); if (!dir.exists()) dir.mkdirs(); File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); } return Uri.fromFile(file);
    }

    private String readLatestBackup() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "";
        String[] projection = {MediaStore.MediaColumns._ID};
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        try (Cursor cursor = getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, new String[]{BACKUP_NAME, DOWNLOAD_FOLDER + "/"}, MediaStore.MediaColumns.DATE_MODIFIED + " DESC")) {
            if (cursor == null || !cursor.moveToFirst()) return "";
            Uri uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(cursor.getLong(0)));
            try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) return ""; byte[] buffer = new byte[8192]; int n; while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n); return out.toString("UTF-8");
            }
        }
    }

    private byte[] buildClientPdf(JSONObject data) throws Exception {
        PdfDocument document = new PdfDocument(); PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas(); Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)); canvas.drawColor(Color.WHITE);
        paint.setColor(Color.rgb(15,23,42)); canvas.drawRect(0,0,595,92,paint); paint.setColor(Color.rgb(212,175,55)); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3); canvas.drawRect(18,18,66,66,paint); paint.setStyle(Paint.Style.FILL); paint.setTextSize(22); paint.setFakeBoldText(true); canvas.drawText("DV",27,51,paint);
        paint.setColor(Color.WHITE); paint.setTextSize(20); canvas.drawText("DV Control de Trámites",82,40,paint); paint.setTextSize(11); paint.setFakeBoldText(false); canvas.drawText("Ficha de presentación del cliente",82,62,paint);
        float y=116; Bitmap photo=decodeDataUrl(data.optString("photo",null)); if(photo!=null){RectF box=new RectF(424,112,559,282);paint.setColor(Color.rgb(229,231,235));canvas.drawRoundRect(box,10,10,paint);canvas.drawBitmap(photo,null,fitCenter(photo.getWidth(),photo.getHeight(),box),paint);}
        paint.setColor(Color.rgb(21,94,239));paint.setTextSize(14);paint.setFakeBoldText(true);canvas.drawText("DATOS PERSONALES",36,y,paint);y+=23;
        y=drawField(canvas,paint,"Nombre completo",data.optString("nombre",""),36,y,365);y=drawField(canvas,paint,"CURP",data.optString("curp",""),36,y,365);y=drawField(canvas,paint,"Teléfono / WhatsApp",data.optString("telefono",""),36,y,365);y=drawField(canvas,paint,"Contraseña AFORE",data.optString("contrasenaAfore",""),36,y,365);
        y=Math.max(y+7,305);paint.setColor(Color.rgb(21,94,239));paint.setTextSize(14);paint.setFakeBoldText(true);canvas.drawText("INFORMACIÓN DEL TRÁMITE",36,y,paint);y+=23;
        y=drawField(canvas,paint,"Fecha de inicio",data.optString("fechaInicio",""),36,y,523);y=drawField(canvas,paint,"Fecha para realizar solicitud",data.optString("fechaSolicitud",""),36,y,523);y=drawField(canvas,paint,"Fecha de creación del expediente",formatIso(data.optString("creado","")),36,y,523);
        y+=7;paint.setColor(Color.rgb(21,94,239));paint.setTextSize(14);paint.setFakeBoldText(true);canvas.drawText("DOCUMENTOS REGISTRADOS",36,y,paint);y+=22;JSONArray docs=data.optJSONArray("documentos");paint.setTextSize(10.5f);paint.setFakeBoldText(false);paint.setColor(Color.DKGRAY);
        if(docs==null||docs.length()==0){canvas.drawText("Sin documentos adjuntos.",46,y,paint);y+=18;}else for(int i=0;i<docs.length()&&i<8;i++){canvas.drawText("• "+docs.optString(i),46,y,paint);y+=17;}
        y+=5;paint.setColor(Color.rgb(21,94,239));paint.setTextSize(14);paint.setFakeBoldText(true);canvas.drawText("NOTAS",36,y,paint);y+=20;paint.setColor(Color.DKGRAY);paint.setTextSize(10.5f);paint.setFakeBoldText(false);drawWrappedText(canvas,paint,data.optString("notas","Sin notas."),36,y,523,14,4);
        paint.setColor(Color.LTGRAY);canvas.drawLine(36,794,559,794,paint);paint.setColor(Color.GRAY);paint.setTextSize(9);canvas.drawText("Generado por DV Control de Trámites - Diego Valdes Guerrero",36,813,paint);
        document.finishPage(page);ByteArrayOutputStream output=new ByteArrayOutputStream();document.writeTo(output);document.close();return output.toByteArray();
    }
    private float drawField(Canvas c,Paint p,String label,String value,float x,float y,float width){p.setColor(Color.GRAY);p.setTextSize(9.5f);p.setFakeBoldText(true);c.drawText(label,x,y,p);p.setColor(Color.rgb(31,41,55));p.setTextSize(11.5f);p.setFakeBoldText(false);String text=value==null||value.trim().isEmpty()?"—":value.trim();drawWrappedText(c,p,text,x,y+15,width,14,2);return y+44;}
    private void drawWrappedText(Canvas c,Paint p,String text,float x,float y,float width,float lineHeight,int maxLines){String[] words=text.replace('\n',' ').trim().split("\\s+");StringBuilder line=new StringBuilder();int lines=0;for(String word:words){String test=line.length()==0?word:line+" "+word;if(p.measureText(test)>width&&line.length()>0){c.drawText(line.toString(),x,y+lines*lineHeight,p);if(++lines>=maxLines)return;line=new StringBuilder(word);}else line=new StringBuilder(test);}if(line.length()>0&&lines<maxLines)c.drawText(line.toString(),x,y+lines*lineHeight,p);}
    private Bitmap decodeDataUrl(String value){try{return BitmapFactory.decodeByteArray(decodeDataUrlBytes(value),0,decodeDataUrlBytes(value).length);}catch(Exception e){return null;}}
    private RectF fitCenter(float sw,float sh,RectF box){float scale=Math.min(box.width()/sw,box.height()/sh),w=sw*scale,h=sh*scale;return new RectF(box.centerX()-w/2,box.centerY()-h/2,box.centerX()+w/2,box.centerY()+h/2);}
    private String sanitizeFileName(String value){String n=Normalizer.normalize(value==null?"cliente":value,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^A-Za-z0-9_-]+","_").replaceAll("_+","_");if(n.isEmpty())n="cliente";return n.substring(0,Math.min(n.length(),60));}
    private String formatIso(String value){if(value==null||value.length()<10)return value==null||value.isEmpty()?"—":value;String[] p=value.substring(0,10).split("-");return p.length==3?p[2]+"/"+p[1]+"/"+p[0]:value;}
    private String readAsset(String name){try(BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open(name)))){return reader.lines().collect(Collectors.joining("\n"));}catch(Exception e){return "";}}
    private void openExternal(Uri uri){try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException ignored){}}
    @Override protected void onActivityResult(int requestCode,int resultCode,@Nullable Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==FILE_CHOOSER_REQUEST&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));fileCallback=null;return;}if(requestCode==SAVE_PDF_REQUEST){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&pendingPdf!=null){try(OutputStream output=getContentResolver().openOutputStream(data.getData())){if(output==null)throw new IllegalStateException();output.write(pendingPdf);output.flush();Toast.makeText(this,"PDF guardado correctamente.",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"No se pudo guardar el PDF.",Toast.LENGTH_LONG).show();}}pendingPdf=null;}}
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
}
