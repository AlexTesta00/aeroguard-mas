package explanation

import domain.Conflict
import domain.ConflictType
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.ResolutionPlan
import domain.SimulationState
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reasoning.TuPrologSafetyReasoner
import simulation.SimulationEngine
import java.nio.file.Path

class ExplanationServiceTest {
    private val loader = JsonScenarioLoader()
    private val service =
        ExplanationService(
            safetyReasoner = TuPrologSafetyReasoner.fromClasspath(),
        )

    @Test
    fun `generates explanation for STRIPS resolution plan`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val conflict =
            Conflict(
                id = "C1",
                tick = 0,
                aircraftIds = setOf("AZA123", "DLH456"),
                type = ConflictType.PREDICTED_CONFLICT,
                horizontalDistance = 3.0,
                verticalDistanceFeet = 0,
                predictedAtTick = 4,
            )

        val plan =
            ResolutionPlan(
                id = "strips-C1",
                conflictId = "C1",
                maneuvers =
                    listOf(
                        Maneuver(
                            aircraftId = "DLH456",
                            type = ManeuverType.CLIMB,
                            targetFlightLevel = FlightLevel(32000),
                            reason = "Increase vertical separation",
                        ),
                    ),
                explanation = "STRIPS selected climb(DLH456,32000).",
            )

        val explanations =
            service.explainResolution(
                conflict = conflict,
                state = state,
                plan = plan,
            )

        assertTrue(explanations.isNotEmpty())
        assertTrue(explanations.any { it.message.contains("STRIPS") })
        assertTrue(explanations.any { it.agent == "explanation_agent" })
    }

    @Test
    fun `generates no conflict baseline explanation`() {
        val scenario = loader.load(Path.of("scenarios/no_conflict.json"))
        val result = SimulationEngine().run(scenario)

        val explanations =
            service.explainRun(
                runResult = result,
                resolutionPlan = null,
            )

        assertTrue(explanations.any { it.message.contains("No conflicts", ignoreCase = true) })
    }

    @Test
    fun `generates explanation for weather replanning`() {
        val scenario = loader.load(Path.of("scenarios/weather_replanning.json"))
        val reasoner = TuPrologSafetyReasoner.fromClasspath()
        val runResult =
            SimulationEngine(
                conflictDetector = simulation.ConflictDetector(safetyReasoner = reasoner),
                predictionHorizonTicks = 6,
            ).run(scenario)

        val decision =
            replanning
                .WeatherReplanningService(reasoner)
                .planWeatherReplanning(scenario, runResult)
                .single()

        val explanations =
            ExplanationService(reasoner)
                .explainWeatherReplanning(decision)

        assertTrue(explanations.isNotEmpty())
        assertTrue(explanations.any { it.message.contains("weather", ignoreCase = true) })
        assertTrue(explanations.any { it.agent == "explanation_agent" })
    }
}
