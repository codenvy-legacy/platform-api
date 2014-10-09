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
import java.util.Map;

/**
 * A {@code ProjectTypeExtension} helps register information about project type in {@link ProjectTypeDescriptionRegistry}. One
 * implementation of {@code ProjectTypeExtension} is used to register one project type with its predefined attributes and templates.
 * Information about predefined attributes, templates and icons are optional.
 *
 * @author gazarenkov
 */
public interface ProjectTypeExtension {
    /** Gets ProjectType registered with this {@code ProjectTypeExtension}. */
    ProjectType getProjectType();

    /** Gets list of predefined attributes for ProjectType registered with this {@code ProjectTypeExtension}.
     * Can't be null. In case no attributes description must return empty list.
     * */
    @Nonnull
    List<Attribute> getPredefinedAttributes();

    /** Gets builder configurations. */
    Builders getBuilders();

    /** Gets runner configurations. */
    Runners getRunners();

    /** Gets list of templates for ProjectType registered with this {@code ProjectTypeExtension}. */
    List<ProjectTemplateDescription> getTemplates();
    
    /** Gets map of icons urls for ProjectType registered with this {@code ProjectTypeExtension}. */
    Map<String, String> getIconRegistry();
}