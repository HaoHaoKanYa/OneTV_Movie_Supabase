package top.cywin.onetv.film.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.cywin.onetv.film.ui.screens.*

/**
 * Film模块导航扩展
 * 
 * 基于 FongMi/TV 的完整影视解析系统导航
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
fun NavGraphBuilder.filmNavigation(navController: NavController) {
    
    // 影视首页
    composable("film_home") {
        Log.d("ONETV_FILM", "进入影视首页路由")
        FilmHomeScreen(navController = navController)
    }
    
    // 分类页面
    composable(
        "film_category/{typeId}",
        arguments = listOf(navArgument("typeId") { type = NavType.StringType })
    ) { backStackEntry ->
        val typeId = backStackEntry.arguments?.getString("typeId") ?: ""
        FilmCategoryScreen(
            typeId = typeId,
            navController = navController
        )
    }
    
    // 分类页面（带站点参数）
    composable(
        "film_category/{typeId}/{siteKey}",
        arguments = listOf(
            navArgument("typeId") { type = NavType.StringType },
            navArgument("siteKey") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val typeId = backStackEntry.arguments?.getString("typeId") ?: ""
        val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
        FilmCategoryScreen(
            typeId = typeId,
            siteKey = siteKey,
            navController = navController
        )
    }
    
    // 详情页面
    composable(
        "film_detail/{vodId}",
        arguments = listOf(navArgument("vodId") { type = NavType.StringType })
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        FilmDetailScreen(
            vodId = vodId,
            navController = navController
        )
    }
    
    // 详情页面（带站点参数）
    composable(
        "film_detail/{vodId}/{siteKey}",
        arguments = listOf(
            navArgument("vodId") { type = NavType.StringType },
            navArgument("siteKey") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
        FilmDetailScreen(
            vodId = vodId,
            siteKey = siteKey,
            navController = navController
        )
    }
    
    // 播放页面
    composable(
        "film_player/{vodId}/{episodeIndex}",
        arguments = listOf(
            navArgument("vodId") { type = NavType.StringType },
            navArgument("episodeIndex") { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
        FilmPlayerScreen(
            vodId = vodId,
            episodeIndex = episodeIndex,
            navController = navController
        )
    }
    
    // 播放页面（带站点参数）
    composable(
        "film_player/{vodId}/{episodeIndex}/{siteKey}",
        arguments = listOf(
            navArgument("vodId") { type = NavType.StringType },
            navArgument("episodeIndex") { type = NavType.IntType },
            navArgument("siteKey") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
        val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
        FilmPlayerScreen(
            vodId = vodId,
            episodeIndex = episodeIndex,
            siteKey = siteKey,
            navController = navController
        )
    }
    
    // 搜索页面
    composable("film_search") {
        FilmSearchScreen(navController = navController)
    }
    
    // 搜索页面（带关键词）
    composable(
        "film_search/{keyword}",
        arguments = listOf(navArgument("keyword") { type = NavType.StringType })
    ) { backStackEntry ->
        val keyword = backStackEntry.arguments?.getString("keyword") ?: ""
        FilmSearchScreen(
            initialKeyword = keyword,
            navController = navController
        )
    }
    
    // 历史记录页面
    composable("film_history") {
        FilmHistoryScreen(navController = navController)
    }
    
    // 设置页面
    composable("film_settings") {
        FilmSettingsScreen(navController = navController)
    }
    
    // 测试页面
    composable("film_test") {
        FilmTestScreen(navController = navController)
    }
}

/**
 * Film导航路由常量
 */
object FilmRoutes {
    const val HOME = "film_home"
    const val CATEGORY = "film_category"
    const val DETAIL = "film_detail"
    const val PLAYER = "film_player"
    const val SEARCH = "film_search"
    const val HISTORY = "film_history"
    const val SETTINGS = "film_settings"
    const val TEST = "film_test"
    
    /**
     * 构建分类路由
     */
    fun category(typeId: String, siteKey: String? = null): String {
        return if (siteKey != null) {
            "$CATEGORY/$typeId/$siteKey"
        } else {
            "$CATEGORY/$typeId"
        }
    }
    
    /**
     * 构建详情路由
     */
    fun detail(vodId: String, siteKey: String? = null): String {
        return if (siteKey != null) {
            "$DETAIL/$vodId/$siteKey"
        } else {
            "$DETAIL/$vodId"
        }
    }
    
    /**
     * 构建播放路由
     */
    fun player(vodId: String, episodeIndex: Int, siteKey: String? = null): String {
        return if (siteKey != null) {
            "$PLAYER/$vodId/$episodeIndex/$siteKey"
        } else {
            "$PLAYER/$vodId/$episodeIndex"
        }
    }
    
    /**
     * 构建搜索路由
     */
    fun search(keyword: String? = null): String {
        return if (keyword != null) {
            "$SEARCH/$keyword"
        } else {
            SEARCH
        }
    }
}
