package simulation

import domain.SimulationState
import integration.JsonScenarioLoader
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reasoning.TuPrologSafetyReasoner
import java.nio.file.Path

class ReasonedConflictDetectorTest {
    @Test
    fun `conflict detector can use tuProlog safety reasoner`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/simple_conflict.json"))
        val state = SimulationState.fromScenario(scenario)

        val detector =
            ConflictDetector(
                aircraftMover = AircraftMover(),
                safetyReasoner = TuPrologSafetyReasoner.fromClasspath(),
            )

        val predictions =
            detector.predictConflicts(
                state = state,
                horizonTicks = 6,
            )

        assertTrue(
            predictions.any { it.aircraftIds == setOf("AZA123", "DLH456") },
            "The detector should still predict the simple conflict when backed by tuProlog.",
        )
    }
}
