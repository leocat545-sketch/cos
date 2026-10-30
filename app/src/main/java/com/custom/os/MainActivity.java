package com.custom.os;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PICK = 1, HOME_REQ = 2;
    static final int BLUE = 0xFF0B1FA0, FG = 0xFFCFD9FF;
    LinearLayout root;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        menu();
        if (b == null && !isHome()) askHome();
    }

    boolean isHome() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.app.role.RoleManager rm = (android.app.role.RoleManager) getSystemService(Context.ROLE_SERVICE);
                return rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME);
            }
            ResolveInfo r = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            return r != null && getPackageName().equals(r.activityInfo.packageName);
        } catch (Exception e) { return false; }
    }

    // Demande à Android de définir COs comme écran d'accueil par défaut
    void askHome() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.app.role.RoleManager rm = (android.app.role.RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                    startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), HOME_REQ);
                    return;
                }
            }
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Paramètres > Apps > Apps par défaut > Application d'accueil", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onNewIntent(Intent i) { // appui sur Accueil
        super.onNewIntent(i);
        menu();
    }

    @Override public void onBackPressed() { menu(); }

    void screen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLUE);
        int p = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(root);
        setContentView(sv);
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(FG); t.setTextSize(size);
        t.setTypeface(Typeface.MONOSPACE);
        t.setPadding(0, 0, 0, 16);
        root.addView(t);
        return t;
    }

    Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(FG); b.setTextSize(22);
        b.setAllCaps(false); b.setTypeface(Typeface.MONOSPACE);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setBackgroundColor(0x33FFFFFF);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, (int) (64 * getResources().getDisplayMetrics().density));
        lp.setMargins(0, 0, 0, 16);
        root.addView(b, lp);
        return b;
    }

    void menu() {
        screen();
        text("COs BIOS v0.1\nChoisis un système :", 24);
        button("Cocoon", v -> launch("cocoon", "Cocoon"));
        button("COs", v -> cos());
        if (!isHome()) button("Définir COs comme launcher par défaut", v -> askHome());
    }

    void cos() {
        screen();
        text("COs", 28);
        button("Ouvrir un fichier (.apk / .exe)", v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK);
        });
        button("Mes apps", v -> apps());
        button("Retour au menu", v -> menu());
    }

    // Retrouve une app par nom (ou paquet), parmi les apps normales et les launchers
    ResolveInfo find(String key) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> all = new ArrayList<>();
        all.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));
        all.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0));
        for (ResolveInfo r : all) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String label = r.loadLabel(pm).toString().toLowerCase();
            if (label.contains(key) || pkg.toLowerCase().contains(key)) return r;
        }
        return null;
    }

    boolean launch(String key, String nom) {
        ResolveInfo r = find(key);
        if (r == null) {
            Toast.makeText(this, nom + " n’est pas installé.", Toast.LENGTH_LONG).show();
            return false;
        }
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); return true; }
        catch (Exception e) { Toast.makeText(this, "Impossible d’ouvrir " + nom, Toast.LENGTH_LONG).show(); return false; }
    }

    void apps() {
        screen();
        text("Mes apps", 28);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> l = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        Collections.sort(l, (a, c) -> a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
        for (ResolveInfo r : l) {
            if (r.activityInfo.packageName.equals(getPackageName())) continue;
            button(r.loadLabel(pm).toString(), v -> {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            });
        }
        button("Retour", v -> cos());
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || res != RESULT_OK || data == null) return;
        Uri u = data.getData();
        String name = "";
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        } catch (Exception ignored) {}
        String n = name.toLowerCase();
        if (n.endsWith(".apk")) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(u, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(i); }
            catch (Exception e) { Toast.makeText(this, "Autorise COs à installer des apps (Paramètres).", Toast.LENGTH_LONG).show(); }
        } else if (n.endsWith(".exe")) {
            Toast.makeText(this, "Les .exe passent par Winlator : ouvre-le puis choisis ce fichier.", Toast.LENGTH_LONG).show();
            launch("winlator", "Winlator");
        } else {
            Toast.makeText(this, "Format non pris en charge : " + name, Toast.LENGTH_LONG).show();
        }
    }
}
