package domain

data class SeparationConfiguration(
    val horizontal: Double,
    val verticalFeet: Int,
) {
    init {
        require(horizontal > 0.0) { "Horizontal separation must be positive." }
        require(verticalFeet > 0) { "Vertical separation must be positive." }
    }
}
