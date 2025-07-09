package top.cywin.onetv.movie.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.cywin.onetv.movie.ui.screens.MovieHomeScreen
import top.cywin.onetv.movie.ui.screens.MovieCategoryScreen
import top.cywin.onetv.movie.ui.screens.MovieDetailScreen
import top.cywin.onetv.movie.ui.screens.MoviePlayerScreen
import top.cywin.onetv.movie.ui.screens.MovieSearchScreen
import top.cywin.onetv.movie.ui.screens.MovieHistoryScreen
import top.cywin.onetv.movie.ui.screens.MovieSettingsScreen

/**
 * Movie模块导航扩展 (TVBOX标准)
 */
fun NavGraphBuilder.movieNavigation(navController: NavController) {
    
    // 点播首页
    composable("movie_home") {
        MovieHomeScreen(navController = navController)
    }
    
    // 分类页面
    composable(
        "movie_category/{typeId}",
        arguments = listOf(navArgument("typeId") { type = NavType.StringType })
    ) { backStackEntry ->
        val typeId = backStackEntry.arguments?.getString("typeId") ?: ""
        MovieCategoryScreen(
            typeId = typeId,
            navController = navController
        )
    }
    
    // 分类页面（带站点参数）
    composable(
        "movie_category/{typeId}/{siteKey}",
        arguments = listOf(
            navArgument("typeId") { type = NavType.StringType },
            navArgument("siteKey") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val typeId = backStackEntry.arguments?.getString("typeId") ?: ""
        val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
        MovieCategoryScreen(
            typeId = typeId,
            siteKey = siteKey,
            navController = navController
        )
    }
    
    // 详情页面
    composable(
        "movie_detail/{vodId}",
        arguments = listOf(navArgument("vodId") { type = NavType.StringType })
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        MovieDetailScreen(
            vodId = vodId,
            navController = navController
        )
    }
    
    // 详情页面（带站点参数）
    composable(
        "movie_detail/{vodId}/{siteKey}",
        arguments = listOf(
            navArgument("vodId") { type = NavType.StringType },
            navArgument("siteKey") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
        MovieDetailScreen(
            vodId = vodId,
            siteKey = siteKey,
            navController = navController
        )
    }
    
    // 播放页面
    composable(
        "movie_player/{vodId}/{episodeIndex}",
        arguments = listOf(
            navArgument("vodId") { type = NavType.StringType },
            navArgument("episodeIndex") { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
        MoviePlayerScreen(
            vodId = vodId,
            episodeIndex = episodeIndex,
            navController = navController
        )
    }
    
    // 播放页面（带站点参数）
    composable(
        "movie_player/{vodId}/{episodeIndex}/{siteKey}",
        arguments = listOf(
            navArgument("vodId") { type = NavType.StringType },
            navArgument("episodeIndex") { type = NavType.IntType },
            navArgument("siteKey") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
        val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
        val siteKey = backStackEntry.arguments?.getString("siteKey") ?: ""
        MoviePlayerScreen(
            vodId = vodId,
            episodeIndex = episodeIndex,
            siteKey = siteKey,
            navController = navController
        )
    }
    
    // 搜索页面
    composable("movie_search") {
        MovieSearchScreen(navController = navController)
    }
    
    // 搜索页面（带关键词）
    composable(
        "movie_search/{keyword}",
        arguments = listOf(navArgument("keyword") { type = NavType.StringType })
    ) { backStackEntry ->
        val keyword = backStackEntry.arguments?.getString("keyword") ?: ""
        MovieSearchScreen(
            initialKeyword = keyword,
            navController = navController
        )
    }
    
    // 历史记录页面
    composable("movie_history") {
        MovieHistoryScreen(navController = navController)
    }
    
    // 设置页面
    composable("movie_settings") {
        MovieSettingsScreen(navController = navController)
    }
}

/**
 * Movie导航路由常量
 */
object MovieRoutes {
    const val HOME = "movie_home"
    const val CATEGORY = "movie_category"
    const val DETAIL = "movie_detail"
    const val PLAYER = "movie_player"
    const val SEARCH = "movie_search"
    const val HISTORY = "movie_history"
    const val SETTINGS = "movie_settings"
    
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
