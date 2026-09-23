package com.drabdie.tweak;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only device diagnostics: CPU load from /proc/stat, per-core
 * frequencies from sysfs (readable without root), RAM, battery and storage.
 * Nothing here writes anything.
 */
public final class DeviceMonitor {

    private long lastIdle = -1, lastTotal = -1;
    private int cachedCpuCount = -1;

    /** CPU busy percent over the interval since the previous call (1s tick). */
    public int cpuLoad() {
        long[] s = readProcStat();
        if (s == null) return -1;
        long idle = s[0], total = s[1];
        if (lastTotal < 0) { lastIdle = idle; lastTotal = total; return -1; }
        long dIdle = idle - lastIdle, dTotal = total - lastTotal;
        lastIdle = idle; lastTotal = total;
        if (dTotal <= 0) return -1;
        return (int) Math.max(0, Math.min(100, 100 - (dIdle * 100 / dTotal)));
    }

    private static long[] readProcStat() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = r.readLine(); // "cpu  user nice system idle iowait irq softirq steal ..."
            if (line == null || !line.startsWith("cpu ")) return null;
            String[] parts = line.split("\\s+");
            long idle = 0, total = 0;
            for (int i = 1; i < parts.length; i++) {
                long v = Long.parseLong(parts[i]);
                total += v;
                if (i == 4 || i == 5) idle += v; // idle + iowait
            }
            return new long[]{idle, total};
        } catch (Throwable t) {
            return null;
        }
    }

    public int cpuCount() {
        if (cachedCpuCount > 0) return cachedCpuCount;
        try (BufferedReader r = new BufferedReader(new FileReader("/sys/devices/system/cpu/present"))) {
            String present = r.readLine(); // e.g. "0-7"
            int idx = present.indexOf('-');
            cachedCpuCount = idx < 0 ? 1 : Integer.parseInt(present.substring(idx + 1)) + 1;
        } catch (Throwable t) {
            try (BufferedReader r = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                int n = 0;
                while (r.readLine() != null) if (r.ready()) n++;
                cachedCpuCount = Math.max(1, Runtime.getRuntime().availableProcessors());
            } catch (Throwable t2) {
                cachedCpuCount = Math.max(1, Runtime.getRuntime().availableProcessors());
            }
        }
        return cachedCpuCount;
    }

    /** Current frequency (kHz) of one core, or -1 when not exposed by the OEM. */
    public long coreFreq(int core) {
        try (BufferedReader r = new BufferedReader(new FileReader(
                "/sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_cur_freq"))) {
            return Long.parseLong(r.readLine().trim());
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Average frequency in MHz across cores that expose it, or -1. */
    public int avgFreqMhz() {
        long sum = 0; int n = 0;
        for (int i = 0; i < cpuCount(); i++) {
            long f = coreFreq(i);
            if (f > 0) { sum += f; n++; }
        }
        return n == 0 ? -1 : (int) (sum / n / 1000);
    }

    /** Max frequency in MHz across cores that expose it, or -1. */
    public int maxFreqMhz() {
        long max = -1;
        for (int i = 0; i < cpuCount(); i++) {
            long f = coreFreq(i);
            if (f > max) max = f;
        }
        return max < 0 ? -1 : (int) (max / 1000);
    }

    public static long[] ram(Context ctx) {
        ActivityManager.MemoryInfo m = new ActivityManager.MemoryInfo();
        ((ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);
        return new long[]{m.availMem, m.totalMem};
    }

    /** {level, tempTenthsC, currentMicroAmps}; temp/current may be unknown. */
    public static Object[] battery(Context ctx) {
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = -1, temp = -1;
        if (i != null) {
            int lv = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (lv >= 0 && sc > 0) level = lv * 100 / sc;
            temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        }
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        long current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        return new Object[]{level, temp, current};
    }

    /** {freeBytes, totalBytes} of the data partition. */
    public static long[] storage() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            return new long[]{fs.getAvailableBytes(), fs.getTotalBytes()};
        } catch (Throwable t) {
            return new long[]{0, 0};
        }
    }
}
