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
package com.codenvy.api.account.server;

import com.codenvy.api.account.shared.dto.UpdateResourcesDescriptor;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

import java.util.List;

/**
 * //TODO
 *
 * @author Sergii Leschenko
 */
public interface ResourcesManager {
    void redistributeResources(String accountId, List<UpdateResourcesDescriptor> updateResourcesDescriptors)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException;
}