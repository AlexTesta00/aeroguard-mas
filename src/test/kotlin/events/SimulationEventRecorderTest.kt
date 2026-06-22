package events

import explanation.ExplanationService
import integration.JsonScenarioLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import planning.StripsResolutionPlanner
import reasoning.TuPrologSafetyReasoner
import simulation.ConflictDetector
import simulation.SimulationEngine
import java.nio.file.Files
import java.nio.file.Path

class SimulationEventRecorderTest {
    @Test
    fun `records end to end simulation events as JSONL`() {
        val scenario = JsonScenarioLoader().load(Path.of("scenarios/simple_conflict.json"))
        val reasoner = TuPrologSafetyReasoner.fromClasspath()
        val engine =
            SimulationEngine(
                conflictDetector = ConflictDetector(safetyReasoner = reasoner),
                predictionHorizonTicks = 6,
            )

        val runResult = engine.run(scenario)
        val conflict =
            runResult.predictedConflicts.firstOrNull()
                ?: runResult.currentConflicts.first()

        val planningState =
            runResult.states.firstOrNull { it.tick == conflict.tick }
                ?: runResult.finalState

        val plan =
            StripsResolutionPlanner(reasoner)
                .planResolution(conflict, planningState)

        val explanations =
            ExplanationService(reasoner)
                .explainRun(runResult, plan)

        val output = Files.createTempFile("aeroguard-recorded-events", ".jsonl")

        JsonlSimulationEventSink(output).use { sink ->
            SimulationEventRecorder(sink).recordRun(
                runResult = runResult,
                resolutionPlan = plan,
                explanations = explanations,
            )
        }

        val lines = Files.readAllLines(output)
        val types =
            lines.map {
                Json
                    .parseToJsonElement(it)
                    .jsonObject["type"]!!
                    .jsonPrimitive.content
            }

        assertTrue("aircraft_state" in types)
        assertTrue("conflict_detected" in types)
        assertTrue("plan_generated" in types)
        assertTrue("maneuver_selected" in types)
        assertTrue("explanation" in types)
    }
}
