package com.n2.oemlauncher.mobile.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.n2.oemlauncher.mobile.CompanionApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CompanionListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(events: DataEventBuffer) {
        // The system releases the buffer right after this callback, so freeze the events we
        // care about and finish decoding on a service-scoped job instead of blocking here.
        val frozen = events.use { buffer ->
            buffer.mapNotNull { if (it.type == DataEvent.TYPE_CHANGED) it.freeze() else null }
        }
        if (frozen.isEmpty()) return
        val receiver = (application as CompanionApplication).container.receiver
        scope.launch { for (event in frozen) receiver.receive(event.dataItem) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
