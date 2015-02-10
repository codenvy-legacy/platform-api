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
package com.codenvy.api.machine.shared;

/**
 * Recipe to create new {@link com.codenvy.api.machine.server.spi.Image}.
 *
 * @author gazarenkov
 */
public interface Recipe {
    RecipeId getId();

    String getType();

    String getScript();
}
