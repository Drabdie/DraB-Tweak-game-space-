package com.drabdie.tweak;

import java.util.ArrayList;
import java.util.List;

/**
 * Curated debloat recommendations (subset of the community Universal Debloat
 * List ideas, adapted to what the shell user can safely remove with
 * "pm uninstall -k --user 0"). Removal always keeps APK and data on disk,
 * so "cmd package install-existing <pkg>" restores everything.
 *
 * risk: 0 = safe (replacement exists / purely redundant),
 *       1 = optional (useful to some users),
 *       2 = advanced (may break OEM features - read the note).
 */
public final class DebloatList {

    public static final class Entry {
        public final String pkg, note;
        public final int risk;

        Entry(String pkg, int risk, String note) {
            this.pkg = pkg;
            this.risk = risk;
            this.note = note;
        }
    }

    public static final int SAFE = 0, OPTIONAL = 1, ADVANCED = 2;

    private static final Entry[] ENTRIES = {
            // Google - safe
            new Entry("com.google.android.apps.tachyon", SAFE, "Google Meet/Duo (replaced by Meet in most builds)"),
            new Entry("com.google.android.videos", SAFE, "Google TV / Google Videos"),
            new Entry("com.google.android.music", SAFE, "Google Play Music (legacy)"),
            new Entry("com.google.android.apps.googleassistant", OPTIONAL, "Google Assistant app"),
            new Entry("com.google.android.apps.bard", OPTIONAL, "Gemini app"),
            new Entry("com.google.android.googlequicksearchbox", OPTIONAL, "Google App / Search + feed"),
            new Entry("com.google.android.gm", OPTIONAL, "Gmail"),
            new Entry("com.google.android.apps.maps", OPTIONAL, "Google Maps"),
            new Entry("com.google.android.youtube", OPTIONAL, "YouTube"),
            new Entry("com.google.android.apps.docs", OPTIONAL, "Google Docs/Drive"),
            new Entry("com.google.android.apps.photos", OPTIONAL, "Google Photos"),
            new Entry("com.android.chrome", OPTIONAL, "Chrome (another browser required)"),
            new Entry("com.google.android.feedback", SAFE, "Google feedback reporter"),
            new Entry("com.google.android.printservice.recommendation", SAFE, "Print service recommender"),
            new Entry("com.google.android.apps.restore", SAFE, "Android Switch / restore helper"),
            new Entry("com.google.android.setupwizard", ADVANCED, "Setup wizard - do NOT remove before first setup"),
            // AOSP - safe
            new Entry("com.android.wallpaperbackup", SAFE, "Wallpaper backup"),
            new Entry("com.android.wallpapercropper", SAFE, "Wallpaper cropper"),
            new Entry("com.android.egg", SAFE, "Android easter egg"),
            new Entry("com.android.protectedapp", OPTIONAL, "Protected app notice on some OEMs"),
            new Entry("com.android.simappdialog", OPTIONAL, "SIM app dialog"),
            new Entry("com.android.stk", OPTIONAL, "SIM Toolkit"),
            new Entry("com.android.bookmarkprovider", SAFE, "Legacy bookmark provider"),
            new Entry("com.android.dreams.basic", OPTIONAL, "Daydream basic screensavers"),
            new Entry("com.android.dreams.phototable", OPTIONAL, "Photos screensaver"),
            new Entry("com.android.noisefield", OPTIONAL, "Noisefield wallpaper"),
            new Entry("com.android.phasebeam", OPTIONAL, "Phasebeam wallpaper"),
            new Entry("com.android.bips", OPTIONAL, "Default print service"),
            // Verizon / carrier
            new Entry("com.verizon.mips.services", SAFE, "Verizon services"),
            new Entry("com.verizon.llkagent", SAFE, "Verizon LLK agent"),
            new Entry("com.vzw.hss.widgets", SAFE, "Verizon widgets"),
            new Entry("com.verizon.obda_permissions", SAFE, "Verizon ODMA"),
            new Entry("com.impulselabs.guidebuttonapp", SAFE, "Verizon guide button"),
            // Microsoft preinstalls
            new Entry("com.microsoft.skydrive", OPTIONAL, "OneDrive preinstall"),
            new Entry("com.microsoft.office.officehubrow", OPTIONAL, "Office preinstall"),
            new Entry("com.linkedin.android", OPTIONAL, "LinkedIn preinstall"),
            // Facebook preinstalls
            new Entry("com.facebook.katana", OPTIONAL, "Facebook app"),
            new Entry("com.facebook.appmanager", SAFE, "Facebook app manager (background updater)"),
            new Entry("com.facebook.services", SAFE, "Facebook services (background)"),
            new Entry("com.facebook.system", SAFE, "Facebook app installer"),
            // Netflix preinstalls
            new Entry("com.netflix.mediaclient", OPTIONAL, "Netflix app"),
            new Entry("com.netflix.partner.activation", SAFE, "Netflix activation"),
            // Misc
            new Entry("com.qualcomm.qti.remotefpsassist", SAFE, "Qualcomm FPS assist"),
            new Entry("com.qualcomm.qti.workloadservice", ADVANCED, "Qualcomm workload service - may affect scheduling"),
            new Entry("com.quicinc.voipsettings", SAFE, "Qualcomm VoIP settings"),
            new Entry("com.scee.psnandroidapp", OPTIONAL, "PlayStation App preinstall"),
            new Entry("com.ebay.mobile", OPTIONAL, "eBay preinstall"),
            new Entry("com.amazon.mShop.android.shopping", OPTIONAL, "Amazon Shopping preinstall"),
            new Entry("com.booking", OPTIONAL, "Booking.com preinstall"),
            new Entry("com.tripadvisor.tripadvisor", OPTIONAL, "TripAdvisor preinstall"),
            new Entry("com.adobe.reader", OPTIONAL, "Adobe Reader preinstall"),
            new Entry("com.gameword.gamehub", OPTIONAL, "OEM game hub"),
    };

    public static List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES) out.add(e);
        return out;
    }

    public static List<Entry> byRisk(int risk) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES) if (e.risk == risk) out.add(e);
        return out;
    }

    private DebloatList() {}
}
