package com.example.qucidemo

import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object DictApi {

    /**
     * 调用有道免费词典接口（jsonapi_s，无需 key）。
     * 返回：音标字符串 + 释义列表。
     */
    fun lookup(word: String): Pair<String, List<String>> {
        val url = "https://dict.youdao.com/jsonapi_s?q=" + URLEncoder.encode(word, "UTF-8")
        val json = URL(url).readText()
        val root = JSONObject(json)

        val explains = mutableListOf<String>()
        var phone = ""

        if (root.has("ec")) {
            val ec = root.getJSONObject("ec")
            if (ec.has("word")) {
                val w = ec.getJSONArray("word").getJSONObject(0)
                val us = w.optString("usphone")
                val uk = w.optString("ukphone")
                if (us.isNotEmpty() || uk.isNotEmpty()) {
                    phone = "英/$uk  美/$us"
                }
                val trs = w.optJSONArray("trs") ?: return phone to explains
                for (i in 0 until trs.length()) {
                    val trArr = trs.getJSONObject(i).optJSONArray("tr") ?: continue
                    for (j in 0 until trArr.length()) {
                        val l = trArr.getJSONObject(j).optJSONObject("l") ?: continue
                        val iArr = l.optJSONArray("i") ?: continue
                        for (k in 0 until iArr.length()) {
                            explains.add(iArr.getString(k))
                        }
                    }
                }
            }
        }
        return phone to explains
    }
}
