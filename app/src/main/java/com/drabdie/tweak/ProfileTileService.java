package com.drabdie.tweak;

import android.content.Context;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick Settings tile: toggles between Performance and Battery profiles.
 * Uses the same ProfileEngine as the app - identical honest behaviour:
 * when Shizuku is unavailable the tile only changes its label, nothing else.
 */
public class ProfileTileService extends TileService {

    private boolean batteryMode = false;

    @Override
    public void onStartListening() {
        updateTile();
    }

    @Override
    public void onClick() {
        batteryMode = !batteryMode;
        Context ctx = getApplicationContext();
        ProfileEngine engine = new ProfileEngine(ctx);
        if (ShizukuExec.available()) {
            engine.apply(batteryMode ? 2 : 0);
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        String profile = batteryMode ? "Battery" : "Performance";
        tile.setLabel("DraB · " + profile);
        tile.setSubtitle(ShizukuExec.available() ? profile + " active" : "Shizuku off");
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
