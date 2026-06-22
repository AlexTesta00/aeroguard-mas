package events

class ConsoleSimulationEventSink : SimulationEventSink {
    override fun emit(event: SimulationEvent) {
        println(event.toJsonLine())
    }
}
