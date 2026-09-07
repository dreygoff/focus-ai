package app.focus.common

/**
 * Sealed interface representing the result of an asynchronous operation.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val throwable: Throwable, val data: T? = null) : Result<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> throwable
        is Success -> null
    }

    /** Maps the success data to a new value, returning `Error` if this is an error. */
    suspend fun map(transform: suspend (T) -> Any?): Result<Any> = when (this) {
        is Success -> try { Success(transform(data)) } catch (e: Exception) { Error(e) }
        is Error -> this as Result<Any>
    }

    /** Maps the success data to another `Result`, enabling flatMap-like behavior. */
    suspend fun <R> mapCatching(transform: suspend (T) -> R): Result<R> = when (this) {
        is Success -> try { Success(transform(data)) } catch (e: Exception) { Error(e) }
        is Error -> Error(this.throwable, null as R?)
    }
}

/** Creates a success result. */
fun <T> Result(success: T): Result<T> = Result.Success(success)

/** Runs the [block] and returns [Result.Success] on success or [Result.Error] on exception. */
suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
}
