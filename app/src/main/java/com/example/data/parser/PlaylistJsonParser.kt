package com.example.data.parser

import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelGroupEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ParsedPlaylist(
    val channels: List<ChannelEntity>,
    val groups: List<ChannelGroupEntity>,
)

object PlaylistJsonParser {

    fun parse(jsonText: String, isCustomImport: Boolean = false): ParsedPlaylist {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) return ParsedPlaylist(emptyList(), emptyList())

        val channels = mutableListOf<ChannelEntity>()
        val groupsMap = linkedMapOf<String, ChannelGroupEntity>()

        if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseChannelObject(obj, fallbackId = null, isCustom = isCustomImport)?.let {
                    channels.add(it)
                }
            }
        } else {
            val root = JSONObject(trimmed)

            // Parse channelGroups if present
            val groupsObj = root.optJSONObject("channelGroups")
            if (groupsObj != null) {
                val keys = groupsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val gObj = groupsObj.optJSONObject(key) ?: continue
                    val name = gObj.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        groupsMap[name] = ChannelGroupEntity(
                            name = name,
                            orderIndex = gObj.optInt("order", 999),
                            type = gObj.optString("type", "live"),
                        )
                    }
                }
            }

            // Also respect appConfig.groupOrder / groupOrderSport if channelGroups was absent
            val appConfig = root.optJSONObject("appConfig")
            if (appConfig != null && groupsMap.isEmpty()) {
                val orderList = mutableListOf<String>()
                appConfig.optJSONArray("groupOrderSport")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf { it.isNotBlank() }?.let { orderList.add(it) }
                    }
                }
                appConfig.optJSONArray("groupOrder")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf { it.isNotBlank() }?.let { orderList.add(it) }
                    }
                }
                orderList.forEachIndexed { idx, groupName ->
                    groupsMap[groupName] = ChannelGroupEntity(name = groupName, orderIndex = idx, type = "live")
                }
            }

            // Parse managedPlaylist object (kobani_4k.json format)
            val managedPlaylist = root.optJSONObject("managedPlaylist")
            if (managedPlaylist != null) {
                val keys = managedPlaylist.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val chObj = managedPlaylist.optJSONObject(key) ?: continue
                    parseChannelObject(chObj, fallbackId = key, isCustom = isCustomImport)?.let {
                        channels.add(it)
                    }
                }
            } else {
                // Fallback for generic { "channels": [...] } JSON
                val arr = root.optJSONArray("channels")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseChannelObject(obj, fallbackId = null, isCustom = isCustomImport)?.let {
                            channels.add(it)
                        }
                    }
                }
            }
        }

        // Ensure every channel group present in channels exists in groupsMap
        var nextFallbackOrder = (groupsMap.values.maxOfOrNull { it.orderIndex } ?: 0) + 1
        channels.forEach { ch ->
            if (ch.groupName.isNotBlank() && !groupsMap.containsKey(ch.groupName)) {
                groupsMap[ch.groupName] = ChannelGroupEntity(
                    name = ch.groupName,
                    orderIndex = nextFallbackOrder++,
                    type = ch.type.ifBlank { "live" },
                )
            }
        }

        val sortedChannels = channels.sortedWith(compareBy({ it.orderIndex }, { it.name.lowercase() }))
        val sortedGroups = groupsMap.values.sortedWith(compareBy({ it.orderIndex }, { it.name.lowercase() }))
        return ParsedPlaylist(sortedChannels, sortedGroups)
    }

    private fun parseChannelObject(
        obj: JSONObject,
        fallbackId: String?,
        isCustom: Boolean,
    ): ChannelEntity? {
        val rawUrl = obj.optString("url", "").trim()
        val rawUrl2 = obj.optString("url2", "").trim()
        val rawUrl3 = obj.optString("url3", "").trim()

        // Automatically ignore channels with all empty stream URLs
        if (rawUrl.isEmpty() && rawUrl2.isEmpty() && rawUrl3.isEmpty()) {
            return null
        }

        val name = obj.optString("name", "").trim()
        if (name.isEmpty()) return null

        val id = obj.optString("id", "").trim().ifEmpty {
            fallbackId ?: UUID.randomUUID().toString()
        }
        val group = obj.optString("group", "").trim().ifEmpty { "GENERAL" }

        return ChannelEntity(
            id = id,
            name = name,
            groupName = group,
            logo = obj.optString("logo", "").trim(),
            backdrop = obj.optString("backdrop", "").trim(),
            url = rawUrl,
            url2 = rawUrl2,
            url3 = rawUrl3,
            referer = obj.optString("referer", "").trim(),
            userAgent = obj.optString("userAgent", "").trim(),
            url2Name = obj.optString("url2Name", "").trim(),
            url2Referer = obj.optString("url2Referer", "").trim(),
            url2UserAgent = obj.optString("url2UserAgent", "").trim(),
            url3Name = obj.optString("url3Name", "").trim(),
            url3Referer = obj.optString("url3Referer", "").trim(),
            url3UserAgent = obj.optString("url3UserAgent", "").trim(),
            drmScheme = obj.optString("drmScheme", "").trim(),
            drmLicense = obj.optString("drmLicense", "").trim(),
            url2DrmScheme = obj.optString("url2DrmScheme", "").trim(),
            url2DrmLicense = obj.optString("url2DrmLicense", "").trim(),
            url3DrmScheme = obj.optString("url3DrmScheme", "").trim(),
            url3DrmLicense = obj.optString("url3DrmLicense", "").trim(),
            type = obj.optString("type", "live").trim().ifEmpty { "live" },
            featured = obj.optBoolean("featured", false),
            featuredOrder = obj.optInt("featured_order", 0),
            orderIndex = obj.optInt("order", 0),
            enabled = obj.optBoolean("enabled", true),
            isCustom = obj.optBoolean("isCustom", isCustom),
        )
    }

    fun exportToJson(
        channels: List<ChannelEntity>,
        groups: List<ChannelGroupEntity>,
    ): String {
        val root = JSONObject()
        root.put("opticTvBackupVersion", 1)
        root.put("app", "D09 TV")

        val playlistObj = JSONObject()
        channels.forEach { ch ->
            val obj = JSONObject()
            obj.put("id", ch.id)
            obj.put("name", ch.name)
            obj.put("group", ch.groupName)
            obj.put("logo", ch.logo)
            obj.put("backdrop", ch.backdrop)
            obj.put("url", ch.url)
            obj.put("url2", ch.url2)
            obj.put("url3", ch.url3)
            obj.put("referer", ch.referer)
            obj.put("userAgent", ch.userAgent)
            obj.put("url2Name", ch.url2Name)
            obj.put("url2Referer", ch.url2Referer)
            obj.put("url2UserAgent", ch.url2UserAgent)
            obj.put("url3Name", ch.url3Name)
            obj.put("url3Referer", ch.url3Referer)
            obj.put("url3UserAgent", ch.url3UserAgent)
            obj.put("drmScheme", ch.drmScheme)
            obj.put("drmLicense", ch.drmLicense)
            obj.put("url2DrmScheme", ch.url2DrmScheme)
            obj.put("url2DrmLicense", ch.url2DrmLicense)
            obj.put("url3DrmScheme", ch.url3DrmScheme)
            obj.put("url3DrmLicense", ch.url3DrmLicense)
            obj.put("type", ch.type)
            obj.put("featured", ch.featured)
            obj.put("featured_order", ch.featuredOrder)
            obj.put("order", ch.orderIndex)
            obj.put("enabled", ch.enabled)
            obj.put("isCustom", ch.isCustom)
            playlistObj.put(ch.id, obj)
        }
        root.put("managedPlaylist", playlistObj)

        val groupsObj = JSONObject()
        groups.forEach { g ->
            val gObj = JSONObject()
            gObj.put("id", g.name)
            gObj.put("name", g.name)
            gObj.put("order", g.orderIndex)
            gObj.put("type", g.type)
            groupsObj.put(g.name, gObj)
        }
        root.put("channelGroups", groupsObj)

        return root.toString(2)
    }
}
