package com.vaguer.virtualcamshizuku;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int REQ_SHIZUKU = 4011;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView statusView;
    private TextView cameraView;
    private TextView reportView;
    private Button permissionButton;
    private Button diagnosticButton;

    private ICamProbe probeService;
    private Shizuku.UserServiceArgs serviceArgs;
    private boolean binding;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener = () ->
            runOnUiThread(this::refreshShizukuStatus);

    private final Shizuku.OnBinderDeadListener binderDeadListener = () ->
            runOnUiThread(() -> {
                probeService = null;
                binding = false;
                refreshShizukuStatus();
            });

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener = (requestCode, grantResult) -> {
        if (requestCode != REQ_SHIZUKU) return;
        runOnUiThread(() -> {
            refreshShizukuStatus();
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso Shizuku concedido", Toast.LENGTH_SHORT).show();
                bindProbeService(true);
            } else {
                Toast.makeText(this, "Permiso Shizuku rechazado", Toast.LENGTH_LONG).show();
            }
        });
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binding = false;
            probeService = ICamProbe.Stub.asInterface(service);
            runOnUiThread(() -> {
                appendReport("\nServicio privilegiado conectado.\n");
                diagnosticButton.setEnabled(true);
            });
            runRemoteProbe();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            probeService = null;
            binding = false;
            runOnUiThread(() -> {
                appendReport("\nServicio privilegiado desconectado.\n");
                refreshShizukuStatus();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceArgs = new Shizuku.UserServiceArgs(new ComponentName(this, CamProbeService.class))
                .processNameSuffix("cam_probe")
                .tag("virtualcam_cam_probe")
                .version(12)
                .debuggable(true)
                .daemon(false);

        setContentView(buildUi());

        Shizuku.addBinderReceivedListener(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionResultListener);

        refreshShizukuStatus();
        inspectLocalCameras();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshShizukuStatus();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        if (probeService != null && serviceArgs != null) {
            try {
                Shizuku.unbindUserService(serviceArgs, serviceConnection, true);
            } catch (Throwable ignored) {}
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(17, 19, 24));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("VirtualCam Shizuku Check", 28, true, Color.WHITE);
        root.addView(title);

        TextView sub = text("Diagnóstico real de privilegios ADB/root y del sistema de cámaras de Android.", 15, false, Color.rgb(190, 196, 207));
        root.addView(sub, margins(0, 6, 0, 18));

        statusView = panelText();
        root.addView(statusView);

        permissionButton = button("AUTORIZAR CON SHIZUKU", Color.rgb(86, 77, 214));
        permissionButton.setOnClickListener(v -> requestShizukuPermission());
        root.addView(permissionButton, margins(0, 14, 0, 8));

        diagnosticButton = button("EJECUTAR DIAGNÓSTICO COMPLETO", Color.rgb(11, 122, 83));
        diagnosticButton.setOnClickListener(v -> bindProbeService(true));
        root.addView(diagnosticButton, margins(0, 0, 0, 18));

        root.addView(sectionTitle("Cámaras que Android expone a las apps"));
        cameraView = panelText();
        cameraView.setTypeface(Typeface.MONOSPACE);
        root.addView(cameraView, margins(0, 8, 0, 18));

        root.addView(sectionTitle("Reporte privilegiado"));
        reportView = panelText();
        reportView.setTypeface(Typeface.MONOSPACE);
        reportView.setTextSize(12);
        reportView.setText("Todavía no se ha ejecutado el diagnóstico.\n");
        root.addView(reportView, margins(0, 8, 0, 10));

        Button copy = button("COPIAR REPORTE", Color.rgb(58, 63, 74));
        copy.setOnClickListener(v -> copyReport());
        root.addView(copy);

        TextView note = text("Esta versión no registra una cámara virtual. Su función es comprobar exactamente qué acceso concede Shizuku/Sui en este teléfono antes de construir el módulo de cámara del sistema.", 13, false, Color.rgb(170, 176, 187));
        root.addView(note, margins(0, 18, 0, 0));

        return scroll;
    }

    private void requestShizukuPermission() {
        if (!safePing()) {
            Toast.makeText(this, "Shizuku no está activo. Inícialo primero.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "La app ya está autorizada", Toast.LENGTH_SHORT).show();
                bindProbeService(true);
                return;
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Toast.makeText(this, "Abre Shizuku > Aplicaciones autorizadas y permite esta app.", Toast.LENGTH_LONG).show();
                return;
            }
            Shizuku.requestPermission(REQ_SHIZUKU);
        } catch (Throwable t) {
            Toast.makeText(this, "No se pudo solicitar permiso: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshShizukuStatus() {
        StringBuilder s = new StringBuilder();
        boolean alive = safePing();
        s.append("Shizuku: ").append(alive ? "ACTIVO" : "NO CONECTADO").append('\n');
        if (alive) {
            try {
                int uid = Shizuku.getUid();
                int version = Shizuku.getVersion();
                String context = Shizuku.getSELinuxContext();
                boolean granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;

                s.append("API del servidor: ").append(version).append('\n');
                s.append("UID del servidor: ").append(uid).append('\n');
                s.append("Modo: ").append(uid == 0 ? "ROOT / SUI" : uid == 2000 ? "ADB SHELL" : "UID " + uid).append('\n');
                s.append("SELinux: ").append(context == null ? "desconocido" : context).append('\n');
                s.append("Permiso de esta app: ").append(granted ? "AUTORIZADO" : "PENDIENTE").append('\n');

                if (granted) {
                    s.append("Permiso remoto DUMP: ").append(permissionName("android.permission.DUMP")).append('\n');
                    s.append("Permiso remoto CAMERA: ").append(permissionName("android.permission.CAMERA")).append('\n');
                    s.append("Permiso remoto INTERACT_ACROSS_USERS_FULL: ")
                            .append(permissionName("android.permission.INTERACT_ACROSS_USERS_FULL")).append('\n');
                }

                s.append('\n');
                if (uid == 0) {
                    s.append("Resultado preliminar: acceso root detectado. Podemos probar integración de sistema más profunda.");
                } else if (uid == 2000) {
                    s.append("Resultado preliminar: Shizuku está funcionando con privilegios ADB shell. Es más que una APK normal, pero no equivale a root completo.");
                }

                permissionButton.setText(granted ? "SHIZUKU AUTORIZADO" : "AUTORIZAR CON SHIZUKU");
                diagnosticButton.setEnabled(granted);
            } catch (Throwable t) {
                s.append("Error leyendo Shizuku: ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
                diagnosticButton.setEnabled(false);
            }
        } else {
            permissionButton.setText("ABRE / INICIA SHIZUKU");
            diagnosticButton.setEnabled(false);
        }
        statusView.setText(s.toString());
    }

    private String permissionName(String permission) {
        try {
            return Shizuku.checkRemotePermission(permission) == PackageManager.PERMISSION_GRANTED ? "SÍ" : "NO";
        } catch (Throwable t) {
            return "ERROR";
        }
    }

    private void inspectLocalCameras() {
        executor.execute(() -> {
            StringBuilder out = new StringBuilder();
            try {
                CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String[] ids = cm.getCameraIdList();
                out.append("Total: ").append(ids.length).append("\n\n");
                for (String id : ids) {
                    CameraCharacteristics c = cm.getCameraCharacteristics(id);
                    Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                    Integer level = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    Size pixels = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    out.append("ID ").append(id).append("\n");
                    out.append("  Lente: ").append(lensName(facing)).append("\n");
                    out.append("  Nivel: ").append(levelName(level)).append("\n");
                    if (pixels != null) out.append("  Sensor: ").append(pixels.getWidth()).append("x").append(pixels.getHeight()).append("\n");
                }
            } catch (Throwable t) {
                out.append("ERROR Camera2: ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
            }
            runOnUiThread(() -> cameraView.setText(out.toString()));
        });
    }

    private void bindProbeService(boolean autoRun) {
        if (!safePing()) {
            Toast.makeText(this, "Shizuku no está activo", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                requestShizukuPermission();
                return;
            }
            if (probeService != null) {
                if (autoRun) runRemoteProbe();
                return;
            }
            if (binding) return;
            binding = true;
            reportView.setText("Conectando servicio privilegiado de Shizuku...\n");
            Shizuku.bindUserService(serviceArgs, serviceConnection);
        } catch (Throwable t) {
            binding = false;
            appendReport("ERROR al enlazar UserService: " + t.getClass().getSimpleName() + ": " + t.getMessage() + "\n");
        }
    }

    private void runRemoteProbe() {
        ICamProbe service = probeService;
        if (service == null) return;
        diagnosticButton.setEnabled(false);
        reportView.setText("Ejecutando comandos con la identidad de Shizuku...\n\n");
        executor.execute(() -> {
            String result;
            try {
                result = "=== IDENTIDAD DEL USER SERVICE ===\n" + service.getIdentity() +
                        "\n=== SONDEO DE CÁMARA ===\n" + service.runCameraProbe();
            } catch (Throwable t) {
                result = "ERROR remoto: " + t.getClass().getSimpleName() + ": " + t.getMessage();
            }
            String finalResult = result;
            runOnUiThread(() -> {
                reportView.setText(finalResult);
                diagnosticButton.setEnabled(true);
            });
        });
    }

    private void copyReport() {
        String full = statusView.getText() + "\n\n=== CAMERA2 LOCAL ===\n" + cameraView.getText() +
                "\n\n=== SHIZUKU PROBE ===\n" + reportView.getText();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("VirtualCam Shizuku report", full));
        Toast.makeText(this, "Reporte copiado", Toast.LENGTH_SHORT).show();
    }

    private boolean safePing() {
        try { return Shizuku.pingBinder(); } catch (Throwable t) { return false; }
    }

    private void appendReport(String text) {
        reportView.append(text);
    }

    private String lensName(Integer facing) {
        if (facing == null) return "desconocido";
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) return "FRONTAL";
        if (facing == CameraCharacteristics.LENS_FACING_BACK) return "TRASERA";
        if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) return "EXTERNA";
        return String.valueOf(facing);
    }

    private String levelName(Integer level) {
        if (level == null) return "desconocido";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) return "LEGACY";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) return "LIMITED";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) return "FULL";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) return "LEVEL_3";
        if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL) return "EXTERNAL";
        return String.valueOf(level);
    }

    private TextView sectionTitle(String value) {
        return text(value, 17, true, Color.WHITE);
    }

    private TextView panelText() {
        TextView t = text("Cargando...", 14, false, Color.rgb(226, 229, 235));
        t.setBackgroundColor(Color.rgb(31, 34, 41));
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        t.setTextIsSelectable(true);
        return t;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundColor(color);
        b.setMinHeight(dp(54));
        return b;
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.12f);
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
