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
package com.codenvy.api.workspace.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface Attribute {
    String getName();

    void setName(String name);

    Attribute withName(String name);

    String getValue();

    void setValue(String value);

    Attribute withValue(String value);

    String getDescription();

    void setDescription(String description);

    Attribute withDescription(String description);
}
