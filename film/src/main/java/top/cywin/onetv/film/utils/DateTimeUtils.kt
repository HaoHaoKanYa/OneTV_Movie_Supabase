package top.cywin.onetv.film.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 日期时间工具类
 * 
 * 基于 FongMi/TV 的日期时间处理工具实现
 * 提供日期时间格式化、解析、计算等功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object DateTimeUtils {
    
    private const val TAG = "ONETV_FILM_DATETIME_UTILS"
    
    // 常用日期时间格式
    const val FORMAT_YYYY_MM_DD = "yyyy-MM-dd"
    const val FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS"
    const val FORMAT_MM_DD_HH_MM = "MM-dd HH:mm"
    const val FORMAT_HH_MM_SS = "HH:mm:ss"
    const val FORMAT_HH_MM = "HH:mm"
    const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val FORMAT_RFC_2822 = "EEE, dd MMM yyyy HH:mm:ss zzz"
    
    // 时间单位常量
    const val SECOND_MILLIS = 1000L
    const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    const val DAY_MILLIS = 24 * HOUR_MILLIS
    const val WEEK_MILLIS = 7 * DAY_MILLIS
    
    /**
     * 📅 获取当前时间戳（毫秒）
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * 📅 获取当前时间戳（秒）
     */
    fun getCurrentTimestampSeconds(): Long {
        return System.currentTimeMillis() / 1000
    }
    
    /**
     * 📅 格式化时间戳
     */
    fun formatTimestamp(timestamp: Long, pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            Log.e(TAG, "❌ 时间戳格式化失败: $timestamp, pattern: $pattern", e)
            timestamp.toString()
        }
    }
    
    /**
     * 📅 格式化当前时间
     */
    fun formatCurrentTime(pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): String {
        return formatTimestamp(getCurrentTimestamp(), pattern)
    }
    
    /**
     * 📅 解析时间字符串为时间戳
     */
    fun parseTimestamp(timeStr: String?, pattern: String = FORMAT_YYYY_MM_DD_HH_MM_SS): Long {
        if (StringUtils.isEmpty(timeStr)) return 0L
        
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(timeStr!!)?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "❌ 时间字符串解析失败: $timeStr, pattern: $pattern", e)
            0L
        }
    }
    
    /**
     * 📅 智能解析时间字符串（尝试多种格式）
     */
    fun parseTimestampSmart(timeStr: String?): Long {
        if (StringUtils.isEmpty(timeStr)) return 0L
        
        val patterns = listOf(
            FORMAT_YYYY_MM_DD_HH_MM_SS,
            FORMAT_YYYY_MM_DD_HH_MM_SS_SSS,
            FORMAT_YYYY_MM_DD,
            FORMAT_ISO_8601,
            FORMAT_RFC_2822,
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy"
        )
        
        for (pattern in patterns) {
            val timestamp = parseTimestamp(timeStr, pattern)
            if (timestamp > 0) {
                return timestamp
            }
        }
        
        // 尝试解析为时间戳
        return try {
            val timestamp = timeStr!!.toLong()
            // 判断是秒还是毫秒
            if (timestamp < 10000000000L) { // 小于这个值认为是秒
                timestamp * 1000
            } else {
                timestamp
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 无法解析时间字符串: $timeStr")
            0L
        }
    }
    
    /**
     * 📅 获取今天的开始时间（00:00:00）
     */
    fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 📅 获取今天的结束时间（23:59:59.999）
     */
    fun getTodayEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 📅 获取昨天的开始时间
     */
    fun getYesterdayStart(): Long {
        return getTodayStart() - DAY_MILLIS
    }
    
    /**
     * 📅 获取昨天的结束时间
     */
    fun getYesterdayEnd(): Long {
        return getTodayEnd() - DAY_MILLIS
    }
    
    /**
     * 📅 获取明天的开始时间
     */
    fun getTomorrowStart(): Long {
        return getTodayStart() + DAY_MILLIS
    }
    
    /**
     * 📅 获取明天的结束时间
     */
    fun getTomorrowEnd(): Long {
        return getTodayEnd() + DAY_MILLIS
    }
    
    /**
     * 📅 获取本周的开始时间（周一 00:00:00）
     */
    fun getThisWeekStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 📅 获取本周的结束时间（周日 23:59:59.999）
     */
    fun getThisWeekEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 📅 获取本月的开始时间
     */
    fun getThisMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 📅 获取本月的结束时间
     */
    fun getThisMonthEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * ⏱️ 计算时间差（毫秒）
     */
    fun getTimeDifference(startTime: Long, endTime: Long): Long {
        return endTime - startTime
    }
    
    /**
     * ⏱️ 格式化持续时间
     */
    fun formatDuration(durationMillis: Long): String {
        val seconds = durationMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}天${hours % 24}小时${minutes % 60}分钟"
            hours > 0 -> "${hours}小时${minutes % 60}分钟${seconds % 60}秒"
            minutes > 0 -> "${minutes}分钟${seconds % 60}秒"
            else -> "${seconds}秒"
        }
    }
    
    /**
     * ⏱️ 格式化持续时间（简短版本）
     */
    fun formatDurationShort(durationMillis: Long): String {
        val seconds = durationMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * ⏱️ 格式化时间长度（HH:mm:ss 格式）
     */
    fun formatTimeLength(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = durationMillis / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 📅 获取相对时间描述
     */
    fun getRelativeTimeDescription(timestamp: Long): String {
        val now = getCurrentTimestamp()
        val diff = now - timestamp
        
        return when {
            diff < 0 -> "未来"
            diff < MINUTE_MILLIS -> "刚刚"
            diff < HOUR_MILLIS -> "${diff / MINUTE_MILLIS}分钟前"
            diff < DAY_MILLIS -> "${diff / HOUR_MILLIS}小时前"
            diff < WEEK_MILLIS -> "${diff / DAY_MILLIS}天前"
            diff < 30 * DAY_MILLIS -> "${diff / WEEK_MILLIS}周前"
            diff < 365 * DAY_MILLIS -> "${diff / (30 * DAY_MILLIS)}个月前"
            else -> "${diff / (365 * DAY_MILLIS)}年前"
        }
    }
    
    /**
     * 📅 检查是否为今天
     */
    fun isToday(timestamp: Long): Boolean {
        val today = getTodayStart()
        val tomorrow = getTomorrowStart()
        return timestamp >= today && timestamp < tomorrow
    }
    
    /**
     * 📅 检查是否为昨天
     */
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = getYesterdayStart()
        val today = getTodayStart()
        return timestamp >= yesterday && timestamp < today
    }
    
    /**
     * 📅 检查是否为本周
     */
    fun isThisWeek(timestamp: Long): Boolean {
        val weekStart = getThisWeekStart()
        val weekEnd = getThisWeekEnd()
        return timestamp >= weekStart && timestamp <= weekEnd
    }
    
    /**
     * 📅 检查是否为本月
     */
    fun isThisMonth(timestamp: Long): Boolean {
        val monthStart = getThisMonthStart()
        val monthEnd = getThisMonthEnd()
        return timestamp >= monthStart && timestamp <= monthEnd
    }
    
    /**
     * 📅 获取年份
     */
    fun getYear(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR)
    }
    
    /**
     * 📅 获取月份（1-12）
     */
    fun getMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MONTH) + 1
    }
    
    /**
     * 📅 获取日期（1-31）
     */
    fun getDayOfMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.DAY_OF_MONTH)
    }
    
    /**
     * 📅 获取星期几（1=周日，2=周一，...，7=周六）
     */
    fun getDayOfWeek(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.DAY_OF_WEEK)
    }
    
    /**
     * 📅 获取星期几的中文名称
     */
    fun getDayOfWeekName(timestamp: Long): String {
        val dayOfWeek = getDayOfWeek(timestamp)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "未知"
        }
    }
    
    /**
     * 📅 获取小时（0-23）
     */
    fun getHour(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    /**
     * 📅 获取分钟（0-59）
     */
    fun getMinute(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MINUTE)
    }
    
    /**
     * 📅 获取秒（0-59）
     */
    fun getSecond(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.SECOND)
    }
    
    /**
     * 📅 添加时间
     */
    fun addTime(timestamp: Long, field: Int, amount: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(field, amount)
        return calendar.timeInMillis
    }
    
    /**
     * 📅 添加天数
     */
    fun addDays(timestamp: Long, days: Int): Long {
        return addTime(timestamp, Calendar.DAY_OF_MONTH, days)
    }
    
    /**
     * 📅 添加小时
     */
    fun addHours(timestamp: Long, hours: Int): Long {
        return addTime(timestamp, Calendar.HOUR_OF_DAY, hours)
    }
    
    /**
     * 📅 添加分钟
     */
    fun addMinutes(timestamp: Long, minutes: Int): Long {
        return addTime(timestamp, Calendar.MINUTE, minutes)
    }
    
    /**
     * 📅 添加秒
     */
    fun addSeconds(timestamp: Long, seconds: Int): Long {
        return addTime(timestamp, Calendar.SECOND, seconds)
    }
    
    /**
     * 📅 获取时间范围描述
     */
    fun getTimeRangeDescription(startTime: Long, endTime: Long): String {
        val startDate = formatTimestamp(startTime, FORMAT_YYYY_MM_DD)
        val endDate = formatTimestamp(endTime, FORMAT_YYYY_MM_DD)
        
        return if (startDate == endDate) {
            // 同一天
            val startTimeStr = formatTimestamp(startTime, FORMAT_HH_MM)
            val endTimeStr = formatTimestamp(endTime, FORMAT_HH_MM)
            "$startDate $startTimeStr-$endTimeStr"
        } else {
            // 不同天
            val startStr = formatTimestamp(startTime, FORMAT_MM_DD_HH_MM)
            val endStr = formatTimestamp(endTime, FORMAT_MM_DD_HH_MM)
            "$startStr 至 $endStr"
        }
    }
    
    /**
     * ⏰ 检查时间是否在指定范围内
     */
    fun isTimeInRange(timestamp: Long, startTime: Long, endTime: Long): Boolean {
        return timestamp >= startTime && timestamp <= endTime
    }
    
    /**
     * 🕐 获取时间段描述
     */
    fun getTimePeriodDescription(timestamp: Long): String {
        val hour = getHour(timestamp)
        return when (hour) {
            in 0..5 -> "凌晨"
            in 6..8 -> "早晨"
            in 9..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            in 18..19 -> "傍晚"
            in 20..23 -> "晚上"
            else -> "未知"
        }
    }
    
    /**
     * 📊 获取时间统计信息
     */
    fun getTimeStats(timestamps: List<Long>): Map<String, Any> {
        if (timestamps.isEmpty()) {
            return mapOf(
                "count" to 0,
                "earliest" to 0L,
                "latest" to 0L,
                "span" to 0L
            )
        }
        
        val sorted = timestamps.sorted()
        val earliest = sorted.first()
        val latest = sorted.last()
        val span = latest - earliest
        
        return mapOf(
            "count" to timestamps.size,
            "earliest" to earliest,
            "latest" to latest,
            "span" to span,
            "span_formatted" to formatDuration(span),
            "earliest_formatted" to formatTimestamp(earliest),
            "latest_formatted" to formatTimestamp(latest)
        )
    }
}
