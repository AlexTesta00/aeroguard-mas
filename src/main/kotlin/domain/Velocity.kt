package domain

/**
 * Horizontal speed expressed in simulation units per tick.
 *
 * The model is intentionally simple and does not distinguish airspeed, groundspeed, or
 * climb/descent rate.
 */
@JvmInline
value class Velocity(
    val horizontalUnitsPerTick: Double,
) {
    init {
        require(!horizontalUnitsPerTick.isNaN()) { "Velocity must not be NaN." }
        require(!horizontalUnitsPerTick.isInfinite()) { "Velocity must be finite." }
        require(horizontalUnitsPerTick >= 0.0) { "Velocity must be non-negative." }
    }

    override fun toString(): String = "$horizontalUnitsPerTick units/tick"
}
