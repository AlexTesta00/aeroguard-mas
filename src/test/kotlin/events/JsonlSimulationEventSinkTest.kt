package events

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class JsonlSimulationEventSinkTest {
    @Test
    fun `writes one JSON object per line`() {
        val output = Files.createTempFile("aeroguard-events", ".jsonl")

        JsonlSimulationEventSink(output).use { sink ->
            sink.emit(
                AircraftStateEvent(
                    tick = 0,
                    aircraft = "AZA123",
                    x = 0.0,
                    y = 0.0,
                    altitude = 30000,
                    speed = 1.0,
                    status = "normal",
                    priority = "NORMAL",
                ),
            )
            sink.emit(
                ExplanationEvent(
                    tick = 1,
                    agent = "explanation_agent",
                    message = "Test explanation.",
                ),
            )
        }

        val lines = Files.readAllLines(output)

        assertEquals(2, lines.size)
        assertTrue(lines.all { it.isNotBlank() })

        val first = Json.parseToJsonElement(lines.first()).jsonObject
        val second = Json.parseToJsonElement(lines.last()).jsonObject

        assertEquals("aircraft_state", first["type"]?.jsonPrimitive?.content)
        assertEquals("explanation", second["type"]?.jsonPrimitive?.content)
    }
}
