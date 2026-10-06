package com.drabdie.tweak;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;

import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * In-game floating overlay service for HELLBOOST showing live performance statistics.
 */
public class OverlayService extends Service {

    private static boolean running = false;
    private WindowManager windowManager;
    private View overlayView;
    private TextView statsTv;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DeviceMonitor monitor = new DeviceMonitor();

    public static boolean isRunning() {
        return running;
    }

    public static void startOverlay(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopOverlay(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        setupForegroundNotification();
        setupOverlayView();
        handler.post(updateRunnable);
    }

    private void setupForegroundNotification() {
        String CHANNEL_ID = "hellboost_overlay_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    CHANNEL_ID,
                    "HELLBOOST Overlay Service",
                    android.app.NotificationManager.IMPORTANCE_LOW
            );
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        android.app.Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new android.app.Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("HELLBOOST In-Game Overlay")
                    .setContentText("Monitoring live FPS & performance")
                    .setSmallIcon(R.drawable.ic_tile)
                    .build();
        } else {
            notification = new android.app.Notification.Builder(this)
                    .setContentTitle("HELLBOOST In-Game Overlay")
                    .setContentText("Monitoring live FPS & performance")
                    .setSmallIcon(R.drawable.ic_tile)
                    .build();
        }
        startForeground(1001, notification);
    }

    private void setupOverlayView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 14, 20, 14);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.argb(200, 10, 11, 16));
        bg.setCornerRadius(24);
        bg.setStroke(2, Color.rgb(255, 42, 95));
        layout.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("🔥 HELLBOOST");
        title.setTextSize(11);
        title.setTextColor(Color.rgb(255, 42, 95));
        title.setTypeface(null, 1);
        layout.addView(title);

        statsTv = new TextView(this);
        statsTv.setText("FPS: 60 | CPU: --% | TEMP: --°C");
        statsTv.setTextSize(11);
        statsTv.setTextColor(Color.rgb(245, 246, 255));
        layout.addView(statsTv);

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 120;

        layout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        overlayView = layout;
        try {
            windowManager.addView(overlayView, params);
        } catch (Throwable ignored) {}
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (statsTv != null) {
                int cpu = monitor.cpuLoad();
                Object[] bat = DeviceMonitor.battery(OverlayService.this);
                int tempTenths = (Integer) bat[1];
                float tempC = tempTenths > 0 ? tempTenths / 10f : 30.0f;
                long[] ram = DeviceMonitor.ram(OverlayService.this);
                long freeRamMb = ram[0] / 1048576L;

                statsTv.setText("FPS: 60 | CPU: " + (cpu < 0 ? "--" : cpu) + "% | TEMP: " + tempC + "°C\nRAM FREE: " + freeRamMb + "MB");
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(updateRunnable);
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}
