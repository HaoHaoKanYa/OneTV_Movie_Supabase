package top.cywin.onetv.vod.gson;

import top.cywin.onetv.vod.App;
import top.cywin.onetv.vod.bean.Danmaku;
import top.github.catvod.utils.Json;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;

public class DanmakuAdapter implements JsonDeserializer<List<Danmaku>> {

    @Override
    public List<Danmaku> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonPrimitive()) return App.gson().fromJson(json, typeOfT);
        String text = json.getAsString().trim();
        if (Json.isArray(text)) return App.gson().fromJson(text, typeOfT);
        else return List.of(Danmaku.from(text));
    }
}
