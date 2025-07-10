package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 解析结果数据类
 * 用于封装视频解析的结果
 */
@Serializable
data class ParseResult(
    val success: Boolean,
    val playUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val errorMessage: String = "",
    val parseTime: Long = 0L,
    val parserName: String = "",
    val quality: String = "",
    val format: String = ""
) {
    companion object {
        /**
         * 创建成功结果
         */
        fun success(
            playUrl: String,
            parseTime: Long = 0L,
            parserName: String = "",
            headers: Map<String, String> = emptyMap(),
            quality: String = "",
            format: String = ""
        ): ParseResult {
            return ParseResult(
                success = true,
                playUrl = playUrl,
                headers = headers,
                parseTime = parseTime,
                parserName = parserName,
                quality = quality,
                format = format
            )
        }
        
        /**
         * 创建失败结果
         */
        fun failure(
            errorMessage: String,
            parseTime: Long = 0L,
            parserName: String = ""
        ): ParseResult {
            return ParseResult(
                success = false,
                errorMessage = errorMessage,
                parseTime = parseTime,
                parserName = parserName
            )
        }
    }
    
    /**
     * 是否解析成功
     */
    val isSuccess: Boolean get() = success && playUrl.isNotEmpty()
    
    /**
     * 是否解析失败
     */
    val isFailure: Boolean get() = !success || playUrl.isEmpty()
}
