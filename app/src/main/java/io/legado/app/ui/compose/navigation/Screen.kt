package io.legado.app.ui.compose.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Bookshelf : Screen("bookshelf")
    object Explore : Screen("explore")
    object Rss : Screen("rss")
    object MyConfig : Screen("my_config")
    object BookDetail : Screen("book_detail/{bookUrl}") {
        fun createRoute(bookUrl: String) = "book_detail/$bookUrl"
    }
    object BookRead : Screen("book_read/{bookUrl}") {
        fun createRoute(bookUrl: String) = "book_read/$bookUrl"
    }
    object BookSearch : Screen("book_search")
    object BookSourceManage : Screen("book_source_manage")
    object RssSourceManage : Screen("rss_source_manage")
    object ReplaceRuleManage : Screen("replace_rule_manage")
    object Config : Screen("config")
    object About : Screen("about")
    object BookshelfManage : Screen("bookshelf_manage")
    object Toc : Screen("toc/{bookUrl}") {
        fun createRoute(bookUrl: String) = "toc/$bookUrl"
    }
    object Bookmark : Screen("bookmark")
    object Cache : Screen("cache")
    object FileManage : Screen("file_manage")
    object CodeEdit : Screen("code_edit")
    object AudioPlay : Screen("audio_play")
    object VideoPlayer : Screen("video_player")
    object Welcome : Screen("welcome")
    object BookInfoEdit : Screen("book_info_edit/{bookUrl}") {
        fun createRoute(bookUrl: String) = "book_info_edit/$bookUrl"
    }
    object BookSourceEdit : Screen("book_source_edit/{sourceUrl}") {
        fun createRoute(sourceUrl: String) = "book_source_edit/$sourceUrl"
    }
    object BookSourceDebug : Screen("book_source_debug/{sourceUrl}") {
        fun createRoute(sourceUrl: String) = "book_source_debug/$sourceUrl"
    }
    object SearchContent : Screen("search_content/{bookUrl}") {
        fun createRoute(bookUrl: String) = "search_content/$bookUrl"
    }
    object ReadManga : Screen("read_manga/{bookUrl}") {
        fun createRoute(bookUrl: String) = "read_manga/$bookUrl"
    }
    object TxtTocRule : Screen("txt_toc_rule")
    object Donate : Screen("donate")
    object WebView : Screen("web_view/{url}") {
        fun createRoute(url: String) = "web_view/$url"
    }
}
