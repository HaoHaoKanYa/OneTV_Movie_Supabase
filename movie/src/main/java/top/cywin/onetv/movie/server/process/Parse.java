package top.cywin.onetv.movie.server.process;

import top.cywin.onetv.movie.server.Nano;
import top.cywin.onetv.movie.server.impl.Process;
import top.cywin.onetv.movie.catvod.utils.Asset;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Parse implements Process {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String url) {
        return url.startsWith("/parse");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files) {
        try {
            Map<String, String> params = session.getParms();
            String html = String.format(Asset.read("parse.html"), params.get("jxs"), params.get("url"));
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, html);
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }
}
