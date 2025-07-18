package top.cywin.onetv.vod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.api.config.LiveConfig;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        registerCallback();
    }

    private void registerCallback() {
        ConnectivityManager manager = (ConnectivityManager) App.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) manager.registerDefaultNetworkCallback(new Callback());
        else manager.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), new Callback());
    }

    static class Callback extends ConnectivityManager.NetworkCallback {

        private boolean first;

        @Override
        public void onAvailable(@NonNull Network network) {
            if (first) doJob();
            else first = true;
        }

        @Override
        public void onLost(@NonNull Network network) {
        }

        private void doJob() {
            LiveConfig.get().init().load();
            ((ConnectivityManager) App.get().getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(this);
        }
    }
}
