package domain

data class ResolutionPlan(
    val id: String,
    val conflictId: String,
    val maneuvers: List<Maneuver>,
    val explanation: String,
) {
    init {
        require(id.isNotBlank()) { "Resolution plan id must not be blank." }
        require(conflictId.isNotBlank()) { "Conflict id must not be blank." }
        require(maneuvers.isNotEmpty()) { "Resolution plan must contain at least one maneuver." }
        require(explanation.isNotBlank()) { "Resolution plan explanation must not be blank." }
    }
}
