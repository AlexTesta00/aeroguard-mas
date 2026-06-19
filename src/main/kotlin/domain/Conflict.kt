package domain

data class Conflict(
    val id: String,
    val tick: Int,
    val aircraftIds: Set<String>,
    val type: ConflictType,
    val horizontalDistance: Double,
    val verticalDistanceFeet: Int,
    val predictedAtTick: Int? = null,
) {
    init {
        require(id.isNotBlank()) { "Conflict id must not be blank." }
        require(tick >= 0) { "Conflict tick must be non-negative." }
        require(aircraftIds.size >= 2) { "A conflict must involve at least two aircraft." }
        require(horizontalDistance >= 0.0) { "Horizontal distance must be non-negative." }
        require(verticalDistanceFeet >= 0) { "Vertical distance must be non-negative." }
        require(predictedAtTick == null || predictedAtTick >= tick) {
            "Predicted conflict tick must be greater than or equal to detection tick."
        }
    }
}
