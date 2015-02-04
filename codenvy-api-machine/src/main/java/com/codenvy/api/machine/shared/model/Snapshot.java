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
package com.codenvy.api.machine.shared.model;

import com.codenvy.api.machine.v2.server.spi.ImageMetadata;

import java.util.List;

/**
 *
 * Reference to Image stored in the system
 * @author gazarenkov
 */
public interface Snapshot {

    String getId();

    String getDescription();

    String getOwner();

    List<ProjectBinding> getProjects();

    /**
     * Implementation Specific
     * @return
     */
    ImageMetadata getImageMetadata();
}
