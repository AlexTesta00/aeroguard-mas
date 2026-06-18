package domain

import kotlin.math.abs

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
