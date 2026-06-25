package domain

import kotlin.math.abs

/**
 * Discrete altitude value expressed in feet.
 *
 * The project uses simplified flight levels rather than real aeronautical vertical
 * profiles. The value class keeps altitude validation close to the domain model.
 */
@JvmInline
value class FlightLevel(
    val feet: Int,
) {
    init {
        require(feet >= 0) { "Flight level altitude must be non-negative." }
    }

    fun verticalDistanceTo(other: FlightLevel): Int = abs(feet - other.feet)

    override fun toString(): String = "FL$feet"
}
