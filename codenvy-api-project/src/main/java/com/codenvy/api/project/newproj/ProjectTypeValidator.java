package com.codenvy.api.project.newproj;
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
import com.codenvy.api.project.server.InvalidValueException;
import com.codenvy.api.project.shared.*;
import com.codenvy.api.project.shared.dto.AttributeDescriptor;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTypeDescriptor;

/**
 * Utility for validation Project's description against it's ProjectType restrictions
 *
 * @author gazarenkov
 */
public class ProjectTypeValidator {

    public static void validate(ProjectDescriptor project, ProjectTypeDescriptor typeDescriptor) throws InvalidValueException {

        // Do we have all mandatory attributes?
        for(AttributeDescriptor descr : typeDescriptor.getAttributeDescriptors()) {
            if(!project.getAttributes().containsKey(descr.getName()) /*&& it is required */)
                throw new InvalidValueException("No attribute found "+descr.getName());

        }

        // Do not we try to rewrite predefined attributes
//        for(com.codenvy.api.project.shared.Attribute attr : project.getAttributes()) {
//            typeDescriptor.
//            if(attr )
//                throw new InvalidValueException("No attribute found "+descr.getName());
//        }
    }



}
