package com.lifemate

import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.utils.SecureStore
import org.junit.*
import org.junit.Assert.*

class SecurityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @After fun cleanPin() { SecureStore(context).removePin() }
    @Test fun pinSurvivesStoreRecreationAndRejectsWrongAttempts() {
        val store = SecureStore(context)
        store.setPin("827364")
        val reopened = SecureStore(context)
        assertTrue(reopened.hasPin())
        assertFalse(reopened.verifyPin("827365"))
        assertTrue(reopened.verifyPin("827364"))
        repeat(5) { assertFalse(reopened.verifyPin("000000")) }
        assertTrue(reopened.retrySeconds() > 0)
        assertFalse(reopened.verifyPin("827364"))
        reopened.removePin()
        assertFalse(reopened.hasPin())
    }
    @Test fun databaseKeyIsStableAndRandomLengthIsCorrect() {
        val first = SecureStore(context).databaseKey()
        assertEquals(32, first.size)
        assertArrayEquals(first, SecureStore(context).databaseKey())
    }
}
