package integration

import domain.AgentSnapshot

data class JasonMessageDelegation(
    val from: String,
    val to: String,
    val performative: String,
)

data class JasonSmokeReport(
    val requiredAgentsPresent: Boolean,
    val bdiConceptsPresent: Boolean,
    val messagePassingPresent: Boolean,
    val achieveMessagesPresent: Boolean,
    val agentSnapshots: List<AgentSnapshot>,
    val delegations: List<JasonMessageDelegation>,
    val missingAgents: List<String>,
) {
    val passed: Boolean
        get() =
            requiredAgentsPresent &&
                bdiConceptsPresent &&
                messagePassingPresent &&
                achieveMessagesPresent &&
                missingAgents.isEmpty()

    fun toHumanReadableText(): String =
        buildString {
            appendLine("Jason smoke report")
            appendLine("- requiredAgentsPresent=$requiredAgentsPresent")
            appendLine("- bdiConceptsPresent=$bdiConceptsPresent")
            appendLine("- messagePassingPresent=$messagePassingPresent")
            appendLine("- achieveMessagesPresent=$achieveMessagesPresent")
            appendLine("- missingAgents=$missingAgents")
            appendLine("- agents=${agentSnapshots.map { it.agentName }}")
            appendLine("- delegations=$delegations")
        }
}
