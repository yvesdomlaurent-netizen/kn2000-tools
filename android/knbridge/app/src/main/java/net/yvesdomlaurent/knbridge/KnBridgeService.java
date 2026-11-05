package net.yvesdomlaurent.knbridge;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiReceiver;
import android.util.Log;
import java.io.IOException;

/**
 * KN Bridge — périphérique MIDI virtuel.
 * Port d'entrée « Commande MS » (la page y écrit) / port de sortie « Vers Page » (la page y lit).
 * Tout ce qui entre est routé par la classe Bus (USB, Bluetooth).
 */
public class KnBridgeService extends MidiDeviceService {
    private static final String TAG = "KNBridge";

    private final MidiReceiver mInputPort = new MidiReceiver() {
        @Override
        public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
            Bus.fromVirtual(msg, offset, count, timestamp);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Bus.setVirtualSink((msg, off, cnt, ts) -> {
            try {
                MidiReceiver[] outs = getOutputPortReceivers();
                if (outs != null && outs.length > 0) outs[0].send(msg, off, cnt, ts);
            } catch (IOException e) {
                Log.e(TAG, "envoi vers port virtuel", e);
            }
        });
        Bus.log("Service virtuel KN Bridge prêt (Commande MS → Vers Page)");
    }

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { mInputPort };
    }
}
