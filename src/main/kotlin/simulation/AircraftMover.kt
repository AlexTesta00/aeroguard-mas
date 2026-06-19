package simulation

import domain.Aircraft

class AircraftMover {
    fun advanceOneTick(aircraft: Aircraft): Aircraft {
        var position = aircraft.position
        var waypointIndex = aircraft.activeWaypointIndex
        var remainingDistance = aircraft.velocity.horizontalUnitsPerTick

        while (remainingDistance > 0.0 && waypointIndex < aircraft.route.waypoints.size) {
            val target = aircraft.route.waypoints[waypointIndex].position
            val distanceToTarget = position.distanceTo(target)

            if (distanceToTarget == 0.0) {
                waypointIndex += 1
                continue
            }

            if (remainingDistance >= distanceToTarget) {
                position = target
                remainingDistance -= distanceToTarget
                waypointIndex += 1
            } else {
                position = position.stepTowards(target, remainingDistance)
                remainingDistance = 0.0
            }
        }

        val normalizedWaypointIndex =
            minOf(
                waypointIndex,
                aircraft.route.waypoints.lastIndex,
            )

        return aircraft.copy(
            position = position,
            activeWaypointIndex = normalizedWaypointIndex,
        )
    }
}
