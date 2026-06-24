package planning

import domain.ManeuverType
import domain.SimulationState
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reasoning.TuPrologSafetyReasoner
import simulation.ConflictDetector
import simulation.ManagedSimulationEngine
import java.nio.file.Path

class SecondaryConflictAwareResolutionPlannerTest {
    private val loader = JsonScenarioLoader()
    private val reasoner = TuPrologSafetyReasoner.fromClasspath()

    @Test
    fun `rejects climb when it creates a secondary conflict`() {
        val scenario = loader.load(Path.of("scenarios/secondary_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val detector = ConflictDetector(safetyReasoner = reasoner)
        val primaryConflict =
            detector
                .predictConflicts(
                    state = state,
                    horizonTicks = 8,
                ).first { conflict ->
                    conflict.aircraftIds.contains("IBE222") &&
                        conflict.aircraftIds.contains("SAS111")
                }

        val planner =
            SecondaryConflictAwareResolutionPlanner(
                safetyReasoner = reasoner,
                predictionHorizonTicks = 8,
            )

        val plan =
            planner.planResolution(
                conflict = primaryConflict,
                state = state,
            )

        assertNotNull(plan)

        val maneuver = plan!!.maneuvers.single()

        assertEquals("SAS111", maneuver.aircraftId)
        assertEquals(ManeuverType.DESCEND, maneuver.type)
        assertEquals(28000, maneuver.targetFlightLevel?.feet)
        assertTrue(
            plan.explanation.contains("Rejected alternatives", ignoreCase = true),
            "The explanation should mention that unsafe alternatives were rejected.",
        )
    }

    @Test
    fun `managed secondary conflict scenario has no EZY333 SAS111 conflict after prevention`() {
        val scenario = loader.load(Path.of("scenarios/secondary_conflict.json"))

        val result =
            ManagedSimulationEngine(
                safetyReasoner = reasoner,
                predictionHorizonTicks = 8,
            ).run(scenario)

        val plan = result.conflictResolutionPlan

        assertNotNull(plan)

        val maneuver = plan!!.maneuvers.single()

        assertEquals("SAS111", maneuver.aircraftId)
        assertEquals(ManeuverType.DESCEND, maneuver.type)
        assertEquals(28000, maneuver.targetFlightLevel?.feet)

        val secondaryConflicts =
            result.runResult.currentConflicts.filter { conflict ->
                conflict.aircraftIds.contains("EZY333") &&
                    conflict.aircraftIds.contains("SAS111")
            }

        assertTrue(
            secondaryConflicts.isEmpty(),
            "SAS111 must not climb into EZY333 flight level.",
        )
    }
}
