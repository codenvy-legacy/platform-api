/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.auth.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author gazarenkov
 */
@DTO
public interface Credentials {

    String getRealm();

    void setRealm(String realm);

    Credentials withRealm(String realm);

    String getUsername();

    void setUsername(String name);

    Credentials withUsername(String name);

    String getPassword();

    void setPassword(String password);

    Credentials withPassword(String password);

}
