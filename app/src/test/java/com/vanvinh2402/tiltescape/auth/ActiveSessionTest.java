package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class ActiveSessionTest {
    @Test
    public void startsUnauthenticatedAndRequiresAuthenticationForUserId() {
        ActiveSession session = new ActiveSession();

        assertFalse(session.isAuthenticated());
        assertNull(session.getUser());
        try {
            session.requireUserId();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // A game must never begin without a resolved account.
        }
    }

    @Test
    public void authenticateExposesUserUntilClear() {
        ActiveSession session = new ActiveSession();
        AuthenticatedUser user = new AuthenticatedUser(7L, "Player_01");

        session.authenticate(user);

        assertTrue(session.isAuthenticated());
        assertEquals(user, session.getUser());
        assertEquals(7L, session.requireUserId());

        session.clear();
        assertFalse(session.isAuthenticated());
        assertNull(session.getUser());
    }
}
