package com.n2.oemlauncher.shared.protocol

import com.n2.oemlauncher.shared.config.CatalogSanitizer
import com.n2.oemlauncher.shared.config.ConfigSanitizer
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Wire format of every payload: JSON, gzip-compressed, tagged with a schema in the DataMap.
 *
 * Decoding is lenient towards newer peers (unknown keys are ignored and unknown enum values
 * fall back to defaults) and strict about safety (size caps, sanitized output).
 */
object PayloadCodec {

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encodeCatalog(catalog: WatchCatalog): ByteArray = encode(WatchCatalog.serializer(), catalog)

    fun decodeCatalog(schema: Int, bytes: ByteArray): DecodeResult<WatchCatalog> =
        decode(schema, bytes, WatchCatalog.serializer(), CatalogSanitizer::sanitize)

    fun encodeConfig(config: LauncherConfig): ByteArray = encode(LauncherConfig.serializer(), config)

    fun decodeConfig(schema: Int, bytes: ByteArray): DecodeResult<LauncherConfig> =
        decode(schema, bytes, LauncherConfig.serializer(), ConfigSanitizer::sanitize)

    fun encodeState(state: WatchConfigState): ByteArray =
        encode(WatchConfigState.serializer(), state)

    fun decodeState(schema: Int, bytes: ByteArray): DecodeResult<WatchConfigState> =
        decode(schema, bytes, WatchConfigState.serializer()) { state ->
            state.copy(
                current = ConfigSanitizer.sanitize(state.current),
                missingIconIds = state.missingIconIds.filter(IconId::isValid),
            )
        }

    /** True when an encoded payload can be stored in a single DataItem. */
    fun fitsDataItem(payload: ByteArray): Boolean = payload.size <= ProtocolLimits.MAX_PAYLOAD_BYTES

    /** Plain JSON for local persistence (no compression, no schema tag). */
    fun configToJson(config: LauncherConfig): String =
        json.encodeToString(LauncherConfig.serializer(), config)

    /** Reads JSON written by [configToJson]; returns null for corrupt data instead of throwing. */
    fun configFromJson(text: String): LauncherConfig? = try {
        ConfigSanitizer.sanitize(json.decodeFromString(LauncherConfig.serializer(), text))
    } catch (e: IllegalArgumentException) {
        null
    }

    fun catalogToJson(catalog: WatchCatalog): String =
        json.encodeToString(WatchCatalog.serializer(), catalog)

    fun catalogFromJson(text: String): WatchCatalog? = try {
        CatalogSanitizer.sanitize(json.decodeFromString(WatchCatalog.serializer(), text))
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun <T> encode(serializer: KSerializer<T>, value: T): ByteArray =
        GzipCodec.compress(json.encodeToString(serializer, value).encodeToByteArray())

    private inline fun <T> decode(
        schema: Int,
        bytes: ByteArray,
        serializer: KSerializer<T>,
        sanitize: (T) -> T,
    ): DecodeResult<T> {
        if (!ProtocolVersion.isSupported(schema)) {
            return DecodeResult.Failure(DecodeFailure.UNSUPPORTED_SCHEMA, "schema=$schema")
        }
        if (bytes.size > ProtocolLimits.MAX_PAYLOAD_BYTES) {
            return DecodeResult.Failure(DecodeFailure.TOO_LARGE, "size=${bytes.size}")
        }
        return try {
            val text = GzipCodec.decompress(bytes, ProtocolLimits.MAX_INFLATED_BYTES).decodeToString()
            DecodeResult.Success(sanitize(json.decodeFromString(serializer, text)))
        } catch (e: PayloadTooLargeException) {
            DecodeResult.Failure(DecodeFailure.TOO_LARGE, e.message)
        } catch (e: IOException) {
            DecodeResult.Failure(DecodeFailure.MALFORMED, e.message)
        } catch (e: SerializationException) {
            DecodeResult.Failure(DecodeFailure.MALFORMED, e.message)
        } catch (e: IllegalArgumentException) {
            DecodeResult.Failure(DecodeFailure.MALFORMED, e.message)
        }
    }
}
