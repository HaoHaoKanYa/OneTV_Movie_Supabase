package top.cywin.onetv.movie.cloudrive.provider;

import java.util.List;

import top.cywin.onetv.movie.cloudrive.bean.CloudFile;

/**
 * 网盘提供商接口
 * 基于FongMi_TV架构设计
 */
public interface CloudProvider {

    /**
     * 列出指定路径下的文件和文件夹
     * @param path 路径
     * @param token 认证令牌
     * @return 文件列表
     * @throws Exception 操作异常
     */
    List<CloudFile> listFiles(String path, String token) throws Exception;

    /**
     * 获取文件下载链接
     * @param fileId 文件ID或路径
     * @param token 认证令牌
     * @return 下载链接
     * @throws Exception 操作异常
     */
    String getDownloadUrl(String fileId, String token) throws Exception;

    /**
     * 搜索文件
     * @param keyword 搜索关键词
     * @param token 认证令牌
     * @return 搜索结果
     * @throws Exception 操作异常
     */
    List<CloudFile> searchFiles(String keyword, String token) throws Exception;

    /**
     * 获取文件详细信息
     * @param fileId 文件ID
     * @param token 认证令牌
     * @return 文件信息
     * @throws Exception 操作异常
     */
    CloudFile getFileInfo(String fileId, String token) throws Exception;

    /**
     * 获取提供商名称
     * @return 提供商名称
     */
    String getProviderName();

    /**
     * 检查令牌是否有效
     * @param token 认证令牌
     * @return 是否有效
     */
    boolean isTokenValid(String token);

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     * @throws Exception 刷新异常
     */
    String refreshToken(String refreshToken) throws Exception;

    /**
     * 清理资源
     */
    void cleanup();
}
