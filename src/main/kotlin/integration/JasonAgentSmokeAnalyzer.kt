package integration

import domain.AgentSnapshot

/**
 * Static smoke analyzer for Jason AgentSpeak sources.
 *
 * It extracts observable BDI concepts and message passing patterns without starting a
 * full Jason runtime. This keeps CI stable while proving that real `.asl` files exist
 * and contain BDI-oriented content.
 */
class JasonAgentSmokeAnalyzer {
    fun analyze(sources: List<JasonAgentSource>): JasonSmokeReport {
        val expectedNames =
            JasonAgentCatalog.REQUIRED_AGENTS
                .map { it.agentName }
                .toSet()

        val presentNames =
            sources
                .map { it.agentName }
                .toSet()

        val missingAgents = (expectedNames - presentNames).sorted()

        val snapshots =
            sources.map { source ->
                val lines = source.source.cleanedJasonLines()

                AgentSnapshot(
                    agentName = source.agentName,
                    beliefs = extractBeliefs(lines),
                    goals = extractGoals(lines),
                    intentions = extractIntentions(lines),
                )
            }

        val delegations =
            sources.flatMap { source ->
                extractDelegations(source)
            }

        return JasonSmokeReport(
            requiredAgentsPresent = missingAgents.isEmpty(),
            bdiConceptsPresent =
                snapshots.all { snapshot ->
                    snapshot.beliefs.isNotEmpty() &&
                        snapshot.goals.isNotEmpty() &&
                        snapshot.intentions.isNotEmpty()
                },
            messagePassingPresent = delegations.isNotEmpty(),
            achieveMessagesPresent = delegations.any { it.performative == "achieve" },
            agentSnapshots = snapshots,
            delegations = delegations,
            missingAgents = missingAgents,
        )
    }

    private fun extractBeliefs(lines: List<String>): List<String> =
        lines
            .filter { line -> isTopLevelBelief(line) }
            .map { it.removeSuffix(".").trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun extractGoals(lines: List<String>): List<String> =
        lines
            .flatMap { line ->
                GOAL_REGEX.findAll(line).map { match -> match.value }
            }.map { it.trim() }
            .distinct()

    private fun extractIntentions(lines: List<String>): List<String> {
        val intentions = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (!line.startsWith("+")) {
                return@forEachIndexed
            }

            val trigger =
                line
                    .substringBefore("<-")
                    .trim()

            val hasInlineBody = "<-" in line
            val hasBodyOnNextLine =
                lines
                    .drop(index + 1)
                    .takeWhile { nextLine ->
                        !nextLine.startsWith("+") &&
                            !nextLine.startsWith("!") &&
                            !isTopLevelBelief(nextLine)
                    }.any { nextLine ->
                        nextLine.startsWith("<-")
                    }

            if (hasInlineBody || hasBodyOnNextLine) {
                intentions +=
                    trigger
                        .removePrefix("+")
                        .trim()
            }
        }

        return intentions
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun extractDelegations(source: JasonAgentSource): List<JasonMessageDelegation> =
        SEND_REGEX
            .findAll(source.source)
            .map { match ->
                JasonMessageDelegation(
                    from = source.agentName,
                    to = match.groupValues[1].trim(),
                    performative = match.groupValues[2].trim(),
                )
            }.toList()

    private fun isTopLevelBelief(line: String): Boolean =
        line.endsWith(".") &&
            !line.startsWith("+") &&
            !line.startsWith("!") &&
            !line.startsWith(".send") &&
            "<-" !in line

    private fun String.cleanedJasonLines(): List<String> =
        lineSequence()
            .map { line -> line.substringBefore("//").trim() }
            .filter { it.isNotBlank() }
            .toList()

    companion object {
        private val GOAL_REGEX =
            Regex(
                pattern = """![a-zA-Z_][a-zA-Z0-9_]*(?:\([^;\n.]*\))?""",
            )

        private val SEND_REGEX =
            Regex(
                pattern = """\.send\(\s*([^,\s]+)\s*,\s*(tell|achieve|askOne|askAll|askHow)\s*,""",
            )
    }
}
