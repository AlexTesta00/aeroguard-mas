package planning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StripsPlannerTest {
    private val planner = StripsPlanner()

    @Test
    fun `finds single step plan when action reaches goal`() {
        val conflictOpen = Proposition("conflict_unresolved(C1)")
        val conflictResolved = Proposition("conflict_resolved(C1)")

        val action =
            StripsAction(
                name = "climb(DLH456,32000)",
                preconditions = setOf(conflictOpen),
                addEffects = setOf(conflictResolved),
                deleteEffects = setOf(conflictOpen),
            )

        val problem =
            StripsProblem(
                initialState = setOf(conflictOpen),
                goal = setOf(conflictResolved),
                actions = listOf(action),
                maxDepth = 3,
            )

        val plan = planner.plan(problem)

        assertEquals(listOf(action), plan)
    }

    @Test
    fun `finds multi step plan with breadth first search`() {
        val atStart = Proposition("at_start")
        val rerouted = Proposition("rerouted")
        val safe = Proposition("safe")

        val reroute =
            StripsAction(
                name = "reroute_to_waypoint(AZA123,W_SAFE)",
                preconditions = setOf(atStart),
                addEffects = setOf(rerouted),
                deleteEffects = setOf(atStart),
            )

        val continueRoute =
            StripsAction(
                name = "continue_route(AZA123)",
                preconditions = setOf(rerouted),
                addEffects = setOf(safe),
                deleteEffects = emptySet(),
            )

        val problem =
            StripsProblem(
                initialState = setOf(atStart),
                goal = setOf(safe),
                actions = listOf(continueRoute, reroute),
                maxDepth = 4,
            )

        val plan = planner.plan(problem)

        assertEquals(listOf(reroute, continueRoute), plan)
    }

    @Test
    fun `returns empty plan when initial state already satisfies goal`() {
        val safe = Proposition("safe")

        val problem =
            StripsProblem(
                initialState = setOf(safe),
                goal = setOf(safe),
                actions = emptyList(),
                maxDepth = 2,
            )

        val plan = planner.plan(problem)

        assertTrue(plan!!.isEmpty())
    }

    @Test
    fun `returns null when goal is unreachable`() {
        val conflictOpen = Proposition("conflict_unresolved(C1)")
        val conflictResolved = Proposition("conflict_resolved(C1)")
        val unavailable = Proposition("aircraft_unavailable(DLH456)")

        val action =
            StripsAction(
                name = "climb(DLH456,32000)",
                preconditions = setOf(unavailable),
                addEffects = setOf(conflictResolved),
                deleteEffects = setOf(conflictOpen),
            )

        val problem =
            StripsProblem(
                initialState = setOf(conflictOpen),
                goal = setOf(conflictResolved),
                actions = listOf(action),
                maxDepth = 3,
            )

        assertNull(planner.plan(problem))
    }
}
