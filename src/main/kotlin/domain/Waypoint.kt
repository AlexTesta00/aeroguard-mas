package domain

data class Waypoint(
    val name: String,
    val position: Position,
) {
    init {
        require(name.isNotBlank()) { "Waypoint name must not be blank." }
    }
}
