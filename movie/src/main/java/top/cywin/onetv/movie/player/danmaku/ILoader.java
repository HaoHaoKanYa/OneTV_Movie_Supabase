package top.cywin.onetv.movie.player.danmaku;

import java.io.InputStream;

import master.flame.danmaku.danmaku.parser.android.AndroidFileSource;
import master.flame.danmaku.danmaku.parser.IllegalDataException;

public interface ILoader {
    void load(String url) throws IllegalDataException;
    void load(InputStream stream) throws IllegalDataException;
    AndroidFileSource getDataSource();
}
