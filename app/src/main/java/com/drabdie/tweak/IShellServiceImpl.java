package com.drabdie.tweak;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Runs inside the Shizuku server process (shell user in ADB mode, root in
 * root mode). This is the officially supported UserService approach for
 * privileged shell commands in Shizuku API 13.
 */
public class IShellServiceImpl extends IShellService.Stub {

    private static final int TIMEOUT_MS = 15000;

    @Override
    public String[] exec(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
            final Process proc = p;
            final StringBuilder out = new StringBuilder();
            final StringBuilder err = new StringBuilder();
            Thread outT = new Thread(() -> readStream(proc.getInputStream(), out));
            Thread errT = new Thread(() -> readStream(proc.getErrorStream(), err));
            outT.start();
            errT.start();

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (true) {
                try {
                    int code = proc.exitValue();
                    outT.join(3000);
                    errT.join(3000);
                    return new String[]{String.valueOf(code), out.toString(), err.toString()};
                } catch (IllegalThreadStateException stillRunning) {
                    if (System.currentTimeMillis() > deadline) {
                        proc.destroy();
                        return new String[]{"-1", "", "timed out after " + TIMEOUT_MS + "ms"};
                    }
                    Thread.sleep(50);
                }
            }
        } catch (Throwable t) {
            return new String[]{"-1", "", String.valueOf(t)};
        }
    }

    private static void readStream(InputStream in, StringBuilder sb) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch (Throwable ignored) {
        }
    }
}
