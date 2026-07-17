package com.emberforge.generated.glyphplayground

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GlyphPattern(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val activeLeds: Set<Int>,
    val brightness: Int = GlyphController.MAX_BRIGHTNESS,
    val createdAt: Long = System.currentTimeMillis()
)

class PatternRepository(context: Context) {

    private val prefs = context.getSharedPreferences("glyph_patterns", Context.MODE_PRIVATE)

    fun save(pattern: GlyphPattern) {
        val json = JSONObject().apply {
            put("id", pattern.id)
            put("name", pattern.name)
            put("leds", JSONArray(pattern.activeLeds.toList()))
            put("brightness", pattern.brightness)
            put("createdAt", pattern.createdAt)
        }
        val ids = allIds().toMutableSet().apply { add(pattern.id) }
        prefs.edit()
            .putString("p_${pattern.id}", json.toString())
            .putStringSet("ids", ids)
            .apply()
    }

    fun loadAll(): List<GlyphPattern> =
        allIds().mapNotNull(::load).sortedByDescending { it.createdAt }

    fun delete(id: String) {
        val ids = allIds().toMutableSet().apply { remove(id) }
        prefs.edit()
            .remove("p_$id")
            .putStringSet("ids", ids)
            .apply()
    }

    private fun load(id: String): GlyphPattern? = try {
        val obj = JSONObject(prefs.getString("p_$id", "")!!)
        val leds = mutableSetOf<Int>()
        val arr = obj.getJSONArray("leds")
        for (i in 0 until arr.length()) leds.add(arr.getInt(i))
        val brightness = obj.optInt("brightness", GlyphController.MAX_BRIGHTNESS)
        GlyphPattern(obj.getString("id"), obj.getString("name"), leds, brightness, obj.getLong("createdAt"))
    } catch (_: Exception) {
        null
    }

    private fun allIds(): Set<String> = prefs.getStringSet("ids", emptySet()) ?: emptySet()
}
