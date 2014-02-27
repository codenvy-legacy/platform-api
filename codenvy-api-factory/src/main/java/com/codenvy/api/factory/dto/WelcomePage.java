package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Welcome page which user can specified to show when factory accepted.
 * To show custom information applied only for this factory url.
 * Contains two configuration for authenticated users and non authenticated.
 */
@DTO
public interface WelcomePage {
    public WelcomeConfiguration getAuthenticated();

    public void setAuthenticated(WelcomeConfiguration authenticated);

    public WelcomeConfiguration getNonauthenticated();

    public void setNonauthenticated(WelcomeConfiguration nonauthenticated);
}
