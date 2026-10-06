package com.drabdie.tweak;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;

/**
 * Advanced Hardware & Performance Monitor for HELLBOOST.
 */
public class DeviceMonitor {

    private long prevIdle = 0, prevTotal = 0;

    /** Returns CPU load percentage (0..100) or -1 if unavailable. */
    public int cpuLoad() {
        try (RandomAccessFile file = new RandomAccessFile("/proc/stat", "r")) {
            String line = file.readLine();
            if (line == null || !line.startsWith("cpu ")) return -1;
            String[] toks = line.split("\\s+");
            long user = Long.parseLong(toks[1]);
            long nice = Long.parseLong(toks[2]);
            long sys  = Long.parseLong(toks[3]);
            long idle = Long.parseLong(toks[4]);
            long iowait = toks.length > 5 ? Long.parseLong(toks[5]) : 0;
            long irq    = toks.length > 6 ? Long.parseLong(toks[6]) : 0;
            long softirq= toks.length > 7 ? Long.parseLong(toks[7]) : 0;

            long total = user + nice + sys + idle + iowait + irq + softirq;
            long totalIdle = idle + iowait;

            long diffTotal = total - prevTotal;
            long diffIdle  = totalIdle - prevIdle;

            prevTotal = total;
            prevIdle  = totalIdle;

            if (diffTotal <= 0) return 0;
            int pct = (int) (100 * (diffTotal - diffIdle) / diffTotal);
            return Math.max(0, Math.min(100, pct));
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Returns [availBytes, totalBytes]. */
    public static long[] ram(Context context) {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                return new long[]{mi.availMem, mi.totalMem};
            }
        } catch (Throwable ignored) {}
        return new long[]{0, 1};
    }

    /**
     * Battery status: [levelPercent, tempTenthsC, currentMicroAmps].
     */
    public static Object[] battery(Context context) {
        IntentFilter ifilter = new IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent b = context.registerReceiver(null, ifilter);
        int level = -1, temp = -1;
        if (b != null) {
            int raw = b.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            if (raw >= 0 && scale > 0) level = (raw * 100) / scale;
            temp = b.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1);
        }
        long microA = Long.MIN_VALUE;
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                microA = bm.getLongProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            }
        } catch (Throwable ignored) {}
        return new Object[]{level, temp, microA};
    }

    /** Returns [availBytes, totalBytes] for internal storage. */
    public static long[] storage() {
        try {
            File path = android.os.Environment.getDataDirectory();
            android.os.StatFs stat = new android.os.StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availBlocks = stat.getAvailableBlocksLong();
            return new long[]{availBlocks * blockSize, totalBlocks * blockSize};
        } catch (Throwable t) {
            return new long[]{0, 1};
        }
    }

    public int cpuCount() {
        int n = Runtime.getRuntime().availableProcessors();
        return n > 0 ? n : 1;
    }

    public long coreFreq(int core) {
        try (RandomAccessFile f = new RandomAccessFile("/sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_cur_freq", "r")) {
            String l = f.readLine();
            return l != null ? Long.parseLong(l.trim()) : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    public int avgFreqMhz() {
        int count = cpuCount();
        long sum = 0;
        int n = 0;
        for (int i = 0; i < count; i++) {
            long f = coreFreq(i);
            if (f > 0) {
                sum += f;
                n++;
            }
        }
        return n > 0 ? (int) (sum / n / 1000) : -1;
    }

    public int maxFreqMhz() {
        int count = cpuCount();
        long max = -1;
        for (int i = 0; i < count; i++) {
            long f = coreFreq(i);
            if (f > max) max = f;
        }
        return max > 0 ? (int) (max / 1000) : -1;
    }

    /** Network latency check (Ping in ms). */
    public static int pingMs() {
        try {
            long start = System.currentTimeMillis();
            InetAddress address = InetAddress.getByName("8.8.8.8");
            if (address.isReachable(800)) {
                return (int) (System.currentTimeMillis() - start);
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    /** Get display refresh rate (Hz). */
    public static int displayRefreshRate(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return (int) activity.getDisplay().getRefreshRate();
            } else {
                return (int) activity.getWindowManager().getDefaultDisplay().getRefreshRate();
            }
        } catch (Throwable t) {
            return 60;
        }
    }
}
