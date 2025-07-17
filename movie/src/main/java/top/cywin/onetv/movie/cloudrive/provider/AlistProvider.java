package top.cywin.onetv.movie.cloudrive.provider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.cywin.onetv.movie.cloudrive.bean.CloudFile;
import top.cywin.onetv.movie.catvod.net.OkHttp;

/**
 * Alist网盘提供商
 * 基于FongMi_TV架构设计
 */
public class AlistProvider implements CloudProvider {
    private static final String TAG = "AlistProvider";

    @Override
    public List<CloudFile> listFiles(String path, String token) throws Exception {
        String url = getBaseUrl(token) + "/api/fs/list";
        JSONObject params = new JSONObject();
        params.put("path", path);
        params.put("password", "");
        params.put("page", 1);
        params.put("per_page", 100);
        params.put("refresh", false);

        String response = OkHttp.post(url, params.toString(), getHeaders(token));
        JSONObject result = new JSONObject(response);

        if (result.getInt("code") != 200) {
            throw new Exception("Alist API error: " + result.getString("message"));
        }

        List<CloudFile> files = new ArrayList<>();
        JSONArray content = result.getJSONObject("data").getJSONArray("content");

        for (int i = 0; i < content.length(); i++) {
            JSONObject item = content.getJSONObject(i);
            CloudFile file = new CloudFile();
            file.setName(item.getString("name"));
            file.setPath(path + "/" + item.getString("name"));
            file.setSize(item.optLong("size", 0));
            file.setType(item.getInt("is_dir") == 1 ? CloudFile.TYPE_FOLDER : CloudFile.TYPE_FILE);
            file.setModified(item.optString("modified", ""));
            files.add(file);
        }

        return files;
    }

    @Override
    public String getDownloadUrl(String filePath, String token) throws Exception {
        String url = getBaseUrl(token) + "/api/fs/get";
        JSONObject params = new JSONObject();
        params.put("path", filePath);
        params.put("password", "");

        String response = OkHttp.post(url, params.toString(), getHeaders(token));
        JSONObject result = new JSONObject(response);

        if (result.getInt("code") != 200) {
            throw new Exception("Alist API error: " + result.getString("message"));
        }

        return result.getJSONObject("data").getString("raw_url");
    }

    @Override
    public List<CloudFile> searchFiles(String keyword, String token) throws Exception {
        String url = getBaseUrl(token) + "/api/fs/search";
        JSONObject params = new JSONObject();
        params.put("keywords", keyword);
        params.put("scope", 0);
        params.put("page", 1);
        params.put("per_page", 100);

        String response = OkHttp.post(url, params.toString(), getHeaders(token));
        JSONObject result = new JSONObject(response);

        if (result.getInt("code") != 200) {
            throw new Exception("Alist API error: " + result.getString("message"));
        }

        List<CloudFile> files = new ArrayList<>();
        JSONArray content = result.getJSONObject("data").getJSONArray("content");

        for (int i = 0; i < content.length(); i++) {
            JSONObject item = content.getJSONObject(i);
            CloudFile file = new CloudFile();
            file.setName(item.getString("name"));
            file.setPath(item.getString("path"));
            file.setSize(item.optLong("size", 0));
            file.setType(item.getInt("is_dir") == 1 ? CloudFile.TYPE_FOLDER : CloudFile.TYPE_FILE);
            file.setModified(item.optString("modified", ""));
            files.add(file);
        }

        return files;
    }

    @Override
    public CloudFile getFileInfo(String fileId, String token) throws Exception {
        String url = getBaseUrl(token) + "/api/fs/get";
        JSONObject params = new JSONObject();
        params.put("path", fileId);
        params.put("password", "");

        String response = OkHttp.post(url, params.toString(), getHeaders(token));
        JSONObject result = new JSONObject(response);

        if (result.getInt("code") != 200) {
            throw new Exception("Alist API error: " + result.getString("message"));
        }

        JSONObject data = result.getJSONObject("data");
        CloudFile file = new CloudFile();
        file.setId(fileId);
        file.setName(data.getString("name"));
        file.setPath(fileId);
        file.setSize(data.optLong("size", 0));
        file.setType(data.getInt("is_dir") == 1 ? CloudFile.TYPE_FOLDER : CloudFile.TYPE_FILE);
        file.setModified(data.optString("modified", ""));
        file.setDownloadUrl(data.optString("raw_url", ""));
        file.setMimeType(data.optString("type", ""));

        return file;
    }

    @Override
    public String getProviderName() {
        return "Alist";
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            String url = getBaseUrl(token) + "/api/me";
            String response = OkHttp.get(url, getHeaders(token));
            JSONObject result = new JSONObject(response);
            return result.getInt("code") == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String refreshToken(String refreshToken) throws Exception {
        // Alist通常使用长期token，不需要刷新
        return refreshToken;
    }

    @Override
    public void cleanup() {
        // 清理资源
    }

    private String getBaseUrl(String token) {
        // 从token中解析Alist服务器地址
        return token.split("\\|")[0];
    }

    private Map<String, String> getHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (token.contains("|")) {
            String[] parts = token.split("\\|");
            if (parts.length > 1) {
                headers.put("Authorization", "Bearer " + parts[1]);
            }
        }
        return headers;
    }
}
