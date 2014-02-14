/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;

/**
 * @author andrew00x
 */
public class ProjectFileTest {

    private VirtualFile root;

    @BeforeMethod
    public void setUp() throws Exception {
        MemoryMountPoint mmp = new MemoryMountPoint(null, VirtualFileSystemUserContext.newInstance());
        root = mmp.getRoot();
    }

    @Test
    public void test1() throws Exception {
        VirtualFile myProject = root.createFolder("my_project");
        VirtualFile vf = myProject.createFile("test", "text/plain", new ByteArrayInputStream("to be or not to be".getBytes()));
        myProject.createFolder(".codenvy").createFile(".project", null, null);
        ProjectFile pf = new ProjectFile(vf);
        System.err.println(pf.getPath());
        System.err.println(pf.getName());
        System.err.println(pf.getParentFolder());
        System.err.println(pf.getProject().getName());
        System.err.println(pf.getMediaType());
        System.err.println(new String(pf.contentAsBytes()));
    }
}
