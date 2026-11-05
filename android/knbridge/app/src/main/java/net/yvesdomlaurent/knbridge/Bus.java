package net.yvesdomlaurent.knbridge;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Carrefour central : tous les messages MIDI passent ici, avec journalisation. */
public final class Bus {
    public static final String PREFS = "knbridge";

    public interface Sink {
        void send(byte[] msg, int offset, int count, long timestamp);
    }

    public interface LogSink {
        void onLog(String line);
    }

    private static final List<Sink> usbSinks = new ArrayList<>();  // page -> USB
    private static final List<Sink> bleSinks = new ArrayList<>();  // page -> MS (Bluetooth)
    private static volatile Sink virtualSink;                      // -> page ("Vers Page")

    private static final List<LogSink> logSinks = new ArrayList<>();
    private static final SimpleDateFormat hhmmss = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private Bus() {}

    public static synchronized void setVirtualSink(Sink s) { virtualSink = s; }

    public static synchronized void addUsbSink(Sink s) { if (!usbSinks.contains(s)) usbSinks.add(s); }
    public static synchronized void clearUsbSinks() { usbSinks.clear(); }
    public static synchronized void addBleSink(Sink s) { if (!bleSinks.contains(s)) bleSinks.add(s); }
    public static synchronized void removeBleSink(Sink s) { bleSinks.remove(s); }

    /** Message venant de la page (port virtuel « Commande MS ») -> USB + Bluetooth. */
    public static synchronized void fromVirtual(byte[] msg, int off, int cnt, long ts) {
        log("➡ PAGE : " + hex(msg, off, cnt));
        for (Sink s : new ArrayList<>(usbSinks)) { try { s.send(msg, off, cnt, ts); } catch (Exception e) { log("⚠ USB : " + e); } }
        for (Sink s : new ArrayList<>(bleSinks)) { try { s.send(msg, off, cnt, ts); } catch (Exception e) { log("⚠ BLE : " + e); } }
    }

    /** Message venant d'un périphérique USB (KN2000, APC via boîtier) -> page. */
    public static synchronized void fromUsb(String devName, byte[] msg, int off, int cnt, long ts) {
        log("⬅ USB " + devName + " : " + hex(msg, off, cnt));
        toVirtual(msg, off, cnt, ts);
    }

    /** Message venant de MobileSheets (Bluetooth) -> page. */
    public static synchronized void fromBle(byte[] msg, int off, int cnt, long ts) {
        log("⬅ BLE (MS) : " + hex(msg, off, cnt));
        toVirtual(msg, off, cnt, ts);
    }

    private static void toVirtual(byte[] msg, int off, int cnt, long ts) {
        Sink s = virtualSink;
        if (s != null) { try { s.send(msg, off, cnt, ts); } catch (Exception e) { log("⚠ Virtuel : " + e); } }
    }

    public static void addLogSink(LogSink l) { synchronized (logSinks) { logSinks.add(l); } }
    public static void removeLogSink(LogSink l) { synchronized (logSinks) { logSinks.remove(l); } }

    public static void log(String s) {
        String line = hhmmss.format(new Date()) + "  " + s;
        synchronized (logSinks) { for (LogSink l : new ArrayList<>(logSinks)) l.onLog(line); }
        android.util.Log.i("KNBridge", s);
    }

    public static String hex(byte[] msg, int off, int cnt) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(cnt, 20);
        for (int i = 0; i < n; i++) sb.append(String.format("%02X ", msg[off + i] & 0xFF));
        if (cnt > n) sb.append("…(").append(cnt).append(" o)");
        return sb.toString().trim();
    }

    // ---- Préférences ----
    // v1.2 : par appareil (conservé pour reprendre l'ancienne config par défaut)
    public static boolean deviceEnabled(Context ctx, String name, boolean tx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(name + (tx ? "|tx" : "|rx"), false);
    }
    public static void setDeviceEnabled(Context ctx, String name, boolean tx, boolean on) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(name + (tx ? "|tx" : "|rx"), on).apply();
    }

    // v1.3 : par PORT. Si la clé port n'existe pas, reprend la valeur v1.2 de l'appareil.
    public static boolean portEnabled(Context ctx, String name, int port, boolean tx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String k = name + "|" + port + "|" + (tx ? "tx" : "rx");
        if (sp.contains(k)) return sp.getBoolean(k, false);
        return deviceEnabled(ctx, name, tx);
    }
    public static void setPortEnabled(Context ctx, String name, int port, boolean tx, boolean on) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(name + "|" + port + "|" + (tx ? "tx" : "rx"), on).apply();
    }

    public static boolean bleEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ble", false);
    }
    public static void setBleEnabled(Context ctx, boolean on) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ble", on).apply();
    }
}
