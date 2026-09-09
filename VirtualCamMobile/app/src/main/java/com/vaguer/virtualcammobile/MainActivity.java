package com.vaguer.virtualcammobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_PHOTO = 1001;
    private static final int REQ_VIDEO = 1002;
    private static final int REQ_PERMS = 2001;
    private static final long MAX_PHOTO = 12L * 1024L * 1024L;
    private static final long MAX_VIDEO = 16L * 1024L * 1024L;

    private WebView webView;
    private EditText urlEdit;
    private TextView status;
    private Spinner resolutionSpinner;
    private Spinner fitSpinner;
    private CheckBox micCheck;

    private String selectedMode;
    private String selectedDataUrl;
    private String selectedLabel;
    private ScriptHandler scriptHandler;
    private String fallbackScript;
    private PermissionRequest pendingPermission;

    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        setContentView(buildUi());
        configureWebView();
        loadTestPage();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(6), dp(6), dp(6), dp(6));
        root.setBackgroundColor(Color.WHITE);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        Button back = button("←");
        back.setOnClickListener(v -> { if (webView != null && webView.canGoBack()) webView.goBack(); });
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        urlEdit = new EditText(this);
        urlEdit.setSingleLine(true);
        urlEdit.setHint("https://sitio.com");
        urlEdit.setTextSize(14);
        urlEdit.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) { navigate(); return true; }
            return false;
        });
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        urlLp.setMargins(dp(4), 0, dp(4), 0);
        top.addView(urlEdit, urlLp);

        Button go = button("IR");
        go.setOnClickListener(v -> navigate());
        top.addView(go, new LinearLayout.LayoutParams(dp(58), dp(48)));
        root.addView(top);

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        status = new TextView(this);
        status.setText("Fuente: ninguna");
        status.setTextColor(Color.BLACK);
        status.setTextSize(14);
        status.setPadding(dp(4), dp(4), dp(4), dp(3));
        root.addView(status);

        ScrollView sourceScroll = new ScrollView(this);
        sourceScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout sources = new LinearLayout(this);
        sources.setOrientation(LinearLayout.HORIZONTAL);

        Button photo = button("🖼 Foto");
        photo.setOnClickListener(v -> pickMedia("image/*", REQ_PHOTO));
        sources.addView(photo);

        Button video = button("🎬 Video");
        video.setOnClickListener(v -> pickMedia("video/*", REQ_VIDEO));
        sources.addView(video);

        Button real = button("📷 Real");
        real.setOnClickListener(v -> {
            selectedMode = null;
            selectedDataUrl = null;
            selectedLabel = null;
            stopVirtual(true);
            status.setText("Fuente: cámara real del teléfono");
        });
        sources.addView(real);

        Button test = button("PRUEBA");
        test.setOnClickListener(v -> loadTestPage());
        sources.addView(test);
        sourceScroll.addView(sources);
        root.addView(sourceScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);
        options.setGravity(Gravity.CENTER_VERTICAL);

        resolutionSpinner = new Spinner(this);
        resolutionSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"480p", "720p", "1080p"}));
        resolutionSpinner.setSelection(1);
        options.addView(resolutionSpinner, new LinearLayout.LayoutParams(0, dp(48), 1f));

        fitSpinner = new Spinner(this);
        fitSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Cubrir", "Contener"}));
        options.addView(fitSpinner, new LinearLayout.LayoutParams(0, dp(48), 1f));

        micCheck = new CheckBox(this);
        micCheck.setText("Mic");
        options.addView(micCheck, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)));
        root.addView(options);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button start = button("▶ ACTIVAR");
        start.setOnClickListener(v -> startVirtual());
        actions.addView(start, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button stop = button("■ DETENER");
        stop.setOnClickListener(v -> {
            stopVirtual(true);
            status.setText("Cámara virtual detenida");
        });
        actions.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(actions);

        return root;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse response = assetLoader.shouldInterceptRequest(request.getUrl());
                return response != null ? response : super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                urlEdit.setText(url);
                if (fallbackScript != null) view.evaluateJavascript(fallbackScript, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handlePermissionRequest(request));
            }
        });
    }

    private void pickMedia(String mime, int requestCode) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        startActivityForResult(i, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_PHOTO) selectSource(uri, true);
            else if (requestCode == REQ_VIDEO) selectSource(uri, false);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void selectSource(Uri uri, boolean photo) throws Exception {
        byte[] bytes = readAll(uri, photo ? MAX_PHOTO : MAX_VIDEO);
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = photo ? "image/jpeg" : "video/mp4";
        selectedMode = photo ? "photo" : "video";
        selectedDataUrl = "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        selectedLabel = photo ? "foto seleccionada" : "video seleccionado";
        status.setText("Fuente preparada: " + (photo ? "foto" : "video") + ". Pulsa ACTIVAR.");
    }

    private byte[] readAll(Uri uri, long maxBytes) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new Exception("archivo no disponible");
            byte[] buffer = new byte[65536];
            long total = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > maxBytes) throw new Exception("archivo demasiado grande para esta versión");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private void navigate() {
        String url = urlEdit.getText().toString().trim();
        if (url.isEmpty()) return;
        if (!url.startsWith("https://") && !url.startsWith("http://")) url = "https://" + url;
        if (url.startsWith("http://")) {
            Toast.makeText(this, "Usa HTTPS para las funciones de cámara.", Toast.LENGTH_LONG).show();
        }
        webView.loadUrl(url);
    }

    private void loadTestPage() {
        webView.loadUrl("https://appassets.androidplatform.net/assets/test_camera.html");
    }

    private int[] selectedResolution() {
        int p = resolutionSpinner.getSelectedItemPosition();
        if (p == 0) return new int[]{854, 480};
        if (p == 2) return new int[]{1920, 1080};
        return new int[]{1280, 720};
    }

    private void startVirtual() {
        if (selectedMode == null || selectedDataUrl == null) {
            Toast.makeText(this, "Primero selecciona una foto o un video.", Toast.LENGTH_SHORT).show();
            return;
        }
        int[] r = selectedResolution();
        String fit = fitSpinner.getSelectedItemPosition() == 0 ? "cover" : "contain";
        String script = buildVirtualScript(selectedMode, selectedDataUrl, r[0], r[1], 30, fit, micCheck.isChecked());

        stopVirtual(false);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(webView, script, Collections.singleton("*"));
            status.setText("ACTIVA: " + selectedLabel + " → " + r[0] + "×" + r[1] + ". Recargando…");
        } else {
            fallbackScript = script;
            status.setText("Modo compatible activo. Recargando…");
        }
        webView.reload();
    }

    private void stopVirtual(boolean reload) {
        if (scriptHandler != null) {
            scriptHandler.remove();
            scriptHandler = null;
        }
        fallbackScript = null;
        if (reload && webView != null) webView.reload();
    }

    private String buildVirtualScript(String mode, String dataUrl, int width, int height, int fps, String fit, boolean useMic) {
        String m = JSONObject.quote(mode);
        String d = JSONObject.quote(dataUrl);
        String f = JSONObject.quote(fit);
        return "(() => { 'use strict'; try {" +
                "const md=navigator.mediaDevices; if(!md||!md.getUserMedia)return;" +
                "if(!window.__VCM_GUM)window.__VCM_GUM=md.getUserMedia.bind(md);" +
                "if(!window.__VCM_ENUM&&md.enumerateDevices)window.__VCM_ENUM=md.enumerateDevices.bind(md);" +
                "const OG=window.__VCM_GUM,MODE=" + m + ",DATA=" + d + ",W=" + width + ",H=" + height + ",FPS=" + fps + ",FIT=" + f + ",MIC=" + useMic + ";" +
                "function rect(sw,sh){if(!sw||!sh)return{x:0,y:0,w:W,h:H};const sr=sw/sh,dr=W/H;let w,h;if((FIT==='cover'&&sr>dr)||(FIT!=='cover'&&sr<dr)){h=H;w=h*sr}else{w=W;h=w/sr}return{x:(W-w)/2,y:(H-h)/2,w:w,h:h}}" +
                "async function visual(){if(!HTMLCanvasElement.prototype.captureStream)throw new DOMException('captureStream no disponible','NotSupportedError');" +
                "const c=document.createElement('canvas');c.width=W;c.height=H;const x=c.getContext('2d',{alpha:false});if(!x)throw new DOMException('canvas no disponible','NotSupportedError');" +
                "const bg=()=>{x.fillStyle='#000';x.fillRect(0,0,W,H)};" +
                "if(MODE==='photo'){const i=new Image();i.src=DATA;await new Promise((ok,no)=>{i.onload=ok;i.onerror=()=>no(new DOMException('No se pudo cargar la foto','NotReadableError'))});" +
                "const draw=()=>{bg();const q=rect(i.naturalWidth,i.naturalHeight);x.drawImage(i,q.x,q.y,q.w,q.h);requestAnimationFrame(draw)};draw();}" +
                "else if(MODE==='video'){const v=document.createElement('video');v.src=DATA;v.loop=true;v.muted=true;v.playsInline=true;v.preload='auto';await new Promise((ok,no)=>{v.onloadedmetadata=ok;v.onerror=()=>no(new DOMException('No se pudo cargar el video','NotReadableError'))});try{await v.play()}catch(e){};" +
                "const draw=()=>{bg();if(v.readyState>=2){const q=rect(v.videoWidth,v.videoHeight);x.drawImage(v,q.x,q.y,q.w,q.h)}requestAnimationFrame(draw)};draw();}" +
                "return c.captureStream(FPS)}" +
                "const vg=async function(cons){const c=cons||{};if(!c.video)return OG(c);const vs=await visual();const out=new MediaStream();vs.getVideoTracks().forEach(t=>out.addTrack(t));" +
                "if(c.audio&&MIC){try{const a=await OG({audio:c.audio,video:false});a.getAudioTracks().forEach(t=>out.addTrack(t))}catch(e){out.getTracks().forEach(t=>t.stop());throw e}}return out};" +
                "try{Object.defineProperty(md,'getUserMedia',{configurable:true,writable:true,value:vg})}catch(e){md.getUserMedia=vg}" +
                "if(window.__VCM_ENUM){const ve=async()=>{const l=await window.__VCM_ENUM();if(l.some(z=>z&&z.deviceId==='virtualcam-mobile'))return l;return [{deviceId:'virtualcam-mobile',kind:'videoinput',label:'VirtualCam Mobile',groupId:'virtualcam-mobile',toJSON(){return this}},...l]};try{Object.defineProperty(md,'enumerateDevices',{configurable:true,writable:true,value:ve})}catch(e){md.enumerateDevices=ve}}" +
                "window.__VCM_ACTIVE=true;}catch(e){console.error('[VirtualCam Mobile]',e)}})();";
    }

    private void handlePermissionRequest(PermissionRequest request) {
        List<String> needed = new ArrayList<>();
        for (String res : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CAMERA);
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO);
        }
        if (needed.isEmpty()) {
            grantKnown(request);
        } else {
            if (pendingPermission != null) pendingPermission.deny();
            pendingPermission = request;
            requestPermissions(needed.toArray(new String[0]), REQ_PERMS);
        }
    }

    private void grantKnown(PermissionRequest request) {
        List<String> allow = new ArrayList<>();
        for (String res : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) allow.add(res);
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) allow.add(res);
        }
        if (allow.isEmpty()) request.deny(); else request.grant(allow.toArray(new String[0]));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS && pendingPermission != null) {
            PermissionRequest p = pendingPermission;
            pendingPermission = null;
            grantKnown(p);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (pendingPermission != null) pendingPermission.deny();
        if (scriptHandler != null) scriptHandler.remove();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
