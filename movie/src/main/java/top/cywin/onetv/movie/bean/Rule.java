package top.cywin.onetv.movie.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import top.cywin.onetv.movie.App;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class Rule {

    @SerializedName("name")
    private String name;
    @SerializedName("hosts")
    private List<String> hosts;
    @SerializedName("regex")
    private List<String> regex;
    @SerializedName("script")
    private List<String> script;
    @SerializedName("exclude")
    private List<String> exclude;

    public static Rule create(String name) {
        return new Rule(name);
    }

    public static Rule empty() {
        return new Rule("");
    }

    public Rule(String name) {
        this.name = name;
    }

    public static List<Rule> arrayFrom(JsonElement element) {
        Type listType = new TypeToken<List<Rule>>() {}.getType();
        List<Rule> items = App.gson().fromJson(element, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public List<String> getHosts() {
        return hosts == null ? Collections.emptyList() : hosts;
    }

    public List<String> getRegex() {
        return regex == null ? Collections.emptyList() : regex;
    }

    public List<String> getScript() {
        return script == null ? Collections.emptyList() : script;
    }

    public List<String> getExclude() {
        return exclude == null ? Collections.emptyList() : exclude;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Rule)) return false;
        Rule it = (Rule) obj;
        return getName().equals(it.getName());
    }
}
