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

/** Ouvre les PORTS USB choisis (boîtier CME (0)/(1)…) et route vers/depuis le port virtuel.
 *  L'exclusivité Android est PAR PORT : MS peut écrire sur le (1) pendant qu'on écrit le (0). */
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
    public static List<Integer> portNumbers(MidiDeviceInfo info, int type) {
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
        final List<Integer> inPorts = portNumbers(info, MidiDeviceInfo.PortInfo.TYPE_INPUT);
        final List<Integer> outPorts = portNumbers(info, MidiDeviceInfo.PortInfo.TYPE_OUTPUT);
        boolean anyTx = false, anyRx = false;
        for (int p : inPorts) anyTx = anyTx || Bus.portEnabled(ctx, name, p, true);
        for (int p : outPorts) anyRx = anyRx || Bus.portEnabled(ctx, name, p, false);
        if (!anyTx && !anyRx) {
            Bus.log("USB « " + name + " » : aucun port actif — coche TX/RX par port dans le panneau");
            return;
        }
        mm.openDevice(info, device -> {
            if (device == null) { Bus.log("✗ Ouverture impossible : " + name); return; }
            synchronized (openDevices) { openDevices.add(device); }
            for (int p : inPorts) {
                if (!Bus.portEnabled(ctx, name, p, true)) continue;
                final MidiInputPort in = device.openInputPort(p);
                if (in == null) { Bus.log("✗ Port " + p + " indisponible (pris par une autre appli ?) : " + name); continue; }
                synchronized (inputPorts) { inputPorts.add(in); }
                Bus.log("✚ USB « " + name + " » port (" + p + ") connecté [TX]");
                Bus.addUsbSink((msg, off, cnt, ts) -> {
                    try { in.send(msg, off, cnt, ts); }
                    catch (IOException e) { Bus.log("⚠ écriture USB (" + p + ") : " + e); }
                });
            }
            for (int p : outPorts) {
                if (!Bus.portEnabled(ctx, name, p, false)) continue;
                MidiOutputPort out = device.openOutputPort(p);
                if (out == null) { Bus.log("✗ Port " + p + " indisponible : " + name); continue; }
                out.connect(new MidiReceiver() {
                    @Override public void onSend(byte[] msg, int off, int cnt, long ts) {
                        Bus.fromUsb(name + " (" + p + ")", msg, off, cnt, ts);
                    }
                });
                Bus.log("✚ USB « " + name + " » port (" + p + ") connecté [RX]");
            }
        }, handler);
    }
}
