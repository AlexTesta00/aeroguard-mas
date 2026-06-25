package planning

import domain.Conflict
import domain.ResolutionPlan
import domain.SimulationState

/**
 * Interface for components that can generate a resolution plan for a conflict.
 */
interface ResolutionPlanner {
    fun planResolution(
        conflict: Conflict,
        state: SimulationState,
    ): ResolutionPlan?
}
