package domain

/**
 * Snapshot of an agent's observable BDI state.
 *
 * The snapshot is used by smoke tests and JSONL/GUI observability to represent
 * beliefs, goals, and intentions extracted from Jason AgentSpeak source files.
 *
 * @property agentName logical agent name.
 * @property beliefs currently known symbolic beliefs.
 * @property goals declared or observed achievement goals.
 * @property intentions plan triggers interpreted as active intentions.
 */
data class AgentSnapshot(
    val agentName: String,
    val beliefs: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val intentions: List<String> = emptyList(),
) {
    init {
        require(agentName.isNotBlank()) { "Agent name must not be blank." }
    }
}
