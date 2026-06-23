package events

import domain.Conflict
import domain.EmergencyStatus
import domain.ResolutionPlan
import explanation.DecisionExplanation
import planning.formatAsPlannerAction
import simulation.SimulationRunResult

class SimulationEventRecorder(
    private val sink: SimulationEventSink,
) {
    fun recordRun(
        runResult: SimulationRunResult,
        resolutionPlan: ResolutionPlan?,
        explanations: List<DecisionExplanation>,
        additionalEvents: List<SimulationEvent> = emptyList(),
    ) {
        emitAircraftStates(runResult)
        emitConflicts(runResult.currentConflicts)
        emitConflicts(runResult.predictedConflicts)
        emitPlan(resolutionPlan)
        emitAdditionalEvents(additionalEvents)
        emitExplanations(explanations)
    }

    private fun emitAircraftStates(runResult: SimulationRunResult) {
        runResult.states
            .sortedBy { state -> state.tick }
            .forEach { state ->
                state.aircraft.values
                    .sortedBy { aircraft -> aircraft.id }
                    .forEach { aircraft ->
                        sink.emit(
                            AircraftStateEvent(
                                tick = state.tick,
                                aircraft = aircraft.id,
                                x = aircraft.position.x,
                                y = aircraft.position.y,
                                altitude = aircraft.flightLevel.feet,
                                speed = aircraft.velocity.horizontalUnitsPerTick,
                                status = aircraft.emergencyStatus.toEventStatus(),
                                priority = aircraft.priority.name,
                            ),
                        )
                    }
            }
    }

    private fun emitConflicts(conflicts: List<Conflict>) {
        conflicts
            .sortedWith(compareBy<Conflict> { it.tick }.thenBy { it.id })
            .forEach { conflict ->
                sink.emit(
                    ConflictDetectedEvent(
                        tick = conflict.tick,
                        aircraft = conflict.aircraftIds.sorted(),
                        severity = conflict.toSeverity(),
                        horizontalDistance = conflict.horizontalDistance,
                        verticalDistance = conflict.verticalDistanceFeet,
                        predictedAtTick = conflict.predictedAtTick,
                    ),
                )
            }
    }

    private fun emitPlan(plan: ResolutionPlan?) {
        if (plan == null) {
            return
        }

        val actions =
            plan.maneuvers.map { maneuver ->
                maneuver.formatAsPlannerAction()
            }

        val firstManeuver = plan.maneuvers.first()

        sink.emit(
            PlanGeneratedEvent(
                tick = 0,
                planner = "strips",
                aircraft = firstManeuver.aircraftId,
                actions = actions,
            ),
        )

        plan.maneuvers.forEach { maneuver ->
            sink.emit(
                ManeuverSelectedEvent(
                    tick = 0,
                    aircraft = maneuver.aircraftId,
                    maneuver = maneuver.type.name.lowercase(),
                    targetAltitude = maneuver.targetFlightLevel?.feet,
                    reason = maneuver.reason ?: plan.explanation,
                ),
            )
        }

        sink.emit(
            BeliefUpdatedEvent(
                tick = 0,
                agent = "sector_controller",
                belief = "resolution_plan(${plan.conflictId})",
            ),
        )
    }

    private fun emitAdditionalEvents(events: List<SimulationEvent>) {
        events
            .sortedWith(compareBy<SimulationEvent> { it.tick }.thenBy { it.type })
            .forEach { event ->
                sink.emit(event)
            }
    }

    private fun emitExplanations(explanations: List<DecisionExplanation>) {
        explanations.forEach { explanation ->
            sink.emit(
                ExplanationEvent(
                    tick = explanation.tick,
                    agent = explanation.agent,
                    message = explanation.message,
                ),
            )
        }
    }

    private fun EmergencyStatus.toEventStatus(): String =
        when (this) {
            EmergencyStatus.NONE -> "normal"
            EmergencyStatus.GENERAL -> "emergency"
            EmergencyStatus.LOW_FUEL -> "low_fuel"
        }

    private fun Conflict.toSeverity(): String =
        when {
            verticalDistanceFeet == 0 && horizontalDistance < 2.0 -> "critical"
            verticalDistanceFeet == 0 -> "high"
            predictedAtTick != null -> "predicted"
            else -> "medium"
        }
}
