package top.cywin.onetv.movie.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import top.cywin.onetv.movie.event.ActionEvent;

public class ActionReceiver extends BroadcastReceiver {

    public static PendingIntent getPendingIntent(Context context, String action) {
        return PendingIntent.getBroadcast(context, 0, new Intent(action).setPackage(context.getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ActionEvent.send(intent.getAction());
    }
}
