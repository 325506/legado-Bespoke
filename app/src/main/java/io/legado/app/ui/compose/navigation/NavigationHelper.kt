package io.legado.app.ui.compose.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation.NavHostController
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.info.BookInfoComposeActivity
import io.legado.app.ui.book.search.SearchComposeActivity
import io.legado.app.ui.config.ConfigComposeActivity
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.book.manage.BookshelfManageActivity
import io.legado.app.ui.book.toc.TocComposeActivity
import io.legado.app.ui.book.bookmark.AllBookmarkActivity
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.ui.file.FileManageComposeActivity
import io.legado.app.ui.code.CodeEditActivity
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.ui.video.VideoPlayerActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.rss.source.manage.RssSourceComposeActivity
import io.legado.app.ui.replace.ReplaceRuleComposeActivity
import io.legado.app.ui.book.toc.rule.TxtTocRuleComposeActivity
import io.legado.app.ui.browser.WebViewActivity

object NavigationHelper {

    fun navigateToMain(navController: NavHostController) {
        navController.navigate(Screen.Main.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToBookDetail(navController: NavHostController, bookUrl: String) {
        navController.navigate(Screen.BookDetail.createRoute(bookUrl))
    }

    fun navigateToBookSearch(navController: NavHostController) {
        navController.navigate(Screen.BookSearch.route)
    }

    fun navigateToConfig(navController: NavHostController) {
        navController.navigate(Screen.Config.route)
    }

    fun navigateToAbout(navController: NavHostController) {
        navController.navigate(Screen.About.route)
    }

    fun navigateToBookRead(context: Context, bookUrl: String) {
        val intent = Intent(context, ReadBookActivity::class.java).apply {
            putExtra("bookUrl", bookUrl)
        }
        context.startActivity(intent)
    }

    fun navigateToBookInfo(context: Context, bookUrl: String) {
        val intent = Intent(context, BookInfoComposeActivity::class.java).apply {
            putExtra("bookUrl", bookUrl)
        }
        context.startActivity(intent)
    }

    fun navigateToBookSearch(context: Context) {
        val intent = Intent(context, SearchComposeActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToConfig(context: Context) {
        val intent = Intent(context, ConfigComposeActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToAbout(context: Context) {
        val intent = Intent(context, AboutActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToBookshelfManage(context: Context) {
        val intent = Intent(context, BookshelfManageActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToToc(context: Context, bookUrl: String) {
        val intent = Intent(context, TocComposeActivity::class.java).apply {
            putExtra("bookUrl", bookUrl)
        }
        context.startActivity(intent)
    }

    fun navigateToBookmark(context: Context) {
        val intent = Intent(context, AllBookmarkActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToCache(context: Context) {
        val intent = Intent(context, CacheActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToFileManage(context: Context) {
        val intent = Intent(context, FileManageComposeActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToCodeEdit(context: Context) {
        val intent = Intent(context, CodeEditActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToAudioPlay(context: Context) {
        val intent = Intent(context, AudioPlayActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToVideoPlayer(context: Context) {
        val intent = Intent(context, VideoPlayerActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToBookSourceManage(context: Context) {
        val intent = Intent(context, BookSourceActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToRssSourceManage(context: Context) {
        val intent = Intent(context, RssSourceComposeActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToReplaceRuleManage(context: Context) {
        val intent = Intent(context, ReplaceRuleComposeActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToTxtTocRule(context: Context) {
        val intent = Intent(context, TxtTocRuleComposeActivity::class.java)
        context.startActivity(intent)
    }

    fun navigateToWebView(context: Context, url: String, title: String = "") {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra("url", url)
            putExtra("title", title)
        }
        context.startActivity(intent)
    }
}
