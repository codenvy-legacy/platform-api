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

import com.codenvy.api.account.shared.dto.CreditCard;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;

import java.util.List;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com) on 1/26/15.
 * @version $Id: $
 */
public interface CreditCardDao {

    String clientToken(String accountId) throws ServerException;

    String registerCard(String accountId, String nonce)  throws ServerException,ForbiddenException;

    List<CreditCard> getCards(String accountId)  throws ServerException,ForbiddenException;

    void deleteCard(String accountId, String token)  throws ServerException,ForbiddenException;
}
