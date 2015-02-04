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
package com.codenvy.api.machine.v2.server.spi;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.machine.shared.model.InvalidRecipeException;
import com.codenvy.api.machine.v2.server.Image;

import java.util.Set;

/**
 * @author gazarenkov
 */
public interface ImageProvider {
    /**
     * Should be unique per system
     * @return
     */
    String getType();

    /**
     * Supported recipe types
     * @return
     */
    Set<String> getRecipeTypes();

    /**
     * Creates Snapshot from scratch
     * @param recipe
     * @return
     * @throws UnsupportedRecipeTypeException
     * @throws InvalidRecipeException
     */
    Image createImage(Recipe recipe) throws UnsupportedRecipeTypeException, InvalidRecipeException;


    /**
     * Imports image from identifiable storage using implementation specific ImageId
     * @param imageId
     * @return
     * @throws InvalidImageException
     * @throws NotFoundException
     */
    Image importImage(ImageId imageId) throws InvalidImageException, NotFoundException;


}
