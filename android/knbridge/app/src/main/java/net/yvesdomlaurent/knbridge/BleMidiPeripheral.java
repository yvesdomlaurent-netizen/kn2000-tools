package net.yvesdomlaurent.knbridge;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Périphérique MIDI Bluetooth LE : MobileSheets voit « KN Bridge » comme un appareil
 * Bluetooth (type de connexion qu'il sait utiliser). Les messages MS arrivent ici -> Bus.
 * Le Bus renvoie aussi vers MS (notifications) tout ce que la page envoie.
 */
public class BleMidiPeripheral {
    public static final UUID MIDI_SERVICE_UUID = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");
    public static final UUID MIDI_CHAR_UUID    = UUID.fromString("7772E5DB-3868-4112-A1A9-F2669D106BF9");
    public static final UUID CCC_UUID          = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private final Context ctx;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServer server;
    private BluetoothGattCharacteristic midiChar;
    private final List<BluetoothDevice> centrals = new ArrayList<>();
    private volatile int mtu = 23;
    private boolean running = false;

    // --- Réassemblage des messages MIDI reçus ---
    private final ByteArrayOutputStream midi = new ByteArrayOutputStream();
    private boolean inSysEx = false;
    private int pendingData = 0;

    public BleMidiPeripheral(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    public boolean isRunning() { return running; }

    public synchronized void start() {
        if (running) return;
        if (!hasPermissions()) { Bus.log("✗ Permissions Bluetooth manquantes — rouvre KN Bridge, coche BLE, accepte la demande"); return; }
        BluetoothManager bm = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm == null ? null : bm.getAdapter();
        if (adapter == null || !adapter.isEnabled()) { Bus.log("✗ Bluetooth éteint ou absent"); return; }
        advertiser = adapter.getBluetoothLeAdvertiser();
        if (advertiser == null) { Bus.log("✗ Pas de support de publicité BLE sur cet appareil"); return; }

        server = bm.openGattServer(ctx, callbacks);
        if (server == null) { Bus.log("✗ openGattServer a échoué"); return; }
        BluetoothGattService svc = new BluetoothGattService(MIDI_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        midiChar = new BluetoothGattCharacteristic(MIDI_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor ccc = new BluetoothGattDescriptor(CCC_UUID,
            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        ccc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        midiChar.addDescriptor(ccc);
        midiChar.setValue(new byte[0]);
        svc.addCharacteristic(midiChar);
        server.addService(svc);

        AdvertiseSettings st = new AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true).build();
        AdvertiseData data = new AdvertiseData.Builder()
            .addServiceUuid(new ParcelUuid(MIDI_SERVICE_UUID))
            .setIncludeDeviceName(false).build();
        advertiser.startAdvertising(st, data, advCallback);
        Bus.addBleSink(sink);
        running = true;
        Bus.log("📻 KN Bridge visible en Bluetooth MIDI — apparie-le depuis MobileSheets");
    }

    public synchronized void stop() {
        running = false;
        try { if (advertiser != null) advertiser.stopAdvertising(advCallback); } catch (Exception ignored) {}
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        advertiser = null; server = null; midiChar = null;
        synchronized (centrals) { centrals.clear(); }
        Bus.removeBleSink(sink);
        Bus.log("📻 Bluetooth MIDI arrêté");
    }

    private boolean hasPermissions() {
        if (android.os.Build.VERSION.SDK_INT < 31) return true;
        return ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            && ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private final AdvertiseCallback advCallback = new AdvertiseCallback() {
        @Override public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Bus.log("📻 Publicité BLE démarrée");
        }
        @Override public void onStartFailure(int errorCode) {
            Bus.log("✗ Publicité BLE échec code " + errorCode);
        }
    };

    /** Le Bus envoie ici tout ce qui vient de la page -> retransmis vers MS par notifications. */
    private final Bus.Sink sink = (msg, off, cnt, ts) -> broadcast(msg, off, cnt);

    private void broadcast(byte[] msg, int off, int cnt) {
        BluetoothGattServer g = server;
        BluetoothGattCharacteristic ch = midiChar;
        if (g == null || ch == null) return;
        List<BluetoothDevice> targets;
        synchronized (centrals) { targets = new ArrayList<>(centrals); }
        if (targets.isEmpty()) return;
        int room = Math.max(3, mtu - 5) - 2;
        int i = off, end = off + cnt;
        while (i < end) {
            int n = Math.min(room, end - i);
            long t = (System.nanoTime() / 1000) & 0x3FFF;
            byte[] pkt = new byte[n + 2];
            pkt[0] = (byte) (0x80 | ((t >> 7) & 0x3F));
            pkt[1] = (byte) (0x40 | (t & 0x3F));
            System.arraycopy(msg, i, pkt, 2, n);
            i += n;
            for (BluetoothDevice d : targets) {
                try {
                    ch.setValue(pkt);
                    g.notifyCharacteristicChanged(d, ch, false);
                } catch (Exception e) {
                    Bus.log("⚠ notify : " + e);
                }
            }
        }
    }

    private final BluetoothGattServerCallback callbacks = new BluetoothGattServerCallback() {
        @Override public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Bus.log("📱 Appareil Bluetooth connecté (MS ?)");
                synchronized (centrals) { if (!centrals.contains(device)) centrals.add(device); }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Bus.log("📱 Appareil Bluetooth déconnecté");
                synchronized (centrals) { centrals.remove(device); }
            }
        }
        @Override public void onMtuChanged(BluetoothDevice device, int mtuNew) {
            mtu = Math.max(23, mtuNew);
        }
        @Override public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattCharacteristic characteristic) {
            try { server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                characteristic.getUuid().equals(MIDI_CHAR_UUID) ? new byte[0] : null); }
            catch (Exception ignored) {}
        }
        @Override public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                int offset, byte[] value) {
            if (responseNeeded) server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            if (value != null && value.length > 0) parse(value);
        }
        @Override public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattDescriptor descriptor) {
            try { server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                descriptor.getUuid().equals(CCC_UUID) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : null); }
            catch (Exception ignored) {}
        }
        @Override public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset,
                byte[] value) {
            if (responseNeeded) server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            if (descriptor.getUuid().equals(CCC_UUID)) {
                boolean on = value != null && value.length >= 2 && (value[1] & 0x01) != 0;
                synchronized (centrals) {
                    if (on) { if (!centrals.contains(device)) centrals.add(device); }
                    else centrals.remove(device);
                }
                Bus.log("📱 L'appareil Bluetooth " + (on ? "écoute" : "n'écoute plus") + " les notifications");
            }
        }
    };

    // ---- Analyse des paquets BLE-MIDI (en-têtes/timestamps retirés, messages réassemblés) ----
    private void parse(byte[] d) {
        for (byte b : d) parseByte(b & 0xFF);
    }

    private void parseByte(int b) {
        if ((b & 0xC0) == 0x80) return;   // en-tête de paquet
        if ((b & 0xC0) == 0x40) return;    // timestamp bas
        if (b == 0xF0) { midi.reset(); midi.write(b); inSysEx = true; return; }
        if (inSysEx) { midi.write(b); if (b == 0xF7) { emit(); inSysEx = false; } return; }
        if (b >= 0xF8) { byte[] one = {(byte) b}; Bus.fromBle(one, 0, 1, System.currentTimeMillis()); return; }
        if (b >= 0x80) {
            if (pendingData > 0) emit();
            midi.reset(); midi.write(b);
            pendingData = ((b & 0xF0) == 0xC0 || (b & 0xF0) == 0xD0) ? 1 : 2;
            return;
        }
        if (pendingData > 0) { midi.write(b); if (--pendingData == 0) emit(); }
    }

    private void emit() {
        byte[] m = midi.toByteArray();
        midi.reset();
        pendingData = 0;
        if (m.length > 0) Bus.fromBle(m, 0, m.length, System.currentTimeMillis());
    }
}
