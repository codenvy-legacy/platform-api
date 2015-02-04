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
package com.codenvy.api.machine.v2.server;

import com.codenvy.api.machine.v2.server.spi.ImageId;
import com.codenvy.api.machine.v2.shared.ProjectBinding;

import java.util.List;

/**
 * Saved state of {@link com.codenvy.api.machine.v2.server.spi.Machine}.
 *
 * @author andrew00x
 */
public interface Snapshot {
    String getId();

    ImageId getImageId();

    String getCreatedBy();

    long getCreationDate();

    String getWorkspaceId();

    List<ProjectBinding> getProjects();
}
