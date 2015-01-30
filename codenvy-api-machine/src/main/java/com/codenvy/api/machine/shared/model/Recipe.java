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
package com.codenvy.api.machine.shared.model;

import com.codenvy.api.machine.server.MachineRecipe;

/**
 * @author gazarenkov
 *
 *
 *  TODO merge it to one class
 */
public interface Recipe extends MachineRecipe {

    /**
     *
     * @return
     */
    RecipeId getId();

    /**
     * Docker, ...
     * @return
     */
    String getType();

}
