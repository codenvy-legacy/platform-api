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
package com.codenvy.api.account.server;

import com.codenvy.api.account.shared.dto.NewSubscriptionAttributes;
import com.codenvy.api.core.ConflictException;

/**
 * Validates attributes of the subscription.
 *
 * @author Alexander Garagatyi
 */
public interface SubscriptionAttributesValidator {
    void validate(NewSubscriptionAttributes subscriptionAttributes) throws ConflictException;
}
