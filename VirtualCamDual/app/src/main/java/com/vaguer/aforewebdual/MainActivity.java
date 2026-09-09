package com.vaguer.aforewebdual;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String AFOREWEB_URL = "https://www.aforeweb.com.mx/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        int pad = dp(22);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.bg));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(28), pad, dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("AforeWeb Dual", 30, true, R.color.text);
        root.addView(title);
        TextView subtitle = text("Dos funciones separadas: AforeWeb en Chrome real y una cámara virtual de prueba dentro de esta app.", 16, false, R.color.muted);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, lp(-1, -2, 0, 10, 0, 24));

        root.addView(card("1. AforeWeb en Chrome real",
                "Abre el portal oficial directamente en la aplicación Chrome del teléfono. El inicio de sesión, permisos y cámara son manejados por Chrome y Android.",
                "ABRIR AFOREWEB EN CHROME", true));
        root.addView(card("2. Cámara virtual de prueba",
                "Elige una foto o un video y la página interna de pruebas lo convierte en un MediaStream para comprobar el funcionamiento. Este modo no se inyecta en Chrome ni en AforeWeb.",
                "ABRIR MODO DE PRUEBA", false), lp(-1, -2, 0, 18, 0, 0));

        TextView note = text("Versión 1.1.0 • Sin Firebase • Sin bot • Sin base de datos", 13, false, R.color.muted);
        note.setGravity(Gravity.CENTER);
        root.addView(note, lp(-1, -2, 0, 28, 0, 0));
        return scroll;
    }

    private View card(String heading, String body, String buttonText, boolean chrome) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.card);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));

        box.addView(text(heading, 20, true, R.color.text));
        box.addView(text(body, 15, false, R.color.muted), lp(-1, -2, 0, 8, 0, 16));

        Button button = new Button(this);
        button.setText(buttonText);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(chrome ? R.drawable.button_green : R.drawable.button_blue);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setOnClickListener(v -> {
            if (chrome) openAforeWebInChrome();
            else startActivity(new Intent(this, TestCameraActivity.class));
        });
        box.addView(button, lp(-1, dp(54), 0, 0, 0, 0));
        return box;
    }

    private void openAforeWebInChrome() {
        Uri uri = Uri.parse(AFOREWEB_URL);
        Intent chrome = new Intent(Intent.ACTION_VIEW, uri);
        chrome.setPackage("com.android.chrome");
        chrome.addCategory(Intent.CATEGORY_BROWSABLE);
        chrome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(chrome);
        } catch (ActivityNotFoundException e) {
            Intent fallback = new Intent(Intent.ACTION_VIEW, uri);
            fallback.addCategory(Intent.CATEGORY_BROWSABLE);
            try {
                startActivity(fallback);
            } catch (ActivityNotFoundException noBrowser) {
                Toast.makeText(this, "No se encontró un navegador compatible.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private TextView text(String value, int sp, boolean bold, int colorRes) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(getColor(colorRes));
        t.setLineSpacing(0f, 1.12f);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
