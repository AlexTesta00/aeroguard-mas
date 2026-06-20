package planning

import domain.Conflict
import domain.ConflictType
import domain.Maneuver
import domain.ManeuverType
import domain.SimulationState
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reasoning.SafetyReasoner
import reasoning.TuPrologSafetyReasoner
import java.nio.file.Path

class StripsResolutionPlannerTest {
    private val loader = JsonScenarioLoader()

    @Test
    fun `generates resolution plan for simple conflict`() {
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

        val planner =
            StripsResolutionPlanner(
                safetyReasoner = TuPrologSafetyReasoner.fromClasspath(),
            )

        val plan = planner.planResolution(conflict, state)

        assertNotNull(plan)
        assertEquals("C1", plan!!.conflictId)
        assertTrue(plan.maneuvers.isNotEmpty())
        assertTrue(plan.explanation.contains("STRIPS"))
    }

    @Test
    fun `emergency priority scenario maneuvers the lower priority aircraft`() {
        val scenario = loader.load(Path.of("scenarios/emergency_priority.json"))
        val state = SimulationState.fromScenario(scenario)

        val conflict =
            Conflict(
                id = "C-EMERGENCY",
                tick = 0,
                aircraftIds = setOf("ITY900", "KLM321"),
                type = ConflictType.PREDICTED_CONFLICT,
                horizontalDistance = 3.0,
                verticalDistanceFeet = 0,
                predictedAtTick = 4,
            )

        val planner =
            StripsResolutionPlanner(
                safetyReasoner = TuPrologSafetyReasoner.fromClasspath(),
            )

        val plan = planner.planResolution(conflict, state)

        assertNotNull(plan)
        val maneuver = plan!!.maneuvers.single()

        assertEquals("KLM321", maneuver.aircraftId)
        assertEquals(ManeuverType.CLIMB, maneuver.type)
        assertEquals(32000, maneuver.targetFlightLevel?.feet)
    }

    @Test
    fun `returns null when reasoner rejects all candidate maneuvers`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val conflict =
            Conflict(
                id = "C-REJECTED",
                tick = 0,
                aircraftIds = setOf("AZA123", "DLH456"),
                type = ConflictType.PREDICTED_CONFLICT,
                horizontalDistance = 3.0,
                verticalDistanceFeet = 0,
                predictedAtTick = 4,
            )

        val planner =
            StripsResolutionPlanner(
                safetyReasoner = RejectingReasoner,
            )

        assertNull(planner.planResolution(conflict, state))
    }

    private object RejectingReasoner : SafetyReasoner {
        override fun isConflictUnsafe(
            conflict: Conflict,
            state: SimulationState,
        ): Boolean = true

        override fun isManeuverAllowed(
            aircraftId: String,
            maneuver: Maneuver,
            state: SimulationState,
        ): Boolean = false

        override fun priorityOf(
            aircraftId: String,
            state: SimulationState,
        ): Int =
            state.aircraft
                .getValue(aircraftId)
                .priority.score

        override fun explainDecision(decisionId: String): List<String> =
            listOf("All maneuvers rejected by test reasoner.")
    }
}
