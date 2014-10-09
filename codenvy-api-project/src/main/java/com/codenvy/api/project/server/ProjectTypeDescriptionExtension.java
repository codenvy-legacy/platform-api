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

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A {@code ProjectTypeDescriptionExtension} helps register information about project type in {@link ProjectTypeDescriptionRegistry}.
 * <p/>
 * Unlike {@link ProjectTypeExtension} {@code ProjectTypeDescriptionExtension} registers list of ProjectType instead single ProjectType.
 * A {@code ProjectTypeDescriptionExtension} helps maps list of {@code AttributeDescription} to multiple {@code ProjectType}.
 *
 * @author gazarenkov
 */
public interface ProjectTypeDescriptionExtension {
    /** Gets list of ProjectType registered with this {@code ProjectTypeDescriptionExtension}.
     *  Can't be null
     * */
    @Nonnull
    List<ProjectType> getProjectTypes();

    /** Gets list of AttributeDescription that may be defined for project type.
     * Can't be null. In case no attributes description must return empty list.
     * */
    @Nonnull
    List<AttributeDescription> getAttributeDescriptions();
}