package domain

/**
 * Minimum separation thresholds used by conflict detection.
 *
 * Horizontal separation is expressed in simulation coordinate units. Vertical separation
 * is expressed in feet.
 */
data class SeparationConfiguration(
    val horizontal: Double,
    val verticalFeet: Int,
) {
    init {
        require(horizontal > 0.0) { "Horizontal separation must be positive." }
        require(verticalFeet > 0) { "Vertical separation must be positive." }
    }
}
