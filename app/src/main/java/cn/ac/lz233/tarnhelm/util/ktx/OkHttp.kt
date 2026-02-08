package cn.ac.lz233.tarnhelm.util.ktx

import cn.ac.lz233.tarnhelm.logic.Network
import cn.ac.lz233.tarnhelm.logic.dao.SettingsDao
import cn.ac.lz233.tarnhelm.util.LogUtil
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.internal.commonToString
import org.json.JSONObject
import java.io.IOException

fun HttpUrl.followRedirect(userAgent: String?): HttpUrl {
    LogUtil._d("followRedirect: rawUA: $userAgent," + " " +
            "defaultUA: " + SettingsDao.defaultUA.toString() + " " +
            "enableDefaultUAWhenRequest: " + SettingsDao.enableDefaultUAWhenRequest.toString())
    val response = Network.okHttpClientNoRedirect
        .newCall(
            Request.Builder()
                .url(this)
                .apply {
                    when {
                        !userAgent.isNullOrEmpty() -> {
                            addHeader("User-Agent", userAgent)
                        }

                        SettingsDao.enableDefaultUAWhenRequest -> {
                            SettingsDao.defaultUA?.let { value ->
                                addHeader("User-Agent", value)
                            }
                        }
                    }
                }
                .build()
        )
        .execute()
    return if (response.isSuccessful) {
        // bilibili returns 404 but still using 200 header
        val body = response.body.string()
        runCatching {
            if (JSONObject(body).get("code").toString().contains("404")) throw IOException(body)
        }.onFailure {
            if (it is IOException) throw it
        }
        this
    } else if (response.isRedirect) {
        (response.header("Location")?.toHttpUrlOrNull() ?: response.header("location")?.toHttpUrlOrNull())
            ?.followRedirect(userAgent) ?: this
    } else {
        throw IOException(response.commonToString())
    }
}