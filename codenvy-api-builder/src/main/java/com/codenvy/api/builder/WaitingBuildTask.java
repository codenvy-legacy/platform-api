/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
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
package com.codenvy.api.builder;

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildStatus;
import com.codenvy.api.builder.internal.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.remote.RemoteBuildTask;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.UnknownRemoteException;
import com.codenvy.api.core.rest.dto.Link;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class WaitingBuildTask {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long                    id;
    private final long                    since;
    private final BaseBuilderRequest      request;
    private final Future<RemoteBuildTask> future;

    private volatile RemoteBuildTask remote;
    private          boolean         cancelled;

    WaitingBuildTask(BaseBuilderRequest request, Future<RemoteBuildTask> future) {
        id = sequence.getAndIncrement();
        since = System.currentTimeMillis();
        this.future = future;
        this.request = request;
    }

    public Long getId() {
        return id;
    }

    public BaseBuilderRequest getRequest() {
        return request;
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public boolean isWaiting() {
        return !future.isDone();
    }

    public long getSince() {
        return since;
    }

    public synchronized void cancel() throws IOException, RemoteException {
        if (cancelled) {
            return;
        }
        if (isWaiting()) {
            cancelled = future.cancel(true);
        } else {
            cancelled = getRemoteTask().cancel().getStatus() == BuildStatus.CANCELLED;
        }
    }

    public BuildTaskDescriptor getDescriptor(ServiceContext restfulRequestContext) throws RemoteException, IOException {
        if (isWaiting()) {
            final List<Link> links = new ArrayList<>(2);
            final UriBuilder servicePathBuilder = restfulRequestContext.getServicePathBuilder();
            links.add(new Link(
                    servicePathBuilder.clone().path(BuilderService.class, "status").build(getId()).toString(),
                    Constants.LINK_REL_GET_STATUS,
                    "GET",
                    MediaType.APPLICATION_JSON));
            links.add(new Link(
                    servicePathBuilder.clone().path(BuilderService.class, "cancel").build(getId()).toString(),
                    Constants.LINK_REL_CANCEL,
                    "POST",
                    MediaType.APPLICATION_JSON));
            return new BuildTaskDescriptor(BuildStatus.IN_QUEUE, links, -1);
        } else if (isCancelled()) {
            return new BuildTaskDescriptor(BuildStatus.CANCELLED, null, -1);
        } else {
            return getRemoteTask().getDescriptor();
        }
    }

    public RemoteBuildTask getRemoteTask() throws IOException, RemoteException {
        RemoteBuildTask myRemote = remote;
        if (myRemote == null) {
            if (!isWaiting()) {
                synchronized (this) {
                    myRemote = remote;
                    if (myRemote == null) {
                        try {
                            myRemote = remote = future.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            final Throwable cause = e.getCause();
                            if (cause instanceof Error) {
                                throw (Error)cause; // lets caller to get Error as is
                            } else if (cause instanceof UnknownRemoteException) {
                                throw (UnknownRemoteException)cause;
                            } else if (cause instanceof RemoteException) {
                                throw (RemoteException)cause;
                            } else if (cause instanceof IOException) {
                                throw (IOException)cause;
                            } else {
                                throw new UnknownRemoteException(cause.getMessage(), cause);
                            }
                        }
                    }
                }
            }
        }
        return myRemote;
    }

    @Override
    public String toString() {
        return "WaitingBuildTask{" +
               "id=" + id +
               ", request=" + request +
               '}';
    }
}
