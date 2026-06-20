package planning

import domain.Maneuver

data class StripsAction(
    val name: String,
    val preconditions: Set<Proposition>,
    val addEffects: Set<Proposition>,
    val deleteEffects: Set<Proposition>,
    val maneuver: Maneuver? = null,
) {
    init {
        require(name.isNotBlank()) { "STRIPS action name must not be blank." }
        require(addEffects.isNotEmpty()) { "STRIPS action must add at least one effect." }
    }

    fun isApplicable(state: Set<Proposition>): Boolean = state.containsAll(preconditions)

    fun applyTo(state: Set<Proposition>): Set<Proposition> {
        require(isApplicable(state)) {
            "Cannot apply action '$name' because preconditions are not satisfied."
        }

        return (state - deleteEffects) + addEffects
    }
}
