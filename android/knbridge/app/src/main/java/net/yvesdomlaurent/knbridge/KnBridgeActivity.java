package net.yvesdomlaurent.knbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Panneau de contrôle : routage PAR PORT, Bluetooth, moniteur MIDI. */
public class KnBridgeActivity extends Activity {
    private LinearLayout deviceList;
    private TextView monitor;
    private ScrollView monitorScroll;
    private final List<String> lines = new ArrayList<>();

    private final Bus.LogSink logSink = line -> runOnUiThread(() -> {
        lines.add(line);
        while (lines.size() > 300) lines.remove(0);
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        monitor.setText(sb.toString());
        monitorScroll.post(() -> monitorScroll.fullScroll(ScrollView.FOCUS_DOWN));
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 10);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("KN Bridge — panneau de contrôle");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Commande MS (la page écrit ici)  ·  Vers Page (la page lit ici)");
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(Color.GRAY);
        root.addView(sub);

        Button svc = new Button(this);
        svc.setText("▶ Démarrer / relancer le service de routage");
        svc.setOnClickListener(v -> {
            Intent i = new Intent(this, KnRouteService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            Toast.makeText(this, "Service démarré", Toast.LENGTH_SHORT).show();
        });
        root.addView(svc);

        CheckBox bleBox = new CheckBox(this);
        bleBox.setText("Se faire voir en Bluetooth MIDI (pour MobileSheets)");
        bleBox.setChecked(Bus.bleEnabled(this));
        bleBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton btn, boolean on) {
                if (on && Build.VERSION.SDK_INT >= 31 && !hasBlePerms()) {
                    btn.setOnCheckedChangeListener(null);
                    btn.setChecked(false);
                    btn.setOnCheckedChangeListener((b2, o) -> toggleBle());
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    return;
                }
                toggleBle();
            }
        });
        root.addView(bleBox);

        Button refresh = new Button(this);
        refresh.setText("🔄 Actualiser la liste des ports USB");
        refresh.setOnClickListener(v -> buildDeviceList());
        root.addView(refresh);

        TextView devTitle = new TextView(this);
        devTitle.setText("Appareils USB — routage par port :");
        devTitle.setTextSize(16);
        root.addView(devTitle);

        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        root.addView(deviceList);

        TextView monTitle = new TextView(this);
        monTitle.setText("Moniteur MIDI :");
        monTitle.setTextSize(16);
        root.addView(monTitle);

        monitor = new TextView(this);
        monitor.setTypeface(android.graphics.Typeface.MONOSPACE);
        monitor.setTextSize(11);
        monitor.setText("—");
        monitor.setPadding(8, 8, 8, 8);
        monitor.setTextColor(Color.rgb(160, 255, 160));
        monitor.setBackgroundColor(Color.rgb(20, 20, 20));
        monitorScroll = new ScrollView(this);
        monitorScroll.addView(monitor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        monitorScroll.setLayoutParams(lp);
        root.addView(monitorScroll);

        setContentView(root);
        buildDeviceList();
        Bus.addLogSink(logSink);
        Bus.log("Panneau ouvert");
    }

    private void toggleBle() {
        Intent i = new Intent(this, KnRouteService.class);
        i.setAction(KnRouteService.ACTION_TOGGLE_BLE);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private boolean hasBlePerms() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != 1) return;
        boolean ok = grantResults.length >= 2;
        for (int r : grantResults) ok = ok && r == PackageManager.PERMISSION_GRANTED;
        if (ok) {
            toggleBle();
            Toast.makeText(this, "Bluetooth autorisé — coche à nouveau", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permissions Bluetooth refusées", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildDeviceList() {
        deviceList.removeAllViews();
        MidiManager mm = (MidiManager) getSystemService(Context.MIDI_SERVICE);
        if (mm == null) { deviceList.addView(mutLabel("Pas de service MIDI")); return; }
        boolean any = false;
        for (MidiDeviceInfo info : mm.getDevices()) {
            if (info.getType() != MidiDeviceInfo.TYPE_USB) continue;
            any = true;
            String name = info.getProperties() != null
                ? info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME, "USB " + info.getId())
                : "USB " + info.getId();
            List<Integer> inPorts = UsbMidiRouter.portNumbers(info, MidiDeviceInfo.PortInfo.TYPE_INPUT);
            List<Integer> outPorts = UsbMidiRouter.portNumbers(info, MidiDeviceInfo.PortInfo.TYPE_OUTPUT);
            deviceList.addView(mutLabel(name));
            int max = Math.max(inPorts.size(), outPorts.size());
            for (int p = 0; p < max; p++) {
                final String nm = name;
                final int port = p;
                boolean hasIn = inPorts.contains(p);
                boolean hasOut = outPorts.contains(p);
                TextView pt = mutLabel("    Port (" + p + ")");
                pt.setTextColor(Color.WHITE);
                deviceList.addView(pt);
                if (hasIn) deviceList.addView(checkBox(nm, port, true,
                    "       TX →  écrire vers ce port (page → matériel)"));
                if (hasOut) deviceList.addView(checkBox(nm, port, false,
                    "       RX ←  lire ce port (matériel → page)"));
            }
        }
        if (!any) deviceList.addView(mutLabel("(aucun appareil USB détecté)"));
    }

    private TextView mutLabel(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.LTGRAY);
        return t;
    }

    private CheckBox checkBox(final String name, final int port, final boolean tx, String label) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setTextSize(13);
        cb.setChecked(Bus.portEnabled(this, name, port, tx));
        cb.setOnCheckedChangeListener((btn, on) -> {
            Bus.setPortEnabled(KnBridgeActivity.this, name, port, tx, on);
            Bus.log((tx ? "TX" : "RX") + " (" + port + ") " + (on ? "activé" : "désactivé")
                + " : " + name + " — relance le service pour appliquer");
        });
        return cb;
    }

    @Override
    protected void onDestroy() {
        Bus.removeLogSink(logSink);
        super.onDestroy();
    }
}
