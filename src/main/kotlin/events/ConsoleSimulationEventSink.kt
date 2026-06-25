package events

/**
 * Event sink that writes each simulation event as one JSONL line to standard output.
 */
class ConsoleSimulationEventSink : SimulationEventSink {
    override fun emit(event: SimulationEvent) {
        println(event.toJsonLine())
    }
}
