package com.smartvault.app.data.local.converters

import androidx.room.TypeConverter
import com.smartvault.app.data.local.entity.ChecklistItem
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        if (value == null) return "{}"
        val obj = JSONObject()
        value.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        val obj = JSONObject(value)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.getString(key)
        }
        return map
    }

    @TypeConverter
    fun fromChecklist(value: List<ChecklistItem>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("isCompleted", item.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toChecklist(value: String?): List<ChecklistItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<ChecklistItem>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ChecklistItem(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    isCompleted = obj.getBoolean("isCompleted")
                )
            )
        }
        return list
    }
}
