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

import com.codenvy.api.account.shared.dto.CreditCardDescriptor;
import com.codenvy.api.account.shared.dto.NewCreditCard;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

/**
 * Process payments and credit cards.
 *
 * @author Alexander Garagatyi
 */
public interface PaymentService {
    /**
     * Purchases subscription. User should have a credit card to use this method.
     *
     * @param userId
     *         id to identify user that makes purchasing
     * @param subscriptionId
     *         id of the subscription for which the user pays
     * @throws ConflictException
     *         if the subscription is not found; payment is not required; if user has no stored credit card
     * @throws ServerException
     *         if internal server error occurs
     */
    void purchase(String userId, String subscriptionId) throws ConflictException, ServerException;

    /**
     * Retrieves stored credit card.
     *
     * @param userId
     *         id of the user for card retrieving
     * @return saved credit card
     * @throws NotFoundException
     *         if user's credit card is not found
     * @throws ServerException
     *         if internal server error occurs
     */
    CreditCardDescriptor getCreditCard(String userId) throws ServerException, NotFoundException;

    /**
     * Saves credit card in the storage. User can have only 1 credit card at once.
     *
     * @param userId
     *         id of the user who saves credit card
     * @param creditCard
     *         credit card details
     * @throws ConflictException
     *         if user has credit card already
     * @throws ServerException
     *         if internal server error occurs
     */
    void saveCreditCard(String userId, NewCreditCard creditCard) throws ConflictException, ServerException;

    /**
     * Removes current credit card of the user.
     *
     * @param userId
     *         if of user to remove credit card
     * @throws ServerException
     *         if internal server error occurs
     */
    void removeCreditCard(String userId) throws ServerException;
}