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
package com.codenvy.api.project.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Hyperlinks;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface ItemReference extends Hyperlinks {
    /** Get name of item. */
    String getName();

    /** Set name of item. */
    void setName(String name);

    ItemReference withName(String name);

    /** Get type of item, e.g. "file", "folder" or "project". */
    String getType();

    /** Set type of item, e.g. "file" or "folder" or "project". */
    void setType(String type);

    ItemReference withType(String type);

    /** Get mediatype. */
    String getMediaType();

    /** Get mediatype. */
    void setMediaType(String mediaType);

    ItemReference withMediaType(String mediaType);

    /** Get path of item. */
    String getPath();

    /** Set path of item. */
    void setPath(String path);

    ItemReference withPath(String path);

    ItemReference withLinks(List<Link> links);
}
