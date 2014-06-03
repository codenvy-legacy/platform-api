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
package com.codenvy.api.factory.parameter;

import com.codenvy.api.factory.FactoryUrlException;
import com.codenvy.api.factory.dto.Factory;

/**
 * Remove 'wname' parameter from factory.
 *
 * @author Alexander Garagatyi
 */
public class WorkspacNameConverter implements LegacyConverter {
    @Override
    public void convert(Factory factory) throws FactoryUrlException {
        factory.setWname(null);
    }
}
