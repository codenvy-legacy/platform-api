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
package com.codenvy.api.account.server.dao;

import com.codenvy.api.account.shared.dto.Plan;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

import java.util.List;

/**
 * DAO interface that performs CRUD operations with {@link Plan}.
 *
 * @author Alexander Garagatyi
 */
public interface PlanDao {
    /**
     * Retrieve plan with certain planId
     *
     * @param planId
     *         id of required plan
     * @return stored plan
     * @throws NotFoundException
     *         when account doesn't exist
     * @throws ServerException
     */
    public Plan getPlanById(String planId) throws NotFoundException, ServerException;

    /**
     * Get all existing plans
     *
     * @return list of existing plans
     * @throws ServerException
     */
    public List<Plan> getPlans() throws ServerException;
}
