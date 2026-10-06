package com.drabdie.tweak;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores game session records and performance statistics.
 */
public class SessionHistoryStore {

    public static class SessionRecord {
        public final String gameName;
        public final String profileName;
        public final int avgFps;
        public final int maxTempC;
        public final int score;

        public SessionRecord(String gameName, String profileName, int avgFps, int maxTempC, int score) {
            this.gameName = gameName;
            this.profileName = profileName;
            this.avgFps = avgFps;
            this.maxTempC = maxTempC;
            this.score = score;
        }
    }

    private static final String PREF_NAME = "hellboost_sessions";

    public static void recordSession(Context context, String gameName, String profileName, int avgFps, int maxTempC, int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String entry = gameName + "|" + profileName + "|" + avgFps + "|" + maxTempC + "|" + score;
        String existing = prefs.getString("records", "");
        prefs.edit().putString("records", entry + ";" + existing).apply();
    }

    public static List<SessionRecord> getHistory(Context context) {
        List<SessionRecord> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString("records", "");
        if (raw == null || raw.isEmpty()) return list;

        String[] parts = raw.split(";");
        for (String p : parts) {
            if (p.trim().isEmpty()) continue;
            String[] fields = p.split("\\|");
            if (fields.length >= 5) {
                try {
                    list.add(new SessionRecord(
                            fields[0],
                            fields[1],
                            Integer.parseInt(fields[2]),
                            Integer.parseInt(fields[3]),
                            Integer.parseInt(fields[4])
                    ));
                } catch (Throwable ignored) {}
            }
        }
        return list;
    }
}
