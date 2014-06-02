/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.vfs.server.VirtualFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/** @author andrew00x */
public class ResourceLoaderTest extends MemoryFileSystemTest {
    private String folderId;
    private String folderPath;
    private String fileId;
    private String filePath;

    private String vfsId = MY_WORKSPACE_ID;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile resourceLoaderTestFolder = mountPoint.getRoot().createFolder(name);

        VirtualFile folder = resourceLoaderTestFolder.createFolder("GetResourceTest_FOLDER");
        folder.createFile("file1", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        folderId = folder.getId();
        folderPath = folder.getPath();

        VirtualFile file = resourceLoaderTestFolder.createFile("GetResourceTest_FILE", "text/plain",
                                                                new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
        filePath = file.getPath();
    }

    public void testLoadFileByID() throws Exception {
        URL file = new URI("ide+vfs", '/' + vfsId, fileId).toURL();
        final String expectedURL = "ide+vfs:/" + vfsId + '#' + fileId;
        assertEquals(expectedURL, file.toString());
        byte[] b = new byte[128];
        InputStream in = file.openStream();
        int num = in.read(b);
        in.close();
        assertEquals(DEFAULT_CONTENT, new String(b, 0, num));
    }

    public void testLoadFileByPath() throws Exception {
        URL file = new URI("ide+vfs", '/' + vfsId, filePath).toURL();
        final String expectedURL = "ide+vfs:/" + vfsId + '#' + filePath;
        assertEquals(expectedURL, file.toString());
        byte[] b = new byte[128];
        InputStream in = file.openStream();
        int num = in.read(b);
        in.close();
        assertEquals(DEFAULT_CONTENT, new String(b, 0, num));
    }

    public void testLoadFolderByID() throws Exception {
        URL folder = new URI("ide+vfs", '/' + vfsId, folderId).toURL();
        final String expectedURL = "ide+vfs:/" + vfsId + '#' + folderId;
        assertEquals(expectedURL, folder.toString());
        byte[] b = new byte[128];
        InputStream in = folder.openStream();
        int num = in.read(b);
        in.close();
        assertTrue(num > 0);
        assertEquals("file1\n", new String(b, 0, num));
    }

    public void testLoadFolderByPath() throws Exception {
        URL folder = new URI("ide+vfs", '/' + vfsId, folderPath).toURL();
        final String expectedURL = "ide+vfs:/" + vfsId + '#' + folderPath;
        assertEquals(expectedURL, folder.toString());
        byte[] b = new byte[128];
        InputStream in = folder.openStream();
        int num = in.read(b);
        in.close();
        assertTrue(num > 0);
        assertEquals("file1\n", new String(b, 0, num));
    }
}
