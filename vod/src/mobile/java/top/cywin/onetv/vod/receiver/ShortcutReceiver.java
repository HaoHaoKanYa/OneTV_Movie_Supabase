package top.cywin.onetv.vod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import top.cywin.onetv.vod.R;
import top.cywin.onetv.vod.utils.Notify;

public class ShortcutReceiver extends BroadcastReceiver {

    public static final String ACTION = ShortcutReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Notify.show(R.string.shortcut);
    }
}