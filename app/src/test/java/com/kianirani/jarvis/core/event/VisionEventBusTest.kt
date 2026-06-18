package com.kianirani.jarvis.core.event

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class VisionEventBusTest {

    @Test fun `delivers published events to a subscriber in order`() = runBlocking {
        val bus = VisionEventBus()
        val received = async { bus.events.take(2).toList() }
        while (bus.subscriptionCount.value == 0) yield() // wait until subscribed
        bus.emit(VisionEvent.WakeWord)
        bus.emit(VisionEvent.AppOpened("com.x"))
        assertEquals(listOf(VisionEvent.WakeWord, VisionEvent.AppOpened("com.x")), received.await())
    }

    @Test fun `tryEmit succeeds while buffer has room`() {
        val bus = VisionEventBus()
        assertEquals(true, bus.tryEmit(VisionEvent.UserIdle(1000)))
    }
}
