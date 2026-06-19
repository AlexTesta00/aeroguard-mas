package domain

import kotlin.math.sqrt

data class Position(
    val x: Double,
    val y: Double,
) {
    init {
        require(x.isUsableFinite()) { "Position x must be finite." }
        require(y.isUsableFinite()) { "Position y must be finite." }
    }

    fun distanceTo(other: Position): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun stepTowards(
        target: Position,
        maxDistance: Double,
    ): Position {
        require(maxDistance >= 0.0) { "Step distance must be non-negative." }

        val distance = distanceTo(target)
        if (distance == 0.0 || maxDistance == 0.0) {
            return this
        }

        val ratio = minOf(1.0, maxDistance / distance)
        return Position(
            x = x + (target.x - x) * ratio,
            y = y + (target.y - y) * ratio,
        )
    }
}

private fun Double.isUsableFinite(): Boolean = !isNaN() && !isInfinite()
