package events

/**
 * Event sink that forwards each simulation event to multiple child sinks.
 *
 * This is useful when the same run must be logged both to console and to a JSONL file.
 */
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
