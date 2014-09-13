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
package com.codenvy.api.core.rest.shared;

import com.codenvy.api.core.rest.shared.dto.Link;

import java.util.List;

/**
 * Helper class for working with links.
 *
 * @author andrew00x
 */
public class Links {
    /**
     * Find link by its relation in the specified list.
     *
     * @param rel
     *         link's relation
     * @param links
     *         list of links
     * @return found link or {@code null}
     */
    public static Link getLink(String rel, List<Link> links) {
        for (Link link : links) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

    private Links() {
    }
}
