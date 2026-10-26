package ru.dzhaparidze.mykct.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.dzhaparidze.mykct.data.auth.AuthState
import ru.dzhaparidze.mykct.data.auth.User

/** От [AuthState.isPending] зависит, снимут ли устройство с пушей. */
class AuthStateTest {

    @Test
    fun `старт без сети с сохранённой сессией - не выход`() {
        assertTrue(AuthState(user = null, isBootstrapping = false, hasStoredSession = true).isPending)
    }

    @Test
    fun `без сохранённой сессии - выход, а вошедший уже подтверждён`() {
        assertFalse(AuthState(user = null, isBootstrapping = false).isPending)
        assertFalse(AuthState(user = User(), isBootstrapping = false, hasStoredSession = true).isPending)
    }
}
