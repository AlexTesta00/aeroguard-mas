package simulation

import domain.SimulationState
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class SimulationEngineTest {
    private val loader = JsonScenarioLoader()
    private val engine = SimulationEngine(predictionHorizonTicks = 6)

    @Test
    fun `advances simulation state by one tick`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))
        val initialState = SimulationState.fromScenario(scenario)

        val result = engine.advanceOneTick(initialState)

        assertEquals(1, result.state.tick)

        val initialAza = initialState.aircraft.getValue("AZA123")
        val movedAza = result.state.aircraft.getValue("AZA123")

        assertNotEquals(initialAza.position, movedAza.position)
    }

    @Test
    fun `simple conflict scenario produces at least one detected conflict`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))

        val result = engine.run(scenario)

        assertTrue(
            result.currentConflicts.any { it.aircraftIds == setOf("AZA123", "DLH456") },
            "Simple conflict scenario should eventually produce a current conflict.",
        )
    }

    @Test
    fun `no conflict scenario does not produce current conflicts`() {
        val scenario = loader.load(Path.of("scenarios/no_conflict.json"))

        val result = engine.run(scenario)

        assertTrue(
            result.currentConflicts.isEmpty(),
            "No conflict scenario should not produce false positives.",
        )
    }

    @Test
    fun `run stores initial and final states`() {
        val scenario = loader.load(Path.of("scenarios/simple_conflict.json"))

        val result = engine.run(scenario)

        assertEquals(0, result.states.first().tick)
        assertEquals(scenario.maxTicks, result.states.last().tick)
        assertEquals(scenario.maxTicks + 1, result.states.size)
    }
}
