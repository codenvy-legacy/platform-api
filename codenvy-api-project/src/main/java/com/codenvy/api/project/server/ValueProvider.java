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
package com.codenvy.api.project.server;

import java.util.List;

/**
 * Provides access to the value of attribute of Project.
 *
 * @author andrew00x
 */
public interface ValueProvider {

    /** Gets value. */
    List<String> getValues() throws ValueStorageException;

    /** Sets value. */
    void setValues(List<String> value) throws ValueStorageException, InvalidValueException;
}
