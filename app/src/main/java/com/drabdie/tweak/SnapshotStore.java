package com.drabdie.tweak;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Safety net: before the app changes any global setting through Shizuku,
 * the original value is captured here (settings get). "Restore all" puts
 * every captured value back. Everything is stored locally as JSON.
 */
public final class SnapshotStore {

    private final Context ctx;
    private final File file;

    public SnapshotStore(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.file = new File(this.ctx.getFilesDir(), "settings_snapshot.json");
    }

    /** Returns the previous value of the key (from the snapshot) or null if absent. */
    public synchronized String get(String key) {
        JSONObject o = load();
        return o.optString(key, null);
    }

    /**
     * Captures the current value of key if it is not captured yet.
     * Returns the captured (original) value, or null when reading failed.
     */
    public synchronized String captureIfAbsent(String key) {
        JSONObject o = load();
        if (o.has(key)) return o.optString(key, null);
        String current = readSetting(key);
        if (current == null) return null;
        try { o.put(key, current.trim()); } catch (Exception ignored) {}
        save(o);
        return current.trim();
    }

    /** Number of captured settings. */
    public synchronized int size() {
        return load().length();
    }

    /** True when at least one setting is captured. */
    public synchronized boolean exists() {
        return load().length() > 0;
    }

    public static final class RestoreReport {
        public int restored, failed;
        public String lastError = "";
    }

    /** Restores every captured value, then clears the snapshot. */
    public synchronized RestoreReport restoreAll() {
        RestoreReport report = new RestoreReport();
        JSONObject o = load();
        Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = o.optString(key, null);
            ShizukuExec.Result r = ShizukuExec.run(
                    "settings put global " + ShizukuExec.safe(key) + " " + ShizukuExec.safe(value));
            if (r.ok) {
                report.restored++;
                keys.remove();
            } else {
                report.failed++;
                report.lastError = r.summary();
            }
        }
        save(o);
        return report;
    }

    private String readSetting(String key) {
        ShizukuExec.Result r = ShizukuExec.run("settings get global " + ShizukuExec.safe(key));
        if (!r.ok) return null;
        String v = r.out;
        if (v.isEmpty() || "null".equals(v)) return "0";
        return v;
    }

    private JSONObject load() {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int read = fis.read(buf);
            return new JSONObject(new String(buf, 0, Math.max(read, 0), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void save(JSONObject o) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }
}
