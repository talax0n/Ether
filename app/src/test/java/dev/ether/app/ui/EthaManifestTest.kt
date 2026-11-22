package dev.ether.app.ui

import dev.ether.app.ui.components.ETHA_CELL
import dev.ether.app.ui.components.EthaMove
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** The player hard-codes frame data; this keeps it equal to what sprite-gen wrote. */
class EthaManifestTest {
    private val manifest: JsonObject = Json.parseToJsonElement(
        listOf("../docs/brand/etha/manifest.json", "docs/brand/etha/manifest.json").map(::File).first { it.exists() }.readText(),
    ).jsonObject

    @Test
    fun `every move matches its manifest row`() {
        val animation = manifest.getValue("animation").jsonObject
        assertEquals(ETHA_CELL, animation.getValue("cellWidth").jsonPrimitive.int)
        val rows = animation.getValue("rows").jsonObject
        assertEquals(EthaMove.entries.map { it.manifestKey }.toSet(), rows.keys)
        EthaMove.entries.forEach { move ->
            val row = rows.getValue(move.manifestKey).jsonObject
            val key = move.manifestKey
            assertEquals("$key row", move.row, row.getValue("row").jsonPrimitive.int)
            assertEquals("$key frames", move.frames, row.getValue("frames").jsonPrimitive.int)
            assertEquals("$key loop", move.loop, row.getValue("loop").jsonPrimitive.boolean)
            // The player steps at one fixed duration per move.
            val durations = row.getValue("durations_ms").jsonArray.map { it.jsonPrimitive.int.toLong() }.toSet()
            assertEquals("$key frame duration", setOf(move.frameMs), durations)
        }
    }
}
