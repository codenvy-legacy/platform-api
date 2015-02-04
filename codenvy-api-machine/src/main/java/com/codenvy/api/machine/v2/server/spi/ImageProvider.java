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
import com.codenvy.api.machine.v2.server.InvalidImageException;
import com.codenvy.api.machine.v2.server.InvalidRecipeException;
import com.codenvy.api.machine.v2.server.UnsupportedRecipeException;
import com.codenvy.api.machine.v2.shared.Recipe;

import java.util.Set;

/**
 * Provides instances of {@link Image} in implementation specific way.
 *
 * @author gazarenkov
 */
public interface ImageProvider {
    /**
     * Gets type of image that this provider supports. Must be unique per system.
     *
     * @return type of image that this provider supports
     */
    String getType();

    /**
     * Gets supported recipe types.
     *
     * @return supported recipe types
     * @see com.codenvy.api.machine.v2.shared.Recipe#getType()
     */
    Set<String> getRecipeTypes();

    /**
     * Creates image from scratch.
     *
     * @param recipe
     *         image creation {@link Recipe}
     * @return newly created {@link Image}
     * @throws UnsupportedRecipeException
     *         if specified {@code recipe} is not supported
     * @throws InvalidRecipeException
     *         if {@code recipe} is invalid
     */
    Image createImage(Recipe recipe) throws UnsupportedRecipeException, InvalidRecipeException;


    /**
     * Creates image using implementation specific {@link ImageId}.
     *
     * @param imageId
     *         implementation specific {@link ImageId}
     * @return newly created image
     * @throws InvalidImageException
     *         if recipe is invalid
     * @throws NotFoundException
     *         if image doesn't {@code imageId} exists
     */
    Image createImage(ImageId imageId) throws InvalidImageException, NotFoundException;
}
