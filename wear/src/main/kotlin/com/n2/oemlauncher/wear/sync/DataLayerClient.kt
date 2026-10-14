package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset

interface DataLayerClient {
    suspend fun put(path: String, schema: Int, payload: ByteArray, assets: Map<String, Asset>, urgent: Boolean): Boolean
    suspend fun send(path: String): Boolean
}
