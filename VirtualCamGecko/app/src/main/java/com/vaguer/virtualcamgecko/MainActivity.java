package com.vaguer.virtualcamgecko;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {
    private static final String AFORE_URL = "https://www.aforeweb.com.mx/";
    private static GeckoRuntime runtime;
    private GeckoSession session;
    private EditText address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (runtime == null) {
            runtime = GeckoRuntime.create(this);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(4), dp(4), dp(4), dp(4));

        Button back = new Button(this);
        back.setText("←");
        back.setOnClickListener(v -> {
            if (session != null) session.goBack();
        });
        top.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));

        address = new EditText(this);
        address.setSingleLine(true);
        address.setText(AFORE_URL);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigate();
                return true;
            }
            return false;
        });
        top.addView(address, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button go = new Button(this);
        go.setText("IR");
        go.setOnClickListener(v -> navigate());
        top.addView(go, new LinearLayout.LayoutParams(dp(60), dp(48)));

        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        GeckoView view = new GeckoView(this);
        root.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        session = new GeckoSession();
        session.open(runtime);
        view.setSession(session);
        session.loadUri(AFORE_URL);
    }

    private void navigate() {
        String url = address.getText().toString().trim();
        if (url.isEmpty()) return;
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "https://" + url;
        }
        session.loadUri(url);
    }

    @Override
    public void onBackPressed() {
        if (session != null) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
