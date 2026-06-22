package explanation

data class DecisionExplanation(
    val tick: Int,
    val agent: String,
    val decisionId: String,
    val message: String,
    val supportingFacts: List<String> = emptyList(),
) {
    init {
        require(tick >= 0) { "Explanation tick must be non-negative." }
        require(agent.isNotBlank()) { "Explanation agent must not be blank." }
        require(decisionId.isNotBlank()) { "Decision id must not be blank." }
        require(message.isNotBlank()) { "Explanation message must not be blank." }
    }
}
