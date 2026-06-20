package integration

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class JasonAgentSmokeAnalyzerTest {
    private val catalog = JasonAgentCatalog(Path.of("src/main/agents"))
    private val analyzer = JasonAgentSmokeAnalyzer()

    @Test
    fun `Jason agents expose BDI concepts`() {
        val report = analyzer.analyze(catalog.loadRequiredAgents())

        assertTrue(report.requiredAgentsPresent, report.toHumanReadableText())
        println(
            report.agentSnapshots.joinToString("\n") {
                "${it.agentName}: beliefs=${it.beliefs}, goals=${it.goals}, intentions=${it.intentions}"
            },
        )
        assertTrue(report.bdiConceptsPresent, report.toHumanReadableText())
        assertTrue(report.agentSnapshots.isNotEmpty())

        report.agentSnapshots.forEach { snapshot ->
            assertFalse(snapshot.beliefs.isEmpty(), "${snapshot.agentName} should expose beliefs.")
            assertFalse(snapshot.goals.isEmpty(), "${snapshot.agentName} should expose goals.")
            assertFalse(snapshot.intentions.isEmpty(), "${snapshot.agentName} should expose intentions.")
        }
    }

    @Test
    fun `Jason agents use message passing and delegation`() {
        val report = analyzer.analyze(catalog.loadRequiredAgents())

        assertTrue(report.messagePassingPresent, report.toHumanReadableText())
        assertTrue(report.achieveMessagesPresent, report.toHumanReadableText())
        assertTrue(
            report.delegations.any {
                it.from == "sector_controller" &&
                    it.to == "resolution_planner" &&
                    it.performative == "achieve"
            },
            report.toHumanReadableText(),
        )
        assertTrue(
            report.delegations.any {
                it.from == "resolution_planner" &&
                    it.to == "explanation_agent"
            },
            report.toHumanReadableText(),
        )
    }

    @Test
    fun `smoke report passes for current agents`() {
        val report = analyzer.analyze(catalog.loadRequiredAgents())
        println(
            report.agentSnapshots.joinToString("\n") {
                "${it.agentName}: beliefs=${it.beliefs}, goals=${it.goals}, intentions=${it.intentions}"
            },
        )
        assertTrue(report.passed, report.toHumanReadableText())
    }
}
