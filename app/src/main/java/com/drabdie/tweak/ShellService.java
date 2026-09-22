package com.drabdie.tweak;

import android.os.RemoteException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Runs inside the Shizuku server process (ADB shell uid 2000, or root when Shizuku runs as root).
 * This is the real privileged bridge: every command the app applies goes through here.
 */
public class ShellService extends IShellService.Stub {
    public ShellService() {}

    /** Called by Shizuku when the user service is removed. */
    public void destroy() {}

    @Override
    public String[] exec(String cmd) throws RemoteException {
        java.lang.Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd + " 2>&1"});
            String out = read(p.getInputStream());
            int code;
            try { code = p.waitFor(); } catch (InterruptedException e) { code = -1; }
            return new String[]{String.valueOf(code), out};
        } catch (Throwable t) {
            String msg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            return new String[]{"-1", msg};
        } finally {
            if (p != null) p.destroy();
        }
    }

    private static String read(InputStream in) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            String s = b.toString().trim();
            return s.length() > 160 ? s.substring(0, 160) + "…" : s;
        } catch (Exception e) {
            return "read-error: " + e.getMessage();
        }
    }
}
