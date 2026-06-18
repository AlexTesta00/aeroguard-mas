package domain

enum class AircraftPriority(
    val score: Int,
) {
    NORMAL(score = 10),
    HIGH(score = 50),
    EMERGENCY(score = 100),
}
