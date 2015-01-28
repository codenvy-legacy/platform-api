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

import java.util.Map;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface ProvidedResources {
    Map<String, String> getForAccount();

    void setForAccount(Map<String, String> forAccount);

    ProvidedResources withForAccount(Map<String, String> byAccount);
}
