package events

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulationEventSerializationTest {
    @Test
    fun `serializes aircraft state event to JSON`() {
        val event =
            AircraftStateEvent(
                tick = 0,
                aircraft = "AZA123",
                x = 0.0,
                y = 0.0,
                altitude = 30000,
                speed = 1.0,
                status = "normal",
                priority = "NORMAL",
            )

        val json = Json.parseToJsonElement(event.toJsonLine()).jsonObject

        assertEquals("aircraft_state", json["type"]?.jsonPrimitive?.content)
        assertEquals("AZA123", json["aircraft"]?.jsonPrimitive?.content)
        assertEquals("30000", json["altitude"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes conflict detected event with aircraft list`() {
        val event =
            ConflictDetectedEvent(
                tick = 3,
                aircraft = listOf("AZA123", "DLH456"),
                severity = "high",
                horizontalDistance = 3.2,
                verticalDistance = 0,
                predictedAtTick = 5,
            )

        val json = Json.parseToJsonElement(event.toJsonLine()).jsonObject

        assertEquals("conflict_detected", json["type"]?.jsonPrimitive?.content)
        assertEquals(2, json["aircraft"]?.jsonArray?.size)
        assertEquals("5", json["predictedAtTick"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes plan generated event with actions`() {
        val event =
            PlanGeneratedEvent(
                tick = 4,
                planner = "strips",
                aircraft = "DLH456",
                actions = listOf("climb(DLH456,32000)"),
            )

        val json = Json.parseToJsonElement(event.toJsonLine()).jsonObject

        assertEquals("plan_generated", json["type"]?.jsonPrimitive?.content)
        assertEquals("strips", json["planner"]?.jsonPrimitive?.content)
        assertTrue(json["actions"]?.jsonArray?.isNotEmpty() == true)
    }

    @Test
    fun `all event JSON lines are valid JSON objects`() {
        val events =
            listOf(
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
                BeliefUpdatedEvent(
                    tick = 1,
                    agent = "sector_controller",
                    belief = "conflict_resolved(AZA123,DLH456)",
                ),
                ExplanationEvent(
                    tick = 1,
                    agent = "explanation_agent",
                    message = "DLH456 climbed because AZA123 had higher priority.",
                ),
            )

        events.forEach { event ->
            val parsed = Json.parseToJsonElement(event.toJsonLine())
            assertTrue(parsed is JsonObject)
        }
    }

    @Test
    fun `serializes weather zone activated event`() {
        val event =
            WeatherZoneActivatedEvent(
                tick = 3,
                zone = "WX1",
                x = 5.0,
                y = 5.0,
                radius = 2.5,
            )

        val json = Json.parseToJsonElement(event.toJsonLine()).jsonObject

        assertEquals("weather_zone_activated", json["type"]?.jsonPrimitive?.content)
        assertEquals("WX1", json["zone"]?.jsonPrimitive?.content)
    }

    @Test
    fun `serializes replanning triggered event`() {
        val event =
            ReplanningTriggeredEvent(
                tick = 3,
                aircraft = "RYR700",
                reason = "Weather zone WX1 blocks the nominal route",
            )

        val json = Json.parseToJsonElement(event.toJsonLine()).jsonObject

        assertEquals("replanning_triggered", json["type"]?.jsonPrimitive?.content)
        assertEquals("RYR700", json["aircraft"]?.jsonPrimitive?.content)
    }
}
