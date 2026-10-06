package com.drabdie.tweak;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto-detects installed games and manages custom added/removed game entries.
 */
public final class GameLibrary {

    public static class GameInfo {
        public final String packageName;
        public final String label;
        public final boolean isAutoDetected;

        public GameInfo(String packageName, String label, boolean isAutoDetected) {
            this.packageName = packageName;
            this.label = label;
            this.isAutoDetected = isAutoDetected;
        }
    }

    private static final Set<String> CUSTOM_GAMES = new HashSet<>();
    private static final Set<String> IGNORED_PACKAGES = new HashSet<>();

    public static List<GameInfo> getGames(Context context) {
        PackageManager pm = context.getPackageManager();
        List<GameInfo> games = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Detect by Android Game Category (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo ai : apps) {
                    if (IGNORED_PACKAGES.contains(ai.packageName)) continue;
                    if (ai.category == ApplicationInfo.CATEGORY_GAME) {
                        CharSequence l = pm.getApplicationLabel(ai);
                        String name = l != null ? l.toString() : ai.packageName;
                        games.add(new GameInfo(ai.packageName, name, true));
                        seen.add(ai.packageName);
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Query Launcher category GAME intent filter
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> gameIntents = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo ri : gameIntents) {
                String pkg = ri.activityInfo.packageName;
                if (!seen.contains(pkg) && !IGNORED_PACKAGES.contains(pkg)) {
                    String lowerPkg = pkg.toLowerCase();
                    if (lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("genshin") ||
                        lowerPkg.contains("roblox") || lowerPkg.contains("freefire") || lowerPkg.contains("cod") ||
                        lowerPkg.contains("mobilelegend") || lowerPkg.contains("asphalt") || lowerPkg.contains("minecraft") ||
                        lowerPkg.contains("unity") || lowerPkg.contains("unreal") || lowerPkg.contains("mihoyo") ||
                        lowerPkg.contains("supercell") || lowerPkg.contains("tencent") || lowerPkg.contains("riot")) {
                        CharSequence l = ri.loadLabel(pm);
                        games.add(new GameInfo(pkg, l != null ? l.toString() : pkg, true));
                        seen.add(pkg);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. User custom games
        for (String pkg : CUSTOM_GAMES) {
            if (!seen.contains(pkg)) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    CharSequence l = pm.getApplicationLabel(ai);
                    games.add(new GameInfo(pkg, l != null ? l.toString() : pkg, false));
                    seen.add(pkg);
                } catch (Throwable ignored) {}
            }
        }

        Collections.sort(games, Comparator.comparing(g -> g.label.toLowerCase()));
        return games;
    }

    public static void addCustomGame(String pkg) {
        CUSTOM_GAMES.add(pkg);
        IGNORED_PACKAGES.remove(pkg);
    }

    public static void removeGame(String pkg) {
        CUSTOM_GAMES.remove(pkg);
        IGNORED_PACKAGES.add(pkg);
    }
}
