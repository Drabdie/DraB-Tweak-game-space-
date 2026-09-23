package com.drabdie.tweak;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    final int TEXT = Ui.TEXT, MUTED = Ui.MUTED, ACCENT = Ui.ACCENT, GREEN = Ui.GREEN, ORANGE = Ui.ORANGE, RED = Ui.RED, CARD = Ui.CARD, BG = Ui.BG;

    LinearLayout content;
    TextView shizukuStatus, profileValue, cpuStat, ramStat, batteryStat, snapshotStatus;
    int selected = 0;
    final ProfileEngine engine = new ProfileEngine(this);
    final DeviceMonitor monitor = new DeviceMonitor();

    final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        build();
        handler.post(tick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) refreshStatuses();
    }

    void build() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(BG);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), 0);

        LinearLayout head = Ui.row(this);
        TextView title = Ui.tv(this, "DraB Tweak", 27, TEXT);
        title.setTypeface(null, 1);
        head.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        head.addView(Ui.tv(this, "2.0", 14, MUTED), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 42)));
        root.addView(head);

        TextView subtitle = Ui.tv(this, "NO-ROOT SYSTEM MANAGER", 11, ACCENT);
        subtitle.setLetterSpacing(.12f);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, Ui.dp(this, 24)));

        content = Ui.col(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        String[] tabs = {"⌂\nHome", "⊞\nApps", "◈\nMonitor", "⚙\nTools"};
        for (int i = 0; i < tabs.length; i++) {
            final int x = i;
            TextView n = Ui.tv(this, tabs[i], 12, i == 0 ? TEXT : MUTED);
            n.setGravity(Gravity.CENTER);
            n.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
            n.setOnClickListener(v -> {
                if (x == 0) showHome();
                else if (x == 1) showApps();
                else if (x == 2) showMonitor();
                else showTools();
            });
            nav.addView(n, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        }
        root.addView(nav);
        setContentView(root);
        showHome();
    }

    void clear() {
        content.removeAllViews();
    }

    // ============================ HOME ============================

    void showHome() {
        clear();
        Ui.section(this, content, "CURRENT PROFILE");
        LinearLayout hero = Ui.row(this);
        hero.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        hero.setBackground(Ui.bg(this, Ui.HERO, 20));
        LinearLayout htext = Ui.col(this);
        profileValue = Ui.tv(this, ProfileEngine.PROFILES[selected], 23, TEXT);
        profileValue.setTypeface(null, 1);
        htext.addView(profileValue);
        htext.addView(Ui.tv(this, ProfileEngine.PROFILE_DESC[selected], 12, MUTED));
        hero.addView(htext, new LinearLayout.LayoutParams(0, Ui.dp(this, 72), 1));
        hero.addView(Ui.tv(this, "●", 27, selected == 2 ? GREEN : ACCENT));
        content.addView(hero, new LinearLayout.LayoutParams(-1, Ui.dp(this, 102)));

        Ui.section(this, content, "QUICK PROFILES");
        LinearLayout grid = Ui.col(this);
        for (int i = 0; i < ProfileEngine.PROFILES.length; i += 2) {
            LinearLayout r = Ui.row(this);
            for (int j = i; j < i + 2 && j < ProfileEngine.PROFILES.length; j++) {
                final int k = j;
                String icon = j == 0 ? "⚡  " : j == 1 ? "◉  " : j == 2 ? "☾  " : "◈  ";
                TextView c = Ui.tv(this, icon + ProfileEngine.PROFILES[j], 14, j == selected ? TEXT : MUTED);
                c.setGravity(Gravity.CENTER_VERTICAL);
                c.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
                c.setBackground(Ui.bg(this, j == selected ? Ui.CARD_ACTIVE : CARD, 14));
                c.setOnClickListener(v -> applyProfile(k));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1);
                lp.setMargins(0, 0, Ui.dp(this, 6), Ui.dp(this, 6));
                r.addView(c, lp);
            }
            grid.addView(r);
        }
        content.addView(grid);

        Ui.section(this, content, "DEVICE STATUS");
        LinearLayout stats = Ui.row(this);
        cpuStat = statCard(stats, "CPU", "—", "loading");
        ramStat = statCard(stats, "RAM", "—", "loading");
        batteryStat = statCard(stats, "BATTERY", "—", "loading");
        content.addView(stats);

        Ui.section(this, content, "SHIZUKU ACCESS");
        LinearLayout sh = Ui.row(this);
        sh.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12));
        sh.setBackground(Ui.bg(this, CARD, 16));
        LinearLayout st = Ui.col(this);
        shizukuStatus = Ui.tv(this, "Checking Shizuku…", 14, TEXT);
        st.addView(shizukuStatus);
        st.addView(Ui.tv(this, "Shell-level actions, no root required", 11, MUTED));
        sh.addView(st, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        TextView act = Ui.button(this, "CONNECT", v -> requestShizuku());
        act.setTextSize(12);
        sh.addView(act, new LinearLayout.LayoutParams(Ui.dp(this, 104), Ui.dp(this, 42)));
        content.addView(sh, new LinearLayout.LayoutParams(-1, Ui.dp(this, 82)));

        Ui.section(this, content, "SAFETY");
        LinearLayout safety = Ui.card(this);
        snapshotStatus = Ui.tv(this, "No snapshot yet", 13, TEXT);
        safety.addView(snapshotStatus);
        safety.addView(Ui.tv(this, "Every settings change is captured before the first write; restore-all reverts them.", 11, MUTED));
        TextView restore = Ui.ghostButton(this, "RESTORE ALL SETTINGS", v -> restoreAll());
        restore.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, Ui.dp(this, 44));
        rl.topMargin = Ui.dp(this, 10);
        safety.addView(restore, rl);
        content.addView(safety);
        refreshStatuses();
    }

    TextView statCard(LinearLayout row, String name, String value, String sub) {
        LinearLayout s = Ui.card(this);
        s.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 5), Ui.dp(this, 8));
        s.addView(Ui.label(this, name));
        TextView v = Ui.tv(this, value, 20, TEXT);
        v.setTag(name);
        v.setTypeface(null, 1);
        s.addView(v);
        s.addView(Ui.tv(this, sub, 10, MUTED));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 92), 1);
        lp.setMargins(0, 0, Ui.dp(this, 6), 0);
        row.addView(s, lp);
        return v;
    }

    void applyProfile(int k) {
        selected = k;
        if (profileValue != null) {
            profileValue.setText(ProfileEngine.PROFILES[selected]);
        }
        if (!ShizukuExec.available()) {
            toast("Shizuku is not available - profile saved locally, nothing was changed");
            showHome();
            return;
        }
        ProfileEngine.ApplyResult r = engine.apply(k);
        StringBuilder sb = new StringBuilder();
        for (ProfileEngine.Change c : r.changes) {
            sb.append(c.ok ? "✓ " : "✗ ").append(c.key).append(" → ").append(c.value).append('\n');
        }
        new AlertDialog.Builder(this)
                .setTitle(r.allOk() ? ProfileEngine.PROFILES[k] + " applied" : ProfileEngine.PROFILES[k] + " partially applied")
                .setMessage(sb.toString() + (r.allOk() ? "" : "\nRefused changes were NOT faked."))
                .setPositiveButton("OK", null)
                .show();
        showHome();
    }

    void restoreAll() {
        if (!engine.snapshot().exists()) {
            toast("No snapshot to restore");
            return;
        }
        SnapshotStore.RestoreReport r = engine.restoreAll();
        toast("Restored " + r.restored + " setting(s)" + (r.failed > 0 ? ", " + r.failed + " failed: " + r.lastError : ""));
        refreshStatuses();
    }

    // ============================ APPS ============================

    void showApps() {
        clear();
        Ui.section(this, content, "INSTALLED APPS");
        content.addView(Ui.card(this, "App actions", "Force stop, disable, suspend, uninstall (data kept), standby buckets", "Executed through Shizuku as the shell user", ACCENT));
        Ui.section(this, content, "APPLICATIONS");

        List<ApplicationInfo> apps;
        try {
            apps = new ArrayList<>(getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA));
        } catch (Throwable t) {
            content.addView(Ui.card(this, "Cannot list apps", String.valueOf(t), "Query requires package visibility", ORANGE));
            return;
        }
        Collections.sort(apps, Comparator.comparing(
                ai -> String.valueOf(getPackageManager().getApplicationLabel(ai)).toLowerCase()));

        int shown = 0;
        for (ApplicationInfo ai : apps) {
            if (getPackageManager().getApplicationLabel(ai) == null) continue;
            content.addView(appRow(ai));
            shown++;
        }
        content.addView(Ui.tv(this, shown + " apps", 11, MUTED));
    }

    View appRow(ApplicationInfo ai) {
        final String pkg = ai.packageName;
        LinearLayout l = Ui.card(this);
        l.setPadding(Ui.dp(this, 14), Ui.dp(this, 11), Ui.dp(this, 14), Ui.dp(this, 11));

        LinearLayout top = Ui.row(this);
        TextView name = Ui.tv(this, String.valueOf(getPackageManager().getApplicationLabel(ai)), 15, TEXT);
        name.setTypeface(null, 1);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        top.addView(Ui.tv(this, system ? "SYSTEM" : "USER", 10, system ? ORANGE : GREEN));
        l.addView(top);

        boolean disabled = getPackageManager().getApplicationEnabledSetting(pkg)
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        boolean suspended = false;
        try {
            if (Build.VERSION.SDK_INT >= 24) suspended = getPackageManager().isPackageSuspended(pkg);
        } catch (Throwable ignored) {}
        String status = pkg + (disabled ? "  ·  DISABLED" : "") + (suspended ? "  ·  SUSPENDED" : "");
        l.addView(Ui.tv(this, status, 11, disabled || suspended ? RED : MUTED));

        l.setOnClickListener(v -> showAppActions(ai));
        return l;
    }

    void showAppActions(final ApplicationInfo ai) {
        final String pkg = ai.packageName;
        String label = String.valueOf(getPackageManager().getApplicationLabel(ai));
        String[] items = {
                "Force stop",
                "Disable (freeze)",
                "Re-enable",
                "Suspend",
                "Un-suspend",
                "Uninstall (keep data, user 0)",
                "Restore uninstalled (install-existing)",
                "Standby: restrict background",
                "Standby: active",
                "App ops…"
        };
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setMessage(pkg)
                .setItems(items, (d, which) -> {
                    if (which == 9) {
                        showAppOps(ai);
                        return;
                    }
                    final String cmd;
                    switch (which) {
                        case 0: cmd = "am force-stop " + ShizukuExec.safe(pkg); break;
                        case 1: cmd = "pm disable-user --user 0 " + ShizukuExec.safe(pkg); break;
                        case 2: cmd = "pm enable " + ShizukuExec.safe(pkg); break;
                        case 3: cmd = "pm suspend " + ShizukuExec.safe(pkg); break;
                        case 4: cmd = "pm unsuspend " + ShizukuExec.safe(pkg); break;
                        case 5: cmd = "pm uninstall -k --user 0 " + ShizukuExec.safe(pkg); break;
                        case 6: cmd = "cmd package install-existing " + ShizukuExec.safe(pkg); break;
                        case 7: cmd = "am set-standby-bucket " + ShizukuExec.safe(pkg) + " restricted"; break;
                        case 8: cmd = "am set-standby-bucket " + ShizukuExec.safe(pkg) + " active"; break;
                        default: return;
                    }
                    if (which == 5) {
                        confirm("Remove " + label + "?",
                                "Uninstalls for user 0, keeps APK and data. Restore any time with install-existing.",
                                () -> execAndToast(cmd));
                    } else {
                        execAndToast(cmd);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ============================ APP OPS ============================

    static final String[] OPS = {
            "CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_CLIPBOARD", "RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND", "WAKE_LOCK"
    };

    void showAppOps(final ApplicationInfo ai) {
        final String pkg = ai.packageName;
        ScrollView sv = new ScrollView(this);
        LinearLayout list = Ui.col(this);
        list.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        sv.addView(list);
        for (final String op : OPS) {
            LinearLayout row = Ui.card(this);
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout left = Ui.col(this);
            left.addView(Ui.tv(this, op, 13, TEXT));
            left.addView(Ui.tv(this, "appops state", 10, MUTED));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            TextView ignore = Ui.ghostButton(this, "IGNORE", v ->
                    execAndToast("cmd appops set " + ShizukuExec.safe(pkg) + " " + ShizukuExec.safe(op) + " ignore"));
            TextView allow = Ui.ghostButton(this, "DEFAULT", v ->
                    execAndToast("cmd appops set " + ShizukuExec.safe(pkg) + " " + ShizukuExec.safe(op) + " default"));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(this, 82), Ui.dp(this, 36));
            bp.leftMargin = Ui.dp(this, 6);
            row.addView(ignore, bp);
            row.addView(allow, new LinearLayout.LayoutParams(Ui.dp(this, 90), Ui.dp(this, 36)));
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        list.addView(Ui.tv(this, "Unsupported ops will fail honestly - OEM and Android version dependent.", 11, MUTED));
        new AlertDialog.Builder(this)
                .setTitle("App ops · " + getPackageManager().getApplicationLabel(ai))
                .setMessage(pkg)
                .setView(sv)
                .setPositiveButton("Close", null)
                .show();
    }

    // ============================ MONITOR ============================

    LinearLayout freqsBox;

    void showMonitor() {
        clear();
        Ui.section(this, content, "LIVE MONITOR");
        LinearLayout stats = Ui.row(this);
        cpuStat = statCard(stats, "CPU", "—", "load · /proc/stat");
        ramStat = statCard(stats, "RAM", "—", "used / total");
        batteryStat = statCard(stats, "BATTERY", "—", "%· °C · mA");
        content.addView(stats);
        Ui.section(this, content, "CPU FREQUENCIES (READ-ONLY)");
        freqsBox = Ui.card(this);
        freqsBox.addView(Ui.tv(this, "Reading sysfs…", 12, MUTED));
        content.addView(freqsBox);
        Ui.section(this, content, "STORAGE");
        storageBox = Ui.card(this);
        storageBox.addView(Ui.tv(this, "Reading…", 12, MUTED));
        content.addView(storageBox);
        Ui.section(this, content, "CAPABILITIES");
        content.addView(Ui.card(this, "✓  Available without root", "System info, battery, storage, app usage", "Public Android APIs", GREEN));
        content.addView(Ui.card(this, "◈  Shizuku (shell user)", "settings, pm, am, appops, deviceidle", "Depends on OEM / Android version", ACCENT));
        content.addView(Ui.card(this, "×  Root required", "CPU governor, GPU frequency, kernel /sys writes", "Never faked by this app", RED));
    }

    LinearLayout storageBox;

    // ============================ TOOLS ============================

    void showTools() {
        clear();
        Ui.section(this, content, "DEBLOAT");
        content.addView(toolRow("Debloat recommendations", "Curated catalog, data kept, restorable", () -> showDebloat()));
        Ui.section(this, content, "MEMORY & DOZE");
        content.addView(toolRow("Trim all app caches", "pm trim-caches (frees storage)", () -> execAndToast("pm trim-caches 1024G")));
        content.addView(toolRow("Kill background processes", "am kill-all (frees RAM)", () -> execAndToast("am kill-all")));
        content.addView(toolRow("Enter Doze now", "dumpsys deviceidle force-idle", () -> execAndToast("dumpsys deviceidle force-idle")));
        content.addView(toolRow("Step out of Doze", "dumpsys deviceidle step", () -> execAndToast("dumpsys deviceidle step")));
        Ui.section(this, content, "SAFETY");
        content.addView(toolRow("Restore all settings", "Reverts every captured change", this::restoreAll));
        content.addView(toolRow("Open animation settings", "System screen", () -> {
            try {
                startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
            } catch (Exception e) {
                toast("Not available");
            }
        }));
        Ui.section(this, content, "IMPORTANT");
        content.addView(Ui.card(this, "NO ROOT GUARANTEE", "Unsupported kernel controls are never faked or written", "This app will not damage your device", GREEN));
    }

    View toolRow(String title, String sub, Runnable action) {
        LinearLayout l = Ui.card(this);
        l.setPadding(Ui.dp(this, 16), Ui.dp(this, 13), Ui.dp(this, 16), Ui.dp(this, 13));
        TextView t = Ui.tv(this, title, 15, TEXT);
        t.setTypeface(null, 1);
        l.addView(t);
        l.addView(Ui.tv(this, sub, 12, MUTED));
        l.setOnClickListener(v -> action.run());
        return l;
    }

    void showDebloat() {
        ScrollView sv = new ScrollView(this);
        LinearLayout list = Ui.col(this);
        list.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        sv.addView(list);
        int found = 0;
        for (final DebloatList.Entry e : DebloatList.all()) {
            try {
                getPackageManager().getApplicationInfo(e.pkg, 0);
            } catch (PackageManager.NameNotFoundException notInstalled) {
                continue;
            }
            found++;
            LinearLayout row = Ui.card(this);
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout left = Ui.col(this);
            TextView t = Ui.tv(this, e.pkg, 12, TEXT);
            t.setTypeface(null, 1);
            left.addView(t);
            left.addView(Ui.tv(this, e.note, 11, MUTED));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            int color = e.risk == DebloatList.SAFE ? GREEN : e.risk == DebloatList.OPTIONAL ? ORANGE : RED;
            row.addView(Ui.tv(this, e.risk == DebloatList.SAFE ? "SAFE" : e.risk == DebloatList.OPTIONAL ? "OPTIONAL" : "RISKY", 10, color));
            row.setOnClickListener(v -> confirm("Remove " + e.pkg + "?",
                    e.note + ".\n\nData and APK are kept - restore with:\ncmd package install-existing " + e.pkg,
                    () -> execAndToast("pm uninstall -k --user 0 " + ShizukuExec.safe(e.pkg))));
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        if (found == 0) {
            list.addView(Ui.tv(this, "None of the catalog entries are installed on this device.", 13, MUTED));
        }
        new AlertDialog.Builder(this)
                .setTitle("Debloat catalog · " + found + " installed")
                .setView(sv)
                .setPositiveButton("Close", null)
                .show();
    }

    // ============================ PLUMBING ============================

    void execAndToast(String cmd) {
        if (!ShizukuExec.available()) {
            toast("Shizuku is not available - connect it first");
            return;
        }
        ShizukuExec.Result r = ShizukuExec.run(cmd);
        toast(r.summary());
        refreshStatuses();
    }

    void confirm(String title, String message, final Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Continue", (d, w) -> action.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    void refreshStatuses() {
        updateShizuku();
        if (snapshotStatus != null) {
            int n = engine.snapshot().size();
            snapshotStatus.setText(n == 0 ? "No snapshot yet - nothing has been changed" : n + " setting(s) captured · restore available");
            snapshotStatus.setTextColor(n == 0 ? MUTED : GREEN);
        }
    }

    void updateShizuku() {
        if (shizukuStatus == null) return;
        try {
            boolean alive = Shizuku.pingBinder();
            boolean granted = alive && ShizukuExec.available();
            shizukuStatus.setText(granted ? "Shizuku is ready" : alive ? "Shizuku runs - permission needed" : "Shizuku is not running");
            shizukuStatus.setTextColor(granted ? GREEN : ORANGE);
        } catch (Throwable e) {
            shizukuStatus.setText("Install Shizuku to unlock tools");
            shizukuStatus.setTextColor(ORANGE);
        }
    }

    void requestShizuku() {
        try {
            if (!Shizuku.pingBinder()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")));
                return;
            }
            ShizukuExec.requestPermission();
            updateShizuku();
        } catch (Throwable e) {
            Toast.makeText(this, "Install and start Shizuku first", Toast.LENGTH_LONG).show();
        }
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    int freqTick = 0;

    final Runnable tick = new Runnable() {
        @Override public void run() {
            try {
                if (cpuStat != null) {
                    int load = monitor.cpuLoad();
                    cpuStat.setText(load < 0 ? "…" : load + "%");
                }
                if (ramStat != null) {
                    long[] ram = DeviceMonitor.ram(MainActivity.this);
                    ramStat.setText((ram[1] - ram[0]) / 1048576L + "/" + ram[1] / 1048576L + "M");
                }
                if (batteryStat != null) {
                    Object[] b = DeviceMonitor.battery(MainActivity.this);
                    int level = (Integer) b[0];
                    int tempTenths = (Integer) b[1];
                    long microA = (Long) b[2];
                    String s = (level < 0 ? "—" : level + "%");
                    if (tempTenths > 0) s += " · " + (tempTenths / 10f) + "°C";
                    if (microA != Long.MIN_VALUE && microA != 0) s += " · " + (microA / 1000) + "mA";
                    batteryStat.setText(s);
                }
                // Heavy sysfs reads every 5s only.
                if (freqsBox != null && freqTick++ % 5 == 0) {
                    freqsBox.removeAllViews();
                    int avg = monitor.avgFreqMhz();
                    int max = monitor.maxFreqMhz();
                    freqsBox.addView(Ui.tv(MainActivity.this, avg < 0 ? "Frequencies not exposed by OEM (normal without root)" : "Avg " + avg + " MHz · Max " + max + " MHz · " + monitor.cpuCount() + " cores", 13, TEXT));
                    if (avg >= 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int c = 0; c < monitor.cpuCount(); c++) {
                            long f = monitor.coreFreq(c);
                            if (f > 0) {
                                if (sb.length() > 0) sb.append("  ");
                                sb.append(c).append(":").append(f / 1000).append("MHz");
                            }
                        }
                        freqsBox.addView(Ui.tv(MainActivity.this, sb.toString(), 11, MUTED));
                    }
                }
                if (storageBox != null && freqTick % 5 == 1) {
                    long[] st = DeviceMonitor.storage();
                    if (st[1] > 0) {
                        storageBox.removeAllViews();
                        storageBox.addView(Ui.tv(MainActivity.this, (st[1] - st[0]) / (1L << 30) + "/" + st[1] / (1L << 30) + " GB used · " + st[0] / (1L << 30) + " GB free", 13, TEXT));
                    }
                }
            } catch (Throwable ignored) {}
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        ShizukuExec.shutdown(this);
        super.onDestroy();
    }
}
