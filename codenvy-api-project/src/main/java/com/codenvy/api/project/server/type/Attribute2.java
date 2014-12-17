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
package com.codenvy.api.project.server.type;

import com.codenvy.api.project.server.ValueStorageException;

/**
 * @author gazarenkov
 */
public interface Attribute2 {

    String getId();

    String getName();

    String getProjectType();

    String getDescription();

    boolean isRequired();

    boolean isVariable();

    AttributeValue getValue() throws ValueStorageException;

}
