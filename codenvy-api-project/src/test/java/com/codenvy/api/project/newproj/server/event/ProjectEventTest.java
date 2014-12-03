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
package com.codenvy.api.project.newproj.server.event;

import com.codenvy.api.project.newproj.server.AbstractProjectType;
import com.codenvy.api.project.newproj.server.Constant;
import com.codenvy.api.project.server.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author gazarenkov
 */
public class ProjectEventTest {


    private ProjectManager      pm;

    @BeforeMethod
    public void setUp() throws Exception {
//        EventService eventService = new EventService();
//
//
//        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();
//        final MemoryFileSystemProvider memoryFileSystemProvider =
//                new MemoryFileSystemProvider("my_ws", eventService, new VirtualFileSystemUserContext() {
//                    @Override
//                    public VirtualFileSystemUser getVirtualFileSystemUser() {
//                        return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
//                    }
//                }, vfsRegistry);
//
//
//        vfsRegistry.registerProvider("my_ws", memoryFileSystemProvider);
//
//        Set<com.codenvy.api.project.newproj.ProjectType2> pts = new HashSet<>();
//        pts.add(new MyProjectType());
//        ProjectTypeRegistry ptRegistry = new ProjectTypeRegistry(pts);
//
//        pm = new DefaultProjectManager(null, Collections.<ValueProviderFactory>emptySet(), vfsRegistry, eventService, ptRegistry, Collections.<ValueProviderFactory2>emptySet());
//
//        ProjectDescription pd = new ProjectDescription(new ProjectType2("my_project_type", "my project type", "my_category"));
//        pd.setDescription("my test project");
//        pd.setAttributes(Arrays.asList(new Attribute2("my_attribute", "attribute value 1")));
//        pm.createProject("my_ws", "my_project", pd);

    }

    @Test
    public void test1() throws Exception {

    }

    public class MyProjectType extends AbstractProjectType {

        public MyProjectType() {

            super("my", "my type");

            attributes.add(new Constant("my", "const", "Constant", "const_value"));


        }

    }
}
