package domain

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
