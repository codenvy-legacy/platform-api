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
package org.eclipse.che.api.account.server;

import org.eclipse.che.api.account.server.dao.Subscription;
import org.eclipse.che.api.account.shared.dto.NewSubscriptionAttributes;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

/**
 * Process payments.
 *
 * @author Alexander Garagatyi
 */
public interface PaymentService {
    /**
     * Purchases subscription.
     *
     * @param subscription
     *         subscription for which the user pays
     * @param subscriptionAttributes
     *         attributes of the subscription
     * @throws ServerException
     *         if internal server error occurs
     */
    NewSubscriptionAttributes addSubscription(Subscription subscription, NewSubscriptionAttributes subscriptionAttributes)
            throws ConflictException, ServerException, ForbiddenException;

    /**
     * Removes subscription from payment service.
     *
     * @param subscriptionId
     *         id of the subscription to be removed
     * @throws NotFoundException
     *         if subscription is not found
     * @throws ServerException
     */
    void removeSubscription(String subscriptionId) throws NotFoundException, ServerException, ForbiddenException;
}