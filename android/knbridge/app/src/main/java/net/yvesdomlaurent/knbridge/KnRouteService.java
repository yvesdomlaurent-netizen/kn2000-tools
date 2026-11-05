package net.yvesdomlaurent.knbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** Service en avant-plan : garde le routeur USB et le Bluetooth actifs en permanence. */
public class KnRouteService extends Service {
    private static final int NOTIF_ID = 2;
    private UsbMidiRouter router;
    private BleMidiPeripheral ble;

    /** v1.5 — statut visible depuis le panneau. */
    private static volatile boolean running = false;
    public static boolean isRunning() { return running; }

    public static final String ACTION_TOGGLE_BLE = "net.yvesdomlaurent.knbridge.TOGGLE_BLE";

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        startForegroundWithNotif();
        Bus.setShowFe(Bus.showFePref(this));
        router = new UsbMidiRouter(this);
        router.start();
        if (Bus.bleEnabled(this)) startBle();
        Bus.log("Service KN Bridge démarré");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_BLE.equals(intent.getAction())) {
            if (ble != null && ble.isRunning()) { Bus.setBleEnabled(this, false); stopBle(); }
            else { Bus.setBleEnabled(this, true); startBle(); }
        }
        return START_STICKY;
    }

    public void startBle() {
        if (ble == null) ble = new BleMidiPeripheral(this);
        ble.start();
    }

    public void stopBle() {
        if (ble != null) ble.stop();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (router != null) router.stop();
        if (ble != null) ble.stop();
        Bus.log("Service KN Bridge arrêté");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startForegroundWithNotif() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("knbridge", "KN Bridge", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Routage MIDI KN Bridge");
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(this, "knbridge");
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b
            .setContentTitle("KN Bridge actif")
            .setContentText("Routage MIDI USB ↔ virtuel ↔ Bluetooth")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build();
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } catch (Exception e) {
                startForeground(NOTIF_ID, n);
            }
        } else {
            startForeground(NOTIF_ID, n);
        }
    }
}
