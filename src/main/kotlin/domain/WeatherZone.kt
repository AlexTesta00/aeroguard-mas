package domain

/**
 * Circular weather constraint that may become active during a scenario.
 *
 * Active weather zones are treated as forbidden route areas by the replanning layer.
 */
data class WeatherZone(
    val id: String,
    val center: Position,
    val radius: Double,
    val activeFromTick: Int = 0,
    val activeUntilTick: Int? = null,
) {
    init {
        require(id.isNotBlank()) { "Weather zone id must not be blank." }
        require(radius > 0.0) { "Weather zone radius must be positive." }
        require(activeFromTick >= 0) { "Weather zone activeFromTick must be non-negative." }
        require(activeUntilTick == null || activeUntilTick >= activeFromTick) {
            "Weather zone activeUntilTick must be greater than or equal to activeFromTick."
        }
    }

    fun isActiveAt(tick: Int): Boolean {
        if (tick < activeFromTick) return false
        return activeUntilTick == null || tick <= activeUntilTick
    }
}
