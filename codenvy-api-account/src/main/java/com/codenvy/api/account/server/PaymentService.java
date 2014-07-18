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

import com.codenvy.api.account.shared.dto.CreditCard;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;

/**
 * Process payments and credit cards
 *
 * @author Alexander Garagatyi
 */
public interface PaymentService {
    /**
     * Purchase subscription. User should have a credit card to use this method.
     *
     * @param userId
     *         user id to identify user that makes purchasing
     * @param subscriptionId
     *         id of the subscription a user pays for
     * @throws ConflictException
     * @throws ApiException
     */
    void purchase(String userId, String subscriptionId) throws ApiException;

    /**
     * Retrieve stored credit card
     *
     * @param userId
     *         id of the user for card retrieving
     * @return stored credit card
     * @throws NotFoundException
     *         if there is no credit card
     * @throws ApiException
     */
    CreditCard getCreditCard(String userId) throws ApiException;

    /**
     * Store credit card in the storage
     *
     * @param userId
     *         id of the user to store credit card
     * @param creditCard
     *         credit card details to store
     * @throws ConflictException
     *         if user already have credit card
     * @throws ApiException
     */
    void saveCreditCard(String userId, CreditCard creditCard) throws ApiException;

    /**
     * Remove current credit card of the user
     *
     * @param userId
     * @throws ApiException
     */
    void removeCreditCard(String userId) throws ApiException;
}