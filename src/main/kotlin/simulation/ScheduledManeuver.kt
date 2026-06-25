package simulation

import domain.Maneuver

/**
 * Maneuver scheduled for application at a specific simulation tick.
 */
data class ScheduledManeuver(
    val tick: Int,
    val maneuver: Maneuver,
) {
    init {
        require(tick >= 0) { "Scheduled maneuver tick must be non-negative." }
    }
}
