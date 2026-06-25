package domain

/**
 * Types of unsafe situations represented by AeroGuard-MAS.
 *
 * The enum separates current separation losses, predicted conflicts, secondary
 * conflicts created by a maneuver, and weather-zone violations.
 */
enum class ConflictType {
    HORIZONTAL_SEPARATION_LOSS,
    VERTICAL_SEPARATION_LOSS,
    PREDICTED_CONFLICT,
    SECONDARY_CONFLICT,
    WEATHER_ZONE_VIOLATION,
}
