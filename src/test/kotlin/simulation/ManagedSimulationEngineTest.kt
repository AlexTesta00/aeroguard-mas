package simulation

import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reasoning.TuPrologSafetyReasoner
import java.nio.file.Path

class ManagedSimulationEngineTest {
    @Test
    fun `managed simulation applies generated resolution plan to future states`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/simple_conflict.json"))
        val reasoner = TuPrologSafetyReasoner.fromClasspath()

        val result =
            ManagedSimulationEngine(
                safetyReasoner = reasoner,
                predictionHorizonTicks = 6,
            ).run(scenario)

        val plan = result.conflictResolutionPlan

        assertNotNull(plan)
        assertTrue(plan!!.maneuvers.isNotEmpty())

        val maneuver = plan.maneuvers.first()
        val targetAltitude = maneuver.targetFlightLevel?.feet

        assertNotNull(targetAltitude)

        val postManeuverStates =
            result.runResult.states
                .filter { state -> state.tick >= result.appliedManeuvers.first().tick }

        assertTrue(postManeuverStates.isNotEmpty())
        assertTrue(
            postManeuverStates.all { state ->
                state.aircraft
                    .getValue(maneuver.aircraftId)
                    .flightLevel.feet == targetAltitude
            },
            "The maneuvered aircraft should keep the assigned flight level after the maneuver is applied.",
        )
    }

    @Test
    fun `managed simulation has no current conflicts after applying vertical maneuver`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/simple_conflict.json"))
        val reasoner = TuPrologSafetyReasoner.fromClasspath()

        val result =
            ManagedSimulationEngine(
                safetyReasoner = reasoner,
                predictionHorizonTicks = 6,
            ).run(scenario)

        val applicationTick = result.appliedManeuvers.first().tick

        val conflictsAfterApplication =
            result.runResult.currentConflicts
                .filter { conflict -> conflict.tick >= applicationTick }

        assertTrue(
            conflictsAfterApplication.isEmpty(),
            "After applying the resolution maneuver there should be no current unsafe conflicts.",
        )
    }

    @Test
    fun `managed simulation keeps initial predicted conflict for explainability`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/simple_conflict.json"))
        val reasoner = TuPrologSafetyReasoner.fromClasspath()

        val result =
            ManagedSimulationEngine(
                safetyReasoner = reasoner,
                predictionHorizonTicks = 6,
            ).run(scenario)

        assertTrue(result.runResult.predictedConflicts.isNotEmpty())
        assertEquals(1, result.appliedManeuvers.first().tick)
    }

    @Test
    fun `managed simulation reroutes aircraft around active weather zone`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/weather_replanning.json"))
        val reasoner = TuPrologSafetyReasoner.fromClasspath()

        val result =
            ManagedSimulationEngine(
                safetyReasoner = reasoner,
                predictionHorizonTicks = 6,
            ).run(scenario)

        assertTrue(
            result.weatherReplanningDecisions.isNotEmpty(),
            "Weather replanning should be triggered for weather_replanning scenario.",
        )

        val zone = scenario.weatherZones.first { weatherZone -> weatherZone.id == "WX1" }
        val activationTick = result.weatherReplanningDecisions.first().tick

        val ryrStatesAfterActivation =
            result.runResult.states
                .filter { state -> state.tick >= activationTick }
                .map { state ->
                    val aircraft = state.aircraft.getValue("RYR700")
                    state.tick to aircraft
                }

        val violatingStates =
            ryrStatesAfterActivation.filter { (_, aircraft) ->
                aircraft.position.distanceTo(zone.center) <= zone.radius
            }

        assertTrue(
            violatingStates.isEmpty(),
            buildString {
                appendLine("RYR700 should not enter weather zone ${zone.id} after replanning is applied.")
                appendLine("Violating states:")
                violatingStates.forEach { (tick, aircraft) ->
                    appendLine(
                        "- tick=$tick, position=${aircraft.position}, " +
                            "distance=${aircraft.position.distanceTo(zone.center)}",
                    )
                }
            },
        )
    }
}
