package com.n2.oemlauncher.mobile.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.n2.oemlauncher.mobile.CompanionApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CompanionListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val receiver = (application as CompanionApplication).container.receiver
        // The service callback runs off the main thread. Finish before the buffer is released.
        runBlocking(Dispatchers.IO) {
            for (event in events) {
                if (event.type == DataEvent.TYPE_CHANGED) receiver.receive(event.dataItem)
            }
        }
    }
}
