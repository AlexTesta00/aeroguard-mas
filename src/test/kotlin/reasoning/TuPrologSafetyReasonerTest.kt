package reasoning

import domain.AircraftPriority
import domain.Conflict
import domain.ConflictType
import domain.FlightLevel
import domain.Maneuver
import domain.ManeuverType
import domain.SimulationState
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TuPrologSafetyReasonerTest {
    private val reasoner = TuPrologSafetyReasoner.fromClasspath()
    private val loader = JsonScenarioLoader()

    @Test
    fun `marks conflict unsafe when horizontal and vertical separation are below thresholds`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val conflict =
            Conflict(
                id = "test-conflict",
                tick = 4,
                aircraftIds = setOf("AZA123", "DLH456"),
                type = ConflictType.HORIZONTAL_SEPARATION_LOSS,
                horizontalDistance = 3.0,
                verticalDistanceFeet = 0,
            )

        assertTrue(reasoner.isConflictUnsafe(conflict, state))
    }

    @Test
    fun `does not mark conflict unsafe when vertical separation is sufficient`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val conflict =
            Conflict(
                id = "vertical-safe",
                tick = 4,
                aircraftIds = setOf("AZA123", "DLH456"),
                type = ConflictType.HORIZONTAL_SEPARATION_LOSS,
                horizontalDistance = 3.0,
                verticalDistanceFeet = 2000,
            )

        assertFalse(reasoner.isConflictUnsafe(conflict, state))
    }

    @Test
    fun `computes low fuel aircraft priority from Prolog rules`() {
        val scenario = loader.load(Path.of("scenarios/emergency_priority.json"))
        val state = SimulationState.fromScenario(scenario)

        assertEquals(90, reasoner.priorityOf("ITY900", state))
        assertEquals(10, reasoner.priorityOf("KLM321", state))
    }

    @Test
    fun `allows climb maneuver when altitude change is valid and inside sector`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val maneuver =
            Maneuver(
                aircraftId = "DLH456",
                type = ManeuverType.CLIMB,
                targetFlightLevel = FlightLevel(32000),
                reason = "Create vertical separation",
            )

        assertTrue(reasoner.isManeuverAllowed("DLH456", maneuver, state))
    }

    @Test
    fun `rejects climb maneuver when altitude change is too large`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val maneuver =
            Maneuver(
                aircraftId = "DLH456",
                type = ManeuverType.CLIMB,
                targetFlightLevel = FlightLevel(35000),
                reason = "Too large altitude change",
            )

        assertFalse(reasoner.isManeuverAllowed("DLH456", maneuver, state))
    }

    @Test
    fun `rejects maneuver for unknown aircraft`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val maneuver =
            Maneuver(
                aircraftId = "UNKNOWN",
                type = ManeuverType.SLOW_DOWN,
                reason = "Invalid aircraft",
            )

        assertFalse(reasoner.isManeuverAllowed("UNKNOWN", maneuver, state))
    }

    @Test
    fun `returns symbolic explanations from Prolog facts`() {
        val explanations = reasoner.explainDecision("unsafe_pair")

        assertTrue(explanations.isNotEmpty())
        assertTrue(
            explanations.any { it.contains("horizontal", ignoreCase = true) },
            "Expected an explanation mentioning horizontal separation.",
        )
    }

    @Test
    fun `returns fallback explanation for unknown decision`() {
        val explanations = reasoner.explainDecision("unknown_decision")

        assertEquals(
            listOf("No symbolic explanation found for decision 'unknown_decision'."),
            explanations,
        )
    }

    @Test
    fun `keeps normal priority from aircraft priority enum`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        assertEquals(AircraftPriority.NORMAL.score, reasoner.priorityOf("AZA123", state))
    }
}
