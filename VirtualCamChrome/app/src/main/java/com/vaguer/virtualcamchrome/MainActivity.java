package com.vaguer.virtualcamchrome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.browser.customtabs.CustomTabsIntent;

public class MainActivity extends Activity {
    private static final String AFORE_URL = "https://www.aforeweb.com.mx/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(36), dp(22), dp(22));

        TextView title = new TextView(this);
        title.setText("AforeWeb en Chrome");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("Esta versión abre AforeWeb usando el Chrome real del teléfono mediante una pestaña segura del navegador.");
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(18), 0, dp(26));
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));

        Button open = new Button(this);
        open.setText("ABRIR AFOREWEB");
        open.setTextSize(17);
        open.setOnClickListener(v -> openAforeWeb());
        root.addView(open, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView note = new TextView(this);
        note.setText("Si Chrome solicita cámara o micrófono, Android mostrará el permiso correspondiente dentro del navegador real.");
        note.setTextSize(14);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void openAforeWeb() {
        Uri uri = Uri.parse(AFORE_URL);
        CustomTabsIntent tabs = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build();
        tabs.intent.setPackage("com.android.chrome");
        try {
            tabs.launchUrl(this, uri);
        } catch (ActivityNotFoundException e) {
            tabs.intent.setPackage(null);
            tabs.launchUrl(this, uri);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
