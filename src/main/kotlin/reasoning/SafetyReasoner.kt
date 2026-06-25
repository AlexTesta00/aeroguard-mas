package reasoning

import domain.Conflict
import domain.Maneuver
import domain.SimulationState

/**
 * Symbolic safety reasoning interface.
 *
 * Implementations answer safety, priority, maneuver feasibility, and explanation
 * queries using the current simulation state.
 */
interface SafetyReasoner {
    fun isConflictUnsafe(
        conflict: Conflict,
        state: SimulationState,
    ): Boolean

    fun isManeuverAllowed(
        aircraftId: String,
        maneuver: Maneuver,
        state: SimulationState,
    ): Boolean

    fun priorityOf(
        aircraftId: String,
        state: SimulationState,
    ): Int

    fun explainDecision(decisionId: String): List<String>
}
