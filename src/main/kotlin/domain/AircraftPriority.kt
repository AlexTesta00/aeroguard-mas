package domain

/**
 * Priority category assigned to an aircraft.
 *
 * The numeric score is intentionally simple and is used by the symbolic reasoner and
 * planners to decide which aircraft should be protected or maneuvered first.
 *
 * @property score priority score, where larger values represent higher priority.
 */
enum class AircraftPriority(
    val score: Int,
) {
    NORMAL(score = 10),
    HIGH(score = 50),
    EMERGENCY(score = 100),
}
