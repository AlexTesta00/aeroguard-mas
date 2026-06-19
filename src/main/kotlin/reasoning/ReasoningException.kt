package reasoning

class ReasoningException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
