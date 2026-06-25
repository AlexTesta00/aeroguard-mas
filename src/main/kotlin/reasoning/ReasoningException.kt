package reasoning

/**
 * Exception thrown by symbolic reasoning components when a query or theory load fails.
 */
class ReasoningException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
