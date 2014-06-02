/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Welcome page which user can specified to show when factory accepted.
 * To show custom information applied only for this factory url.
 * Contains two configuration for authenticated users and non authenticated.
 */
@DTO
public interface WelcomePage {

    /**
     * @return
     */
    WelcomeConfiguration getAuthenticated();

    void setAuthenticated(WelcomeConfiguration authenticated);

    WelcomePage withAuthenticated(WelcomeConfiguration authenticated);

    /**
     * @return
     */
    WelcomeConfiguration getNonauthenticated();

    void setNonauthenticated(WelcomeConfiguration nonauthenticated);

    WelcomePage withNonauthenticated(WelcomeConfiguration nonauthenticated);
}
