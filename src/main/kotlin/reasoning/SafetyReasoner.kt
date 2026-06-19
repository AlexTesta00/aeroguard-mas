package reasoning

import domain.Conflict
import domain.Maneuver
import domain.SimulationState

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
