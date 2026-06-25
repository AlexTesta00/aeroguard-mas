package domain

/**
 * Named route point in the simplified two-dimensional airspace.
 */
data class Waypoint(
    val name: String,
    val position: Position,
) {
    init {
        require(name.isNotBlank()) { "Waypoint name must not be blank." }
    }
}
