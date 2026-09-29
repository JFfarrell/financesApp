package com.example.personalfinances.domain.model

/** Outcome of an operation that can be refused for a reason the user should see. */
sealed class OperationResult {
    object Success : OperationResult()

    /** Nothing was changed; [message] explains why, in words fit to show the user. */
    data class Failure(val message: String) : OperationResult()
}
