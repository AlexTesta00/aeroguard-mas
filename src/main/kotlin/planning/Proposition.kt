package planning

@JvmInline
value class Proposition(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Proposition value must not be blank." }
    }

    override fun toString(): String = value

    companion object {
        fun conflictUnresolved(conflictId: String): Proposition = Proposition("conflict_unresolved($conflictId)")

        fun conflictResolved(conflictId: String): Proposition = Proposition("conflict_resolved($conflictId)")

        fun aircraftAvailable(aircraftId: String): Proposition = Proposition("aircraft_available($aircraftId)")

        fun maneuverSelected(
            aircraftId: String,
            maneuverName: String,
        ): Proposition = Proposition("maneuver_selected($aircraftId,$maneuverName)")

        fun separationRestored(conflictId: String): Proposition = Proposition("separation_restored($conflictId)")
    }
}
