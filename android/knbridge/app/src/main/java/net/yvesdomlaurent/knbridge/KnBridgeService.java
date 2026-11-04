package net.yvesdomlaurent.knbridge;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiReceiver;
import android.util.Log;
import java.io.IOException;

/**
 * KN Bridge - pont MIDI virtuel pour MobileSheets -> page KN2000.
 *
 * Expose un port d'entree "Commande MS" (ou MobileSheets envoie ses commandes)
 * et un port de sortie "Vers Page" (que Chrome / la page kn2000-apc-live-v2 ecoute).
 * Tout message recu sur l'entree est transmis tel quel sur la sortie,
 * octets bruts 0-255 inclus (sysex F0 7D 00 <msid> F7).
 */
public class KnBridgeService extends MidiDeviceService {
    private static final String TAG = "KNBridge";

    private final MidiReceiver mInputPort = new MidiReceiver() {
        @Override
        public void onSend(byte[] msg, int offset, int count, long timestamp)
                throws IOException {
            MidiReceiver[] outs = getOutputPortReceivers();
            if (outs == null) {
                Log.w(TAG, "output port receiver indisponible");
                return;
            }
            for (MidiReceiver out : outs) {
                if (out != null) {
                    try {
                        out.send(msg, offset, count, timestamp);
                    } catch (IOException e) {
                        Log.e(TAG, "erreur transfert vers port sortie", e);
                    }
                }
            }
        }
    };

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { mInputPort };
    }
}
