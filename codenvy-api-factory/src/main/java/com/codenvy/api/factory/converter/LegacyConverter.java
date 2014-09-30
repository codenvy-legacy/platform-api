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
package com.codenvy.api.factory.converter;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.factory.dto.Factory;

/**
 * Convert legacy factory parameter to new the latest format
 *
 * @author Alexander Garagatyi
 */
public interface LegacyConverter {
    void convert(Factory factory) throws ApiException;

    void convertToV1_2(Factory factory) throws ApiException;
}
