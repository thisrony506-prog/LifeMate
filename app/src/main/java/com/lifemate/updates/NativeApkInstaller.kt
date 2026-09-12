package com.lifemate.updates

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Call only after re-verifying the private APK. Android owns permission and final installation confirmation. */
@Suppress("DEPRECATION")
fun nativeUpdateIntent(context: Context, apk: File): Intent {
    require(apk.parentFile?.canonicalFile == File(context.cacheDir,"updates").canonicalFile) { "Update must be in private update storage." }
    require(apk.isFile && Regex("LifeMate-[0-9]+\\.apk").matches(apk.name)) { "A complete verified update is required." }
    val uri = FileProvider.getUriForFile(context,"${context.packageName}.files",apk)
    return Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("LifeMate update",uri) }
}
