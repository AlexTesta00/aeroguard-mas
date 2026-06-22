package events

class CompositeSimulationEventSink(
    private val sinks: List<SimulationEventSink>,
) : SimulationEventSink {
    constructor(vararg sinks: SimulationEventSink) : this(sinks.toList())

    init {
        require(sinks.isNotEmpty()) { "Composite sink requires at least one sink." }
    }

    override fun emit(event: SimulationEvent) {
        sinks.forEach { sink ->
            sink.emit(event)
        }
    }
}
