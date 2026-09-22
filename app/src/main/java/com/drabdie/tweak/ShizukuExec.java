package com.drabdie.tweak;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import rikka.shizuku.Shizuku;

/**
 * Executes privileged shell commands through the Shizuku UserService
 * (IShellService.aidl runs inside the Shizuku server process).
 *
 * Shizuku in ADB mode runs as the shell user (UID 2000): that is enough for
 * settings / pm / am / cmd appops, but NOT for kernel or /sys writes.
 * Every command result is reported honestly - no fake success.
 */
public final class ShizukuExec {

    /** Result of one command execution. ok == (exit code == 0). */
    public static final class Result {
        public final boolean ok;
        public final int exit;
        public final String out;
        public final String err;

        Result(boolean ok, int exit, String out, String err) {
            this.ok = ok;
            this.exit = exit;
            this.out = out == null ? "" : out.trim();
            this.err = err == null ? "" : err.trim();
        }

        /** One-line summary for toasts. */
        public String summary() {
            if (ok) return "OK" + (out.isEmpty() ? "" : " · " + first(out));
            return "FAILED" + (err.isEmpty() ? " (exit " + exit + ")" : " · " + first(err));
        }

        private static String first(String s) {
            int i = s.indexOf('\n');
            String line = i < 0 ? s : s.substring(0, i);
            return line.length() > 90 ? line.substring(0, 90) + "…" : line;
        }
    }

    private static volatile IShellService service;
    private static ServiceConnection connection;
    private static final Object CONNECT_LOCK = new Object();
    private static final Result UNAVAILABLE = new Result(false, -1, "", "Shizuku is not available");

    private ShizukuExec() {}

    /** True when Shizuku is running AND this app holds its permission. */
    public static boolean available() {
        try {
            return Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Requests the Shizuku permission (no-op when not installed). */
    public static void requestPermission() {
        try {
            if (Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(100);
            }
        } catch (Throwable ignored) {}
    }

    /** Rejects anything that is not a safe package/setting-style token. */
    public static String safe(String arg) {
        if (arg == null || !arg.matches("[A-Za-z0-9._+@-]+")) {
            throw new IllegalArgumentException("unsafe argument rejected: " + arg);
        }
        return arg;
    }

    /** Runs one shell command through Shizuku (binds the user service first). */
    public static Result run(String cmd) {
        IShellService s = obtain();
        if (s == null) return UNAVAILABLE;
        try {
            String[] r = s.exec(cmd);
            int code = Integer.parseInt(r[0]);
            return new Result(code == 0, code, r.length > 1 ? r[1] : "", r.length > 2 ? r[2] : "");
        } catch (Throwable t) {
            return new Result(false, -1, "", String.valueOf(t));
        }
    }

    /** Binds (once) and returns the user service, waiting up to 8s. */
    private static IShellService obtain() {
        if (service != null) return service;
        if (!available()) return null;
        synchronized (CONNECT_LOCK) {
            if (service != null) return service;
            try {
                Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                        new ComponentName("com.drabdie.tweak", IShellServiceImpl.class.getName()))
                        .daemon(false)
                        .processNameSuffix("tools")
                        .debuggable(false)
                        .version(2);
                final Object done = new Object();
                connection = new ServiceConnection() {
                    @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                        service = IShellService.Stub.asInterface(binder);
                        synchronized (done) { done.notifyAll(); }
                    }

                    @Override public void onServiceDisconnected(ComponentName name) {
                        service = null;
                    }

                    @Override public void onBindingDied(ComponentName name) {
                        service = null;
                    }

                    @Override public void onNullBinding(ComponentName name) {
                        synchronized (done) { done.notifyAll(); }
                    }
                };
                Shizuku.bindUserService(args, connection);
                try {
                    synchronized (done) { done.wait(8000); }
                } catch (InterruptedException ignored) {}
            } catch (Throwable ignored) {}
            return service;
        }
    }

    /** Unbinds the user service (call from the main activity's onDestroy). */
    public static void shutdown(Context ctx) {
        try {
            if (connection != null) {
                Shizuku.unbindUserService(
                        new Shizuku.UserServiceArgs(new ComponentName(ctx.getPackageName(),
                                IShellServiceImpl.class.getName()))
                                .daemon(false).processNameSuffix("tools").debuggable(false).version(2),
                        connection, true);
            }
        } catch (Throwable ignored) {}
        service = null;
        connection = null;
    }
}
