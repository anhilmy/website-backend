package com.anhilmy.pos.middleware;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthenticationFilterTest {

    @Test
    void corsPreflightIsPublic() {
        assertTrue(AuthenticationFilter.isPublic("OPTIONS", "/api/v1/orders"));
        assertTrue(AuthenticationFilter.isPublic("OPTIONS", "/api/v1/users"));
    }

    @Test
    void documentedPublicEndpointsArePublic() {
        assertTrue(AuthenticationFilter.isPublic("GET", "/api/v1/healthcheck"));
        assertTrue(AuthenticationFilter.isPublic("GET", "/api/v1/healthcheck/ready"));
        assertTrue(AuthenticationFilter.isPublic("POST", "/api/v1/auth/login"));
        assertTrue(AuthenticationFilter.isPublic("GET", "/api/v1/menu/categories"));
        assertTrue(AuthenticationFilter.isPublic("GET", "/api/v1/menu/items"));
        assertTrue(AuthenticationFilter.isPublic("GET", "/api/v1/menu/items/42"));
        assertTrue(AuthenticationFilter.isPublic("GET", "/api/v1/menu/items/42/modifiers"));
    }

    @Test
    void protectedEndpointsAreNotPublic() {
        assertFalse(AuthenticationFilter.isPublic("GET", "/api/v1/orders"));
        assertFalse(AuthenticationFilter.isPublic("POST", "/api/v1/orders"));
        assertFalse(AuthenticationFilter.isPublic("GET", "/api/v1/users"));
        assertFalse(AuthenticationFilter.isPublic("POST", "/api/v1/categories"));
        // Writes to the menu must require auth even though reads are public.
        assertFalse(AuthenticationFilter.isPublic("POST", "/api/v1/menu/items"));
    }
}
