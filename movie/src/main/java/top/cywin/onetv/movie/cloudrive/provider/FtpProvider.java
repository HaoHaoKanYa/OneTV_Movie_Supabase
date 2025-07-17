package top.cywin.onetv.movie.cloudrive.provider;

import java.util.ArrayList;
import java.util.List;

import top.cywin.onetv.movie.cloudrive.bean.CloudFile;

/**
 * FTP提供商
 * 基于FongMi_TV架构设计
 */
public class FtpProvider implements CloudProvider {
    private static final String TAG = "FtpProvider";

    @Override
    public List<CloudFile> listFiles(String path, String token) throws Exception {
        // TODO: 实现FTP文件列表获取
        return new ArrayList<>();
    }

    @Override
    public String getDownloadUrl(String fileId, String token) throws Exception {
        // TODO: 实现FTP下载链接获取
        return "";
    }

    @Override
    public List<CloudFile> searchFiles(String keyword, String token) throws Exception {
        // TODO: 实现FTP文件搜索
        return new ArrayList<>();
    }

    @Override
    public CloudFile getFileInfo(String fileId, String token) throws Exception {
        // TODO: 实现FTP文件信息获取
        return null;
    }

    @Override
    public String getProviderName() {
        return "FTP";
    }

    @Override
    public boolean isTokenValid(String token) {
        // TODO: 实现FTP连接验证
        return false;
    }

    @Override
    public String refreshToken(String refreshToken) throws Exception {
        // FTP使用用户名密码，不需要刷新token
        return refreshToken;
    }

    @Override
    public void cleanup() {
        // 清理资源
    }
}
