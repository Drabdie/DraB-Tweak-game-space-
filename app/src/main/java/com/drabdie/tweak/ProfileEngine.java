package com.drabdie.tweak;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies profiles through real shell commands (via Shizuku, shell user).
 * The app never claims success when the system refused the change.
 *
 * Kernel / CPU governor / GPU frequency writes stay OUT of scope on purpose:
 * the shell user cannot write most sysfs nodes - those need root.
 */
public final class ProfileEngine {

    public static final String[] PROFILES = {"Performance", "Balanced", "Battery", "Game Mode"};
    public static final String[] PROFILE_DESC = {
            "0.5x animations, battery saver off, background tasks killed",
            "System defaults (1.0x animations, saver off)",
            "Battery saver on, 0.25x animations, standby enforced",
            "0.5x animations, saver off, background tasks killed"
    };

    /** One applied setting change. */
    public static final class Change {
        public final String key, value;
        public final boolean ok;
        public final String detail;

        Change(String key, String value, boolean ok, String detail) {
            this.key = key;
            this.value = value;
            this.ok = ok;
            this.detail = detail;
        }
    }

    /** Result of applying a profile. */
    public static final class ApplyResult {
        public final List<Change> changes = new ArrayList<>();
        public boolean allOk() {
            for (Change c : changes) if (!c.ok) return false;
            return true;
        }
        public int failures() {
            int n = 0;
            for (Change c : changes) if (!c.ok) n++;
            return n;
        }
    }

    private final SnapshotStore snapshot;

    public ProfileEngine(Context ctx) {
        this.snapshot = new SnapshotStore(ctx);
    }

    public SnapshotStore snapshot() {
        return snapshot;
    }

    /** Reads the current animation scale (for UI display). */
    public String read(String key) {
        ShizukuExec.Result r = ShizukuExec.run("settings get global " + ShizukuExec.safe(key));
        return r.ok && !r.out.isEmpty() && !"null".equals(r.out) ? r.out : "—";
    }

    private void put(ApplyResult res, String key, String value) {
        snapshot.captureIfAbsent(key);
        ShizukuExec.Result r = ShizukuExec.run(
                "settings put global " + ShizukuExec.safe(key) + " " + ShizukuExec.safe(value));
        res.changes.add(new Change(key, value, r.ok, r.summary()));
    }

    /** Applies one of the four profiles. */
    public ApplyResult apply(int profileIndex) {
        ApplyResult res = new ApplyResult();
        String anim;
        String saver;
        switch (profileIndex) {
            case 0: // Performance
            case 3: // Game Mode
                anim = "0.5"; saver = "0";
                break;
            case 2: // Battery
                anim = "0.25"; saver = "1";
                break;
            default: // Balanced
                anim = "1.0"; saver = "0";
                break;
        }
        put(res, "window_animation_scale", anim);
        put(res, "transition_animation_scale", anim);
        put(res, "animator_duration_scale", anim);
        put(res, "low_power", saver);
        if (profileIndex == 2) {
            // Enforce app standby for background apps (real command, may be refused by OEM).
            ShizukuExec.Result r = ShizukuExec.run("settings put global app_standby_enabled 1");
            snapshot.captureIfAbsent("app_standby_enabled");
            res.changes.add(new Change("app_standby_enabled", "1", r.ok, r.summary()));
        }
        if (profileIndex == 0 || profileIndex == 3) {
            // Kill all background processes - real and immediate.
            ShizukuExec.Result r = ShizukuExec.run("am kill-all");
            res.changes.add(new Change("<background processes>", "killed", r.ok, r.summary()));
        }
        return res;
    }

    /** One-shot utility actions (Tools screen). */
    public ShizukuExec.Result trimCaches() {
        return ShizukuExec.run("pm trim-caches 1024G");
    }

    /** Enters Doze immediately. Works best with screen off; honest about refusal. */
    public ShizukuExec.Result enterDoze() {
        return ShizukuExec.run("dumpsys deviceidle force-idle");
    }

    /** Steps out of forced idle. */
    public ShizukuExec.Result leaveDoze() {
        return ShizukuExec.run("dumpsys deviceidle step");
    }

    /** Restores every changed setting from the snapshot. */
    public SnapshotStore.RestoreReport restoreAll() {
        return snapshot.restoreAll();
    }
}
