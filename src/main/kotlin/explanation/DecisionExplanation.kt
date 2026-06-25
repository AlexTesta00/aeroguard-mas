package explanation

/**
 * Human-readable explanation produced by the explanation layer.
 *
 * Explanations are linked to a tick, an agent, a decision identifier, and optional
 * symbolic supporting facts.
 */
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
