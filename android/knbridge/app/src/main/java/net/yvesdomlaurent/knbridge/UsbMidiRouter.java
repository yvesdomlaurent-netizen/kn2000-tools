package net.yvesdomlaurent.knbridge;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ouvre les périphériques USB choisis (boîtier CME…) et route vers/depuis le port virtuel. */
public class UsbMidiRouter {
    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MidiManager mm;
    private final List<MidiDevice> openDevices = new ArrayList<>();
    private final List<MidiInputPort> inputPorts = new ArrayList<>();
    private final Set<Integer> connected = new HashSet<>();
    private boolean running = false;

    public UsbMidiRouter(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    public synchronized void start() {
        if (running) return;
        running = true;
        mm = (MidiManager) ctx.getSystemService(Context.MIDI_SERVICE);
        if (mm == null) { Bus.log("✗ Pas de service MIDI Android"); return; }
        mm.registerDeviceCallback(new MidiManager.DeviceCallback() {
            @Override public void onDeviceAdded(MidiDeviceInfo info) { connect(info); }
            @Override public void onDeviceRemoved(MidiDeviceInfo info) {
                Bus.log("− USB retiré (id " + info.getId() + ")");
                connected.remove(info.getId());
            }
        }, handler);
        scan();
        Bus.log("Routeur USB démarré");
    }

    public synchronized void stop() {
        running = false;
        for (MidiInputPort p : inputPorts) { try { p.close(); } catch (IOException ignored) {} }
        inputPorts.clear();
        Bus.clearUsbSinks();
        for (MidiDevice d : openDevices) { try { d.close(); } catch (IOException ignored) {} }
        openDevices.clear();
        connected.clear();
        Bus.log("Routeur USB arrêté");
    }

    public synchronized void rescan() { if (mm != null) scan(); }

    private void scan() {
        for (MidiDeviceInfo info : mm.getDevices()) connect(info);
    }

    /** Liste les numéros de ports du type demandé (TYPE_INPUT ou TYPE_OUTPUT). */
    private static List<Integer> portNumbers(MidiDeviceInfo info, int type) {
        List<Integer> out = new ArrayList<>();
        for (MidiDeviceInfo.PortInfo p : info.getPorts()) {
            if (p.getType() == type) out.add(p.getPortNumber());
        }
        return out;
    }

    private void connect(MidiDeviceInfo info) {
        if (!running || info.getType() != MidiDeviceInfo.TYPE_USB) return;
        synchronized (connected) { if (!connected.add(info.getId())) return; }
        final String name = info.getProperties() != null
            ? info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME, "USB " + info.getId())
            : "USB " + info.getId();
        final boolean tx = Bus.deviceEnabled(ctx, name, true);   // page -> appareil
        final boolean rx = Bus.deviceEnabled(ctx, name, false);  // appareil -> page
        if (!tx && !rx) return;
        mm.openDevice(info, device -> {
            if (device == null) { Bus.log("✗ Ouverture impossible : " + name); return; }
            synchronized (openDevices) { openDevices.add(device); }
            Bus.log("✚ USB « " + name + " » connecté" + (tx ? " [TX]" : "") + (rx ? " [RX]" : ""));
            if (tx) {
                for (int portNum : portNumbers(info, MidiDeviceInfo.PortInfo.TYPE_INPUT)) {
                    final MidiInputPort in = device.openInputPort(portNum);
                    if (in == null) continue;
                    synchronized (inputPorts) { inputPorts.add(in); }
                    Bus.addUsbSink((msg, off, cnt, ts) -> {
                        try { in.send(msg, off, cnt, ts); }
                        catch (IOException e) { Bus.log("⚠ écriture USB : " + e); }
                    });
                }
            }
            if (rx) {
                for (int portNum : portNumbers(info, MidiDeviceInfo.PortInfo.TYPE_OUTPUT)) {
                    MidiOutputPort out = device.openOutputPort(portNum);
                    if (out == null) continue;
                    out.connect(new MidiReceiver() {
                        @Override public void onSend(byte[] msg, int off, int cnt, long ts) {
                            Bus.fromUsb(name, msg, off, cnt, ts);
                        }
                    });
                }
            }
        }, handler);
    }
}
