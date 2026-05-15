package io.legado.app.ui.book.toc

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class TocComposeActivityResult : ActivityResultContract<String, Array<Any>?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, TocComposeActivity::class.java)
            .putExtra("bookUrl", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Array<Any>? {
        if (resultCode == RESULT_OK) {
            intent?.let {
                return arrayOf(
                    it.getIntExtra("index", 0),
                    it.getIntExtra("chapterPos", 0),
                    it.getBooleanExtra("chapterChanged", false),
                    it.getIntExtra("durVolumeIndex", 0),
                    it.getIntExtra("chapterInVolumeIndex", 0)
                )
            }
        }
        return null
    }
}
