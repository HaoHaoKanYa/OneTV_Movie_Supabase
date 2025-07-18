package top.cywin.onetv.movie.gson;

import top.cywin.onetv.movie.bean.Filter;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FilterAdapter implements JsonDeserializer<LinkedHashMap<String, List<Filter>>> {

    @Override
    public LinkedHashMap<String, List<Filter>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        LinkedHashMap<String, List<Filter>> filterMap = new LinkedHashMap<>();
        JsonObject filters = json.getAsJsonObject();
        if (filters == null) return filterMap;
        for (Map.Entry<String, JsonElement> entry : filters.entrySet()) {
            List<Filter> items = new ArrayList<>();
            JsonElement element = filters.get(entry.getKey());
            if (element.isJsonObject()) items.add(Filter.objectFrom(element).check().trans());
            else for (JsonElement item : element.getAsJsonArray()) items.add(Filter.objectFrom(item).check().trans());
            filterMap.put(entry.getKey(), items);
        }
        return filterMap;
    }
}
