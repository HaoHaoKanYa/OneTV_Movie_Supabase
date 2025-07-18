package top.cywin.onetv.movie.quickjs.method;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import top.cywin.onetv.movie.quickjs.bean.Req;
import top.cywin.onetv.movie.quickjs.utils.Connect;
import top.cywin.onetv.movie.quickjs.utils.Crypto;
import top.cywin.onetv.movie.catvod.Proxy;
import top.cywin.onetv.movie.catvod.utils.Trans;
import top.cywin.onetv.movie.catvod.utils.UriUtil;
import com.orhanobut.logger.Logger;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Global {

    private final ExecutorService executor;
    private final QuickJSContext ctx;
    private final Timer timer;

    public static Global create(QuickJSContext ctx, ExecutorService executor) {
        return new Global(ctx, executor);
    }

    private Global(QuickJSContext ctx, ExecutorService executor) {
        this.executor = executor;
        this.timer = new Timer();
        this.ctx = ctx;
        setProperty();
    }

    private void setProperty() {
        for (Method method : getClass().getMethods()) {
            if (!method.isAnnotationPresent(JSMethod.class)) continue;
            ctx.getGlobalObject().setProperty(method.getName(), args -> {
                try {
                    return method.invoke(this, args);
                } catch (Exception e) {
                    return null;
                }
            });
        }
    }

    private void submit(Runnable runnable) {
        if (!executor.isShutdown()) executor.submit(runnable);
    }

    @Keep
    @JSMethod
    public String s2t(String text) {
        return Trans.s2t(false, text);
    }

    @Keep
    @JSMethod
    public String t2s(String text) {
        return Trans.t2s(false, text);
    }

    @Keep
    @JSMethod
    public Integer getPort() {
        return Proxy.getPort();
    }

    @Keep
    @JSMethod
    public String getProxy(Boolean local) {
        return Proxy.getUrl(local) + "?do=js";
    }

    @Keep
    @JSMethod
    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, JSObject headers) {
        return getProxy(!dynamic) + String.format("&from=catvod&siteType=%s&siteKey=%s&header=%s&url=%s", siteType, siteKey, URLEncoder.encode(headers.stringify()), URLEncoder.encode(url));
    }

    @Keep
    @JSMethod
    public Object setTimeout(JSFunction func, Integer delay) {
        func.hold();
        schedule(func, delay);
        return null;
    }

    @Keep
    @JSMethod
    public JSObject _http(String url, JSObject options) {
        JSFunction complete = options.getJSFunction("complete");
        if (complete == null) return req(url, options);
        Req req = Req.objectFrom(options.stringify());
        Connect.to(url, req).enqueue(getCallback(complete, req));
        return null;
    }

    @Keep
    @JSMethod
    public JSObject req(String url, JSObject options) {
        try {
            Req req = Req.objectFrom(options.stringify());
            Response res = Connect.to(url, req).execute();
            return Connect.success(ctx, req, res);
        } catch (Exception e) {
            return Connect.error(ctx);
        }
    }

    @Keep
    @JSMethod
    public String joinUrl(String parent, String child) {
        return UriUtil.resolve(parent, child);
    }

    @Keep
    @JSMethod
    public String md5X(String text) {
        String result = Crypto.md5(text);
        Logger.t("md5X").d("text:%s\nresult:\n%s", text, result);
        return result;
    }

    @Keep
    @JSMethod
    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        String result = Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64);
        Logger.t("aesX").d("mode:%s\nencrypt:%s\ninBase64:%s\noutBase64:%s\nkey:%s\niv:%s\ninput:\n%s\nresult:\n%s", mode, encrypt, inBase64, outBase64, key, iv, input, result);
        return result;
    }

    @Keep
    @JSMethod
    public String rsaX(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        String result = Crypto.rsa(mode, pub, encrypt, input, inBase64, key, outBase64);
        Logger.t("rsaX").d("mode:%s\npub:%s\nencrypt:%s\ninBase64:%s\noutBase64:%s\nkey:\n%s\ninput:\n%s\nresult:\n%s", mode, pub, encrypt, inBase64, outBase64, key, input, result);
        return result;
    }

    private Callback getCallback(JSFunction complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                submit(() -> complete.call(Connect.success(ctx, req, res)));
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                submit(() -> complete.call(Connect.error(ctx)));
            }
        };
    }

    private void schedule(JSFunction func, int delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                submit(func::call);
            }
        }, delay);
    }
}
