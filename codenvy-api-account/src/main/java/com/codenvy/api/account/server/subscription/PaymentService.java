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
package com.codenvy.api.account.server.subscription;

import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.shared.dto.CreditCard;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

/**
 * Process payments.
 *
 * @author Alexander Garagatyi
 */
public interface PaymentService {
    /**
     * Charge subscription.
     *
     * @param subscription
     *         subscription for which the user pays
     * @throws ServerException
     *         if internal server error occurs
     */
    void charge(Subscription subscription) throws ConflictException, ServerException, ForbiddenException;

    void charge(String creditCardToken, double amount, String account, String paymentDescription) throws ServerException, ForbiddenException;


    /**
     * Retrieve credit card description from payment system
     *
     * @param token CC token
     * @return {@link com.codenvy.api.account.shared.dto.CreditCard} with credit card description
     * @throws NotFoundException
     * @throws ServerException
     */
    CreditCard getCreditCard(String token) throws NotFoundException, ServerException, ForbiddenException;

    void removeCreditCard(String token) throws NotFoundException, ServerException, ForbiddenException;
}