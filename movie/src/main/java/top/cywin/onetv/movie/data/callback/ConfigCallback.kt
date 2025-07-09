package top.cywin.onetv.movie.data.callback

/**
 * 配置加载回调接口 (参考OneMoVie ConfigCallback)
 */
interface ConfigCallback {
    
    /**
     * 配置加载成功
     */
    fun onSuccess(message: String)
    
    /**
     * 配置加载失败
     */
    fun onError(error: String)
    
    /**
     * 仓库配置加载成功
     */
    fun onDepotSuccess(configs: List<top.cywin.onetv.movie.data.models.VodDepotConfig>)
}

/**
 * 解析回调接口 (参考OneMoVie ParseCallback)
 */
interface ParseCallback {
    
    /**
     * 解析成功
     */
    fun onParseSuccess(playUrl: String, headers: Map<String, String> = emptyMap())
    
    /**
     * 解析失败
     */
    fun onParseError(error: String)
    
    /**
     * 解析进度
     */
    fun onParseProgress(progress: String)
}
