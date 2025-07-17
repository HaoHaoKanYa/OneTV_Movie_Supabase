package top.cywin.onetv.movie.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Part {

    public static List<String> get(String source) {
        List<String> items = new ArrayList<>();
        items.add(source.trim());
        if (source.contains("ï¼?)) {
            for (String split : source.split("ï¼?)) items.add(split.trim().contains(" ") ? split.split(" ")[0].trim() : split.trim());
        } else if (source.contains("ç¬?) && source.contains("å­?)) {
            for (String split : source.split("ç¬?)) if (!split.contains("å­?)) items.add(split.trim().contains(" ") ? split.split(" ")[0].trim() : split.trim());
        } else if (source.contains("(")) {
            items.add(source.split("\\(")[0].trim());
        } else if (source.contains(" ")) {
            items.addAll(Arrays.asList(source.split(" ")));
        }
        return items;
    }
}
