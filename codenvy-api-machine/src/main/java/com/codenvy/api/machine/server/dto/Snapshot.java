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
package com.codenvy.api.machine.server.dto;

import com.codenvy.api.machine.shared.dto.SnapshotDescriptor;
import com.codenvy.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface Snapshot {
    String getId();

    void setId(String id);

    Snapshot withId(String id);

    String getDescription();

    void setDescription(String description);

    Snapshot withDescription(String description);
}
