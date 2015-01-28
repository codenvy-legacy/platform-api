/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.account.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com) on 1/28/15.
 *
 */
@DTO
public interface ClientToken {
    String getToken();

    void setToken(String token);

    ClientToken withToken(String token);
}
