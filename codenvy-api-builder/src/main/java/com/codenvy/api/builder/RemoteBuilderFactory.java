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
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderList;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;

import java.io.IOException;
import java.util.List;

/**
 * Factory for RemoteBuilder. See {@link RemoteBuilder} about usage of this class.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 * @see RemoteBuilder
 */
public class RemoteBuilderFactory extends RemoteServiceDescriptor {

    public RemoteBuilderFactory(String baseUrl) {
        super(baseUrl);
    }

    public RemoteBuilder getRemoteBuilder(String name) throws IOException, RemoteException {
        for (BuilderDescriptor builderDescriptor : getAvailableBuilders()) {
            if (name.equals(builderDescriptor.getName())) {
                return new RemoteBuilder(baseUrl, builderDescriptor, getLinks());
            }
        }
        throw new IllegalStateException(String.format("Invalid builder name %s", name));
    }

    public List<BuilderDescriptor> getAvailableBuilders() throws IOException, RemoteException {
        final Link link = getLink(Constants.LINK_REL_AVAILABLE_BUILDERS);
        if (link == null) {
            throw new IllegalStateException("Unable get URL for retrieving list of remote builders");
        }

        return HttpJsonHelper.request(BuilderList.class, link).getBuilders();
    }
}
