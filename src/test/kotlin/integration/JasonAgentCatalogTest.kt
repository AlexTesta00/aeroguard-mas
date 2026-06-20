package integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class JasonAgentCatalogTest {
    @Test
    fun `loads all required Jason agents`() {
        val catalog = JasonAgentCatalog(Path.of("src/main/agents"))

        val agents = catalog.loadRequiredAgents()

        assertEquals(
            setOf(
                "aircraft_agent",
                "sector_controller",
                "conflict_detector",
                "resolution_planner",
                "explanation_agent",
            ),
            agents.map { it.agentName }.toSet(),
        )

        agents.forEach { agent ->
            assertTrue(agent.source.isNotBlank(), "Agent ${agent.agentName} should not be empty.")
            assertTrue(agent.fileName.endsWith(".asl"))
        }
    }
}
