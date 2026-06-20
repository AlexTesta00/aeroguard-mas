package planning

import domain.Conflict
import domain.ResolutionPlan
import domain.SimulationState

interface ResolutionPlanner {
    fun planResolution(
        conflict: Conflict,
        state: SimulationState,
    ): ResolutionPlan?
}
