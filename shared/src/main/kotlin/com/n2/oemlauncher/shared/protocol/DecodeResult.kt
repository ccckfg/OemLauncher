package com.n2.oemlauncher.shared.protocol

/** Why a payload from the other device was refused. */
enum class DecodeFailure { UNSUPPORTED_SCHEMA, TOO_LARGE, MALFORMED }

/** Decoding never throws; callers branch on this instead. */
sealed interface DecodeResult<out T> {
    data class Success<out T>(val value: T) : DecodeResult<T>

    data class Failure(val reason: DecodeFailure, val detail: String? = null) :
        DecodeResult<Nothing>
}

fun <T> DecodeResult<T>.getOrNull(): T? = (this as? DecodeResult.Success)?.value
