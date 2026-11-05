package net.yvesdomlaurent.knbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Redémarre le routage après un redémarrage de la tablette. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Intent svc = new Intent(ctx, KnRouteService.class);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc);
        else ctx.startService(svc);
    }
}
