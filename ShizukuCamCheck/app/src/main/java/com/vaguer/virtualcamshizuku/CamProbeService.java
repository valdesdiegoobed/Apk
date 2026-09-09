package com.vaguer.virtualcamshizuku;

import android.content.Context;
import android.system.Os;

import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CamProbeService extends ICamProbe.Stub {

    public CamProbeService() {}

    @Keep
    public CamProbeService(Context context) {}

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public String getIdentity() {
        StringBuilder sb = new StringBuilder();
        sb.append("PID: ").append(Os.getpid()).append('\n');
        sb.append("UID: ").append(Os.getuid()).append('\n');
        sb.append("GID: ").append(Os.getgid()).append('\n');
        sb.append(exec("id"));
        sb.append("SELinux: ").append(exec("getenforce").trim()).append('\n');
        return sb.toString();
    }

    @Override
    public String runCameraProbe() {
        String cmd = "echo '=== IDENTIDAD ==='; id; " +
                "echo '=== DISPOSITIVO ==='; " +
                "echo Fabricante: $(getprop ro.product.manufacturer); " +
                "echo Modelo: $(getprop ro.product.model); " +
                "echo Android: $(getprop ro.build.version.release); " +
                "echo SDK: $(getprop ro.build.version.sdk); " +
                "echo ABI: $(getprop ro.product.cpu.abi); " +
                "echo '=== SELINUX ==='; getenforce; " +
                "echo '=== SERVICIOS CAMERA ==='; service list | grep -i camera || true; " +
                "echo '=== NODOS VIDEO ==='; ls -l /dev/video* 2>/dev/null || echo 'Sin /dev/video visibles'; " +
                "echo '=== PROPIEDADES CAMERA ==='; getprop | grep -i camera | head -n 80 || true; " +
                "echo '=== DUMPSYS MEDIA.CAMERA ==='; dumpsys media.camera 2>&1 | head -n 260";
        return exec(cmd);
    }

    private String exec(String command) {
        StringBuilder out = new StringBuilder();
        try {
            Process process = new ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int chars = 0;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                    chars += line.length() + 1;
                    if (chars > 180000) {
                        out.append("\n[Salida recortada por seguridad]\n");
                        break;
                    }
                }
            }
            process.waitFor();
            out.append("\nCódigo de salida: ").append(process.exitValue()).append('\n');
        } catch (Throwable t) {
            out.append("ERROR: ").append(t.getClass().getSimpleName()).append(": ")
                    .append(t.getMessage()).append('\n');
        }
        return out.toString();
    }
}
