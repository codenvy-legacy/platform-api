package com.codenvy.api.auth.sso.server;


import java.security.Principal;

/** Provide communication with authentication clients. */
public interface SsoClientManager {
    /**
     * Notify clients that whey need to update user principal.
     *
     * @param principal
     *         - user principal
     */
    void updatePrincipal(Principal principal);

    /** Make logout for all clients registered on server for certain token. */
    void logout(AccessTicket accessTicket);
}
