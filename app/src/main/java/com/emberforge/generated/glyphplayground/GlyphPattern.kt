package com.emberforge.generated.glyphplayground

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GlyphPattern(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val activeLeds: Set<Int>,
    val ledBrightness: Map<Int, Int> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun brightnessOf(idx: Int): Int = ledBrightness.getOrElse(idx) {
        if (idx in activeLeds) GlyphController.MAX_BRIGHTNESS else 0
    }

    val hasGrayscale: Boolean get() = ledBrightness.isNotEmpty()
}

class PatternRepository(context: Context) {

    private val prefs = context.getSharedPreferences("glyph_patterns", Context.MODE_PRIVATE)

    fun save(pattern: GlyphPattern) {
        val json = JSONObject().apply {
            put("id", pattern.id)
            put("name", pattern.name)
            put("leds", JSONArray(pattern.activeLeds.toList()))
            if (pattern.ledBrightness.isNotEmpty()) {
                put("brightness", JSONObject().apply {
                    pattern.ledBrightness.forEach { (idx, b) -> put(idx.toString(), b) }
                })
            }
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
        parseJson(prefs.getString("p_$id", "")!!)
    } catch (_: Exception) {
        null
    }

    private fun allIds(): Set<String> = prefs.getStringSet("ids", emptySet()) ?: emptySet()

    companion object {
        fun toExportJson(pattern: GlyphPattern): String {
            return JSONObject().apply {
                put("version", 1)
                put("name", pattern.name)
                put("gridSize", GlyphLayout.GRID_SIZE)
                put("leds", JSONArray(pattern.activeLeds.sorted().toList()))
                if (pattern.ledBrightness.isNotEmpty()) {
                    put("brightness", JSONObject().apply {
                        pattern.ledBrightness.entries.sortedBy { it.key }.forEach { (idx, b) ->
                            put(idx.toString(), b)
                        }
                    })
                }
            }.toString(2)
        }

        fun fromImportJson(jsonString: String): GlyphPattern? = try {
            val obj = JSONObject(jsonString)
            val leds = mutableSetOf<Int>()
            val arr = obj.getJSONArray("leds")
            for (i in 0 until arr.length()) {
                val idx = arr.getInt(i)
                if (GlyphLayout.isInsideCircle(idx)) leds.add(idx)
            }
            val brightness = parseBrightnessJson(obj)
            GlyphPattern(
                name = obj.getString("name"),
                activeLeds = leds,
                ledBrightness = brightness
            )
        } catch (_: Exception) {
            null
        }

        private fun parseJson(raw: String): GlyphPattern {
            val obj = JSONObject(raw)
            val leds = mutableSetOf<Int>()
            val arr = obj.getJSONArray("leds")
            for (i in 0 until arr.length()) leds.add(arr.getInt(i))
            val brightness = parseBrightnessJson(obj)
            return GlyphPattern(obj.getString("id"), obj.getString("name"), leds, brightness, obj.getLong("createdAt"))
        }

        private fun parseBrightnessJson(obj: JSONObject): Map<Int, Int> {
            val bObj = obj.optJSONObject("brightness") ?: return emptyMap()
            val map = mutableMapOf<Int, Int>()
            for (key in bObj.keys()) {
                val idx = key.toIntOrNull() ?: continue
                if (GlyphLayout.isInsideCircle(idx)) {
                    map[idx] = bObj.getInt(key).coerceIn(0, GlyphController.MAX_BRIGHTNESS)
                }
            }
            return map
        }
    }
}
