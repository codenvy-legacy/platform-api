package com.codenvy.api.auth.sso.server;


import com.codenvy.api.auth.sso.client.AuthorizedPrincipal;

import java.security.Principal;

/** Add user roles to principal. */
public interface RolesExtractor {
    public AuthorizedPrincipal extractRoles(Principal principal);
}
