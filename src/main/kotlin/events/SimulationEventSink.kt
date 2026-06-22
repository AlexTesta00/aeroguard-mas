package events

interface SimulationEventSink {
    fun emit(event: SimulationEvent)
}
