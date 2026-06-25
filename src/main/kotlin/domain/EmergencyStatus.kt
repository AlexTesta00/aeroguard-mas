package domain

/**
 * Emergency state associated with an aircraft.
 *
 * Emergency status affects symbolic priority reasoning. `NONE` means that the aircraft
 * has no active emergency condition.
 */
enum class EmergencyStatus {
    NONE,
    GENERAL,
    LOW_FUEL,
    ;

    val isActive: Boolean
        get() = this != NONE
}
