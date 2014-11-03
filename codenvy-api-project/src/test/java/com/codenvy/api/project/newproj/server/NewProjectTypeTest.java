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
package com.codenvy.api.project.newproj.server;


import com.codenvy.api.project.newproj.ProjectType;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author gazarenkov
 */
public class NewProjectTypeTest {

    Injector injector;

    @Before
    public void setUp() throws Exception {

        //MockitoAnnotations.initMocks(this);
        // Bind components
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {

                install(new ProjectApiModule());

                Multibinder<ValueProviderFactory> valueProviderMultibinder = Multibinder.newSetBinder(binder(), ValueProviderFactory.class);
                valueProviderMultibinder.addBinding().to(MyVPFactory.class);

                Multibinder<ProjectType> projectTypesMultibinder = Multibinder.newSetBinder(binder(), ProjectType.class);
                projectTypesMultibinder.addBinding().to(MyProjectType.class);


                bind(ProjectTypeRegistry.class);
//                bind(UserProfileDao.class).toInstance(userProfileDao);
//                bind(SshKeyStore.class).toInstance(sshKeyStore);
//                Multibinder.newSetBinder(binder(), SshKeyUploader.class);
//                Multibinder.newSetBinder(binder(), CredentialsProvider.class);
            }
        });


    }

    @After
    public void tearDown() throws Exception {
//        Assert.assertTrue(IoUtil.deleteRecursive(fsRoot));
//        Assert.assertTrue(IoUtil.deleteRecursive(gitRepo));
    }

    @Test
    public void testFirst() throws Exception {

        ProjectTypeRegistry reg = injector.getInstance(ProjectTypeRegistry.class);

        System.out.println(">>>>>>>>>>>>>>>>>>>>>> "+reg.getProjectType("my").getAttribute("my:var").getValue().getString());
        System.out.println(">>>>>>>>>>>>>>>>>>>>>> "+reg.getProjectType("my").getParents().size());
        System.out.println(">>>>>>>>>>>>>>>>>>>>>> "+reg.getProjectType("my").getAttributes().size());

//        Assert.assertNotNull(folder.getChild("src/hello.c"));
//        Assert.assertNotNull(folder.getChild("README"));
//        Assert.assertNotNull(folder.getChild(".git"));
//
//        Assert.assertEquals("test git importer", new String(readme.contentAsBytes()));
    }
}
