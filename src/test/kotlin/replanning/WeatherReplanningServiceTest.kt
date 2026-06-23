package replanning

import domain.ManeuverType
import events.PlanGeneratedEvent
import events.ReplanningTriggeredEvent
import events.WeatherZoneActivatedEvent
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reasoning.TuPrologSafetyReasoner
import simulation.ConflictDetector
import simulation.SimulationEngine
import java.nio.file.Path

class WeatherReplanningServiceTest {
    private val loader = JsonScenarioLoader()
    private val reasoner = TuPrologSafetyReasoner.fromClasspath()

    @Test
    fun `generates weather replanning decision when active weather zone intersects route`() {
        val scenario = loader.load(Path.of("scenarios/weather_replanning.json"))
        val runResult =
            SimulationEngine(
                conflictDetector = ConflictDetector(safetyReasoner = reasoner),
                predictionHorizonTicks = 6,
            ).run(scenario)

        val service = WeatherReplanningService(reasoner)

        val decisions =
            service.planWeatherReplanning(
                scenario = scenario,
                runResult = runResult,
            )

        assertEquals(1, decisions.size)

        val decision = decisions.single()

        assertEquals("WX1", decision.zone.id)
        assertEquals("RYR700", decision.aircraftId)
        assertNotNull(decision.resolutionPlan)
        assertTrue(
            decision.resolutionPlan.maneuvers.any { it.type == ManeuverType.AVOID_WEATHER_ZONE },
        )
        assertTrue(
            decision.resolutionPlan.maneuvers.any { it.type == ManeuverType.REROUTE_TO_WAYPOINT },
        )
    }

    @Test
    fun `does not generate weather replanning when scenario has no weather events`() {
        val scenario = loader.load(Path.of("scenarios/no_conflict.json"))
        val runResult =
            SimulationEngine(
                conflictDetector = ConflictDetector(safetyReasoner = reasoner),
                predictionHorizonTicks = 6,
            ).run(scenario)

        val service = WeatherReplanningService(reasoner)

        val decisions =
            service.planWeatherReplanning(
                scenario = scenario,
                runResult = runResult,
            )

        assertTrue(decisions.isEmpty())
    }

    @Test
    fun `weather replanning decision produces replay events`() {
        val scenario = loader.load(Path.of("scenarios/weather_replanning.json"))
        val runResult =
            SimulationEngine(
                conflictDetector = ConflictDetector(safetyReasoner = reasoner),
                predictionHorizonTicks = 6,
            ).run(scenario)

        val service = WeatherReplanningService(reasoner)

        val decision =
            service
                .planWeatherReplanning(
                    scenario = scenario,
                    runResult = runResult,
                ).single()

        val eventTypes = decision.events.map { it::class }

        assertTrue(eventTypes.contains(WeatherZoneActivatedEvent::class))
        assertTrue(eventTypes.contains(ReplanningTriggeredEvent::class))
        assertTrue(decision.events.any { it is PlanGeneratedEvent })
    }
}
