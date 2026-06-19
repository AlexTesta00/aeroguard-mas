package integration

import domain.AircraftPriority
import domain.EmergencyStatus
import domain.FlightLevel
import domain.Position
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class JsonScenarioLoaderTest {
    private val loader = JsonScenarioLoader()

    @Test
    fun `loads simple conflict scenario from JSON`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))

        assertEquals("simple_conflict", scenario.name)
        assertEquals(12, scenario.maxTicks)
        assertEquals(5.0, scenario.separation.horizontal, 0.000001)
        assertEquals(1000, scenario.separation.verticalFeet)
        assertEquals(2, scenario.aircraft.size)

        val aza = scenario.aircraftById("AZA123")
        assertEquals(Position(0.0, 0.0), aza.position)
        assertEquals(FlightLevel(30000), aza.flightLevel)
        assertEquals(1.0, aza.velocity.horizontalUnitsPerTick, 0.000001)
        assertEquals(AircraftPriority.NORMAL, aza.priority)
        assertEquals(EmergencyStatus.NONE, aza.emergencyStatus)
        assertEquals(
            "W1",
            aza.route.waypoints
                .first()
                .name,
        )
    }

    @Test
    fun `loads no conflict baseline scenario`() {
        val scenario = loader.load(Path.of("scenarios/no_conflict.json"))

        assertEquals("no_conflict", scenario.name)
        assertEquals(2, scenario.aircraft.size)
        assertTrue(scenario.weatherZones.isEmpty())
        assertTrue(scenario.dynamicEvents.isEmpty())
    }

    @Test
    fun `loads weather replanning scenario with weather zone and dynamic event`() {
        val scenario = loader.load(Path.of("scenarios/weather_replanning.json"))

        assertEquals("weather_replanning", scenario.name)
        assertEquals(1, scenario.weatherZones.size)
        assertEquals("WX1", scenario.weatherZones.first().id)
        Assertions.assertEquals(1, scenario.dynamicEvents.size)
        Assertions.assertEquals(3, scenario.dynamicEvents.first().tick)
    }

    @Test
    fun `rejects scenario with duplicate aircraft ids`() {
        val invalidJson =
            """
            {
              "name": "duplicate_aircraft",
              "maxTicks": 4,
              "separation": {
                "horizontal": 5.0,
                "vertical": 1000
              },
              "aircraft": [
                {
                  "id": "AZA123",
                  "x": 0,
                  "y": 0,
                  "altitude": 30000,
                  "speed": 1,
                  "priority": "normal",
                  "emergency": false,
                  "route": [{"name": "W1", "x": 5, "y": 5}]
                },
                {
                  "id": "AZA123",
                  "x": 10,
                  "y": 0,
                  "altitude": 30000,
                  "speed": 1,
                  "priority": "normal",
                  "emergency": false,
                  "route": [{"name": "W2", "x": 5, "y": 5}]
                }
              ]
            }
            """.trimIndent()

        val tempFile = Files.createTempFile("aeroguard-invalid-scenario", ".json")
        Files.writeString(tempFile, invalidJson)

        Assertions.assertThrows(ScenarioLoadingException::class.java) {
            loader.load(tempFile)
        }
    }
}
