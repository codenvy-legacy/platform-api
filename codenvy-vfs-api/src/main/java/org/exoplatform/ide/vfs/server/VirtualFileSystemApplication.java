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
package org.exoplatform.ide.vfs.server;

import org.exoplatform.ide.vfs.server.exceptions.ConstraintExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.GitUrlResolveExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.InvalidArgumentExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.ItemAlreadyExistExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.ItemNotFoundExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.LocalPathResolveExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.LockExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.NotSupportedExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.PermissionDeniedExceptionMapper;
import org.exoplatform.ide.vfs.server.exceptions.VirtualFileSystemRuntimeExceptionMapper;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class VirtualFileSystemApplication extends Application {
    private final Set<Object> singletons;

    private final Set<Class<?>> classes;

    public VirtualFileSystemApplication() {
        classes = new HashSet<>(3);
        classes.add(VirtualFileSystemFactory.class);
        classes.add(RequestContextResolver.class);
        classes.add(NoCacheJsonWriter.class);
        singletons = new HashSet<>(11);
        singletons.add(new ContentStreamWriter());
        singletons.add(new ConstraintExceptionMapper());
        singletons.add(new InvalidArgumentExceptionMapper());
        singletons.add(new LockExceptionMapper());
        singletons.add(new ItemNotFoundExceptionMapper());
        singletons.add(new ItemAlreadyExistExceptionMapper());
        singletons.add(new NotSupportedExceptionMapper());
        singletons.add(new PermissionDeniedExceptionMapper());
        singletons.add(new LocalPathResolveExceptionMapper());
        singletons.add(new GitUrlResolveExceptionMapper());
        singletons.add(new VirtualFileSystemRuntimeExceptionMapper());
    }

    /** @see javax.ws.rs.core.Application#getClasses() */
    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    /** @see javax.ws.rs.core.Application#getSingletons() */
    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
