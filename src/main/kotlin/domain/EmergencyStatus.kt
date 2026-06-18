package domain

enum class EmergencyStatus {
    NONE,
    GENERAL,
    LOW_FUEL,
    ;

    val isActive: Boolean
        get() = this != NONE
}
