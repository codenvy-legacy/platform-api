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
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.shared.dto.Item;

import java.io.ByteArrayInputStream;

/** @author Vitaliy Guliy */
public class CloneTest extends MemoryFileSystemTest {

    protected static final String DESTINATION_WORKSPACE_ID = "destination-ws";

    private MemoryFileSystemProvider destFileSystemProvider;
    private MemoryMountPoint destMountPoint;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        destFileSystemProvider = new MemoryFileSystemProvider(DESTINATION_WORKSPACE_ID, new EventService(), virtualFileSystemRegistry);
        virtualFileSystemRegistry.registerProvider(DESTINATION_WORKSPACE_ID, destFileSystemProvider);
        destMountPoint = (MemoryMountPoint)destFileSystemProvider.getMountPoint(true);
    }


    protected void tearDown() throws Exception {
        virtualFileSystemRegistry.unregisterProvider(DESTINATION_WORKSPACE_ID);
        super.tearDown();
    }

    public void testCloneFile() throws Exception {
        // create file in 'my-ws'
        VirtualFile rootFolder = mountPoint.getRoot();
        VirtualFile sourceFile = rootFolder.createFile("file-to-clone", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));

        try {
            mountPoint.getVirtualFile(sourceFile.getPath());
        } catch (NotFoundException e) {
            fail("Source file not found.");
        }

        // clone it to 'next-ws'
        VirtualFileSystem sourceVFS = fileSystemProvider.newInstance(null);
        Item item = sourceVFS.clone(sourceFile.getPath(), DESTINATION_WORKSPACE_ID, destMountPoint.getRoot().getPath(), null);
        assertEquals("/file-to-clone", item.getPath());

        // check the result
        try {
            destMountPoint.getVirtualFile(sourceFile.getPath());
        } catch (NotFoundException e) {
            fail("Destination file not found.");
        }
    }

    public void testCloneTree() throws Exception {
        // create below tree in 'my-ws'
        // folder1
        //     folder2
        //         file1
        //     folder3
        //         folder4
        //         file2
        //         file3
        //     folder5
        //     file4
        //     file5

        VirtualFile rootFolder = mountPoint.getRoot();

        VirtualFile folder1 = rootFolder.createFolder("folder1");
            VirtualFile folder2 = folder1.createFolder("folder2");
                VirtualFile file1 = folder2.createFile("file1", "text/plain", new ByteArrayInputStream("file1 text".getBytes()));
            VirtualFile folder3 = folder1.createFolder("folder3");
                VirtualFile folder4 = folder3.createFolder("folder4");
                VirtualFile file2 = folder3.createFile("file2", "text/plain", new ByteArrayInputStream("file2 text".getBytes()));
                VirtualFile file3 = folder3.createFile("file3", "text/plain", new ByteArrayInputStream("file3 text".getBytes()));
            VirtualFile folder5 = folder1.createFolder("folder5");
            VirtualFile file4 = folder1.createFile("file4", "text/plain", new ByteArrayInputStream("file4 text".getBytes()));
            VirtualFile file5 = folder1.createFile("file5", "text/plain", new ByteArrayInputStream("file5 text".getBytes()));

        // clone it to 'next-ws'
        VirtualFileSystem sourceVFS = fileSystemProvider.newInstance(null);
        Item item = sourceVFS.clone(folder1.getPath(), DESTINATION_WORKSPACE_ID, destMountPoint.getRoot().getPath(), null);
        assertEquals("/folder1", item.getPath());

        // check the result
        try {
            destMountPoint.getVirtualFile(folder1.getPath());
            destMountPoint.getVirtualFile(folder2.getPath());
            destMountPoint.getVirtualFile(folder3.getPath());
            destMountPoint.getVirtualFile(folder4.getPath());
            destMountPoint.getVirtualFile(folder5.getPath());

            destMountPoint.getVirtualFile(file1.getPath());
            destMountPoint.getVirtualFile(file2.getPath());
            destMountPoint.getVirtualFile(file3.getPath());
            destMountPoint.getVirtualFile(file4.getPath());
            destMountPoint.getVirtualFile(file5.getPath());
        } catch (NotFoundException e) {
            fail("Destination file not found. " + e.getMessage());
        }
    }

    public void testCloneTreeWithName() throws Exception {
        // create below tree in 'my-ws'
        // folder1
        //     folder2
        //         file1
        //     folder3
        //         folder4
        //         file2
        //         file3
        //     folder5
        //     file4
        //     file5

        VirtualFile rootFolder = mountPoint.getRoot();

        VirtualFile folder1 = rootFolder.createFolder("folder1");
        VirtualFile folder2 = folder1.createFolder("folder2");
        VirtualFile file1 = folder2.createFile("file1", "text/plain", new ByteArrayInputStream("file1 text".getBytes()));
        VirtualFile folder3 = folder1.createFolder("folder3");
        VirtualFile folder4 = folder3.createFolder("folder4");
        VirtualFile file2 = folder3.createFile("file2", "text/plain", new ByteArrayInputStream("file2 text".getBytes()));
        VirtualFile file3 = folder3.createFile("file3", "text/plain", new ByteArrayInputStream("file3 text".getBytes()));
        VirtualFile folder5 = folder1.createFolder("folder5");
        VirtualFile file4 = folder1.createFile("file4", "text/plain", new ByteArrayInputStream("file4 text".getBytes()));
        VirtualFile file5 = folder1.createFile("file5", "text/plain", new ByteArrayInputStream("file5 text".getBytes()));

        VirtualFile destination = destMountPoint.getRoot().createFolder("a/b");
        // clone it to 'next-ws'
        VirtualFileSystem sourceVFS = fileSystemProvider.newInstance(null);
        Item item = sourceVFS.clone(folder1.getPath(), DESTINATION_WORKSPACE_ID, destination.getPath(), "new_name");
        assertEquals("/a/b/new_name", item.getPath());

        Path basePath = destination.getVirtualFilePath().newPath("new_name");

        // check the result
        try {
            destMountPoint.getVirtualFile(basePath.newPath(folder2.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(folder3.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(folder4.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(folder5.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(folder2.getVirtualFilePath().subPath(1)).toString());

            destMountPoint.getVirtualFile(basePath.newPath(file1.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(file2.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(file3.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(file4.getVirtualFilePath().subPath(1)).toString());
            destMountPoint.getVirtualFile(basePath.newPath(file5.getVirtualFilePath().subPath(1)).toString());
        } catch (NotFoundException e) {
            fail("Destination file not found. " + e.getMessage());
        }
    }
}
