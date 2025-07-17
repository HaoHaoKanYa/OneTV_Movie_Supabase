package top.cywin.onetv.movie.server;

import top.cywin.onetv.movie.api.config.LiveConfig;
import top.cywin.onetv.movie.bean.Device;
import top.cywin.onetv.movie.server.impl.Process;
import top.cywin.onetv.movie.server.process.Action;
import top.cywin.onetv.movie.server.process.Cache;
import top.cywin.onetv.movie.server.process.Local;
import top.cywin.onetv.movie.server.process.Media;
import top.cywin.onetv.movie.server.process.Parse;
import top.cywin.onetv.movie.server.process.Proxy;
import top.cywin.onetv.movie.catvod.utils.Asset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Nano extends NanoHTTPD {

    private static final String INDEX = "index.html";

    private List<Process> process;

    public Nano(int port) {
        super(port);
        addProcess();
    }

    private void addProcess() {
        process = new ArrayList<>();
        process.add(new Action());
        process.add(new Cache());
        process.add(new Local());
        process.add(new Media());
        process.add(new Parse());
        process.add(new Proxy());
    }

    public static Response ok() {
        return ok("OK");
    }

    public static Response ok(String text) {
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, text);
    }

    public static Response error(String text) {
        return error(Response.Status.INTERNAL_ERROR, text);
    }

    public static Response error(Response.Status status, String text) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, text);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri().trim();
        Map<String, String> files = new HashMap<>();
        if (session.getMethod() == Method.POST) parse(session, files);
        if (url.startsWith("/tvbus")) return ok(LiveConfig.getResp());
        if (url.startsWith("/device")) return ok(Device.get().toString());
        for (Process process : process) if (process.isRequest(session, url)) return process.doResponse(session, url, files);
        return getAssets(url.substring(1));
    }

    private void parse(IHTTPSession session, Map<String, String> files) {
        try {
            session.parseBody(files);
        } catch (Exception ignored) {
        }
    }

    private Response getAssets(String path) {
        try {
            if (path.isEmpty()) path = INDEX;
            InputStream is = Asset.open(path);
            return newFixedLengthResponse(Response.Status.OK, getMimeTypeForFile(path), is, is.available());
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, null, 0);
        }
    }
}
