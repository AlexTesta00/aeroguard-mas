package planning

data class StripsProblem(
    val initialState: Set<Proposition>,
    val goal: Set<Proposition>,
    val actions: List<StripsAction>,
    val maxDepth: Int = 5,
) {
    init {
        require(goal.isNotEmpty()) { "STRIPS goal must not be empty." }
        require(maxDepth >= 0) { "STRIPS maxDepth must be non-negative." }

        val actionNames = actions.map { it.name }
        require(actionNames.toSet().size == actionNames.size) {
            "STRIPS action names must be unique."
        }
    }

    fun isGoalSatisfiedBy(state: Set<Proposition>): Boolean = state.containsAll(goal)
}
