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
package com.codenvy.api.vfs.server.search;

import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemRuntimeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SearcherProvider for Lucene based Searchers.
 *
 * @author andrew00x
 */
public abstract class LuceneSearcherProvider implements SearcherProvider {

    @Override
    public abstract Searcher getSearcher(MountPoint mountPoint, boolean create) throws VirtualFileSystemException;

    /** Get list of media type of virtual files which must be indexed. */
    protected Set<String> getIndexedMediaTypes() {
        Set<String> forIndex = null;
        final URL url = Thread.currentThread().getContextClassLoader().getResource("META-INF/indices_types.txt");
        if (url != null) {
            InputStream in = null;
            BufferedReader reader = null;
            try {
                in = url.openStream();
                reader = new BufferedReader(new InputStreamReader(in));
                forIndex = new LinkedHashSet<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    int c = line.indexOf('#');
                    if (c >= 0) {
                        line = line.substring(0, c);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        forIndex.add(line);
                    }
                }
            } catch (IOException e) {
                throw new VirtualFileSystemRuntimeException(
                        String.format("Failed to get list of media types for indexing. %s", e.getMessage()));
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        if (forIndex == null || forIndex.isEmpty()) {
            throw new VirtualFileSystemRuntimeException("Failed to get list of media types for indexing. " +
                                                        "File 'META-INF/indices_types.txt not found or empty. ");
        }
        return forIndex;
    }

}
