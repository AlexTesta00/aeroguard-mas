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
}
