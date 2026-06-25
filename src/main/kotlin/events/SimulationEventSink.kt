package events

/**
 * Destination for structured simulation events.
 *
 * Implementations can write to console, JSONL files, or compose multiple sinks.
 */
interface SimulationEventSink {
    fun emit(event: SimulationEvent)
}
