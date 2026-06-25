package planning

/**
 * Minimal breadth-first STRIPS planner.
 *
 * It searches the finite action space up to a maximum depth and returns the first plan
 * whose resulting state satisfies the goal propositions.
 */
class StripsPlanner {
    fun plan(problem: StripsProblem): List<StripsAction>? {
        if (problem.isGoalSatisfiedBy(problem.initialState)) {
            return emptyList()
        }

        val open = ArrayDeque<SearchNode>()
        val visited = mutableSetOf<Set<Proposition>>()

        open.addLast(
            SearchNode(
                state = problem.initialState,
                plan = emptyList(),
            ),
        )
        visited += problem.initialState

        while (open.isNotEmpty()) {
            val node = open.removeFirst()

            if (node.plan.size >= problem.maxDepth) {
                continue
            }

            val applicableActions =
                problem.actions.filter { action ->
                    action.isApplicable(node.state)
                }

            for (action in applicableActions) {
                val nextState = action.applyTo(node.state)

                if (nextState in visited) {
                    continue
                }

                val nextPlan = node.plan + action

                if (problem.isGoalSatisfiedBy(nextState)) {
                    return nextPlan
                }

                visited += nextState
                open.addLast(
                    SearchNode(
                        state = nextState,
                        plan = nextPlan,
                    ),
                )
            }
        }

        return null
    }

    private data class SearchNode(
        val state: Set<Proposition>,
        val plan: List<StripsAction>,
    )
}
