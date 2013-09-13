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
package com.codenvy.api.builder.internal.remote;

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BuilderDtoTypes;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderList;
import com.codenvy.api.core.rest.HttpHelper;
import com.codenvy.api.core.rest.dto.Link;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.UnknownRemoteException;
import com.codenvy.api.core.rest.dto.JsonDto;

import java.io.IOException;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class RemoteBuilderFactory extends RemoteServiceDescriptor {

    public RemoteBuilderFactory(String baseUrl) {
        super(baseUrl);
    }

    public RemoteBuilder getRemoteBuilder(String name) throws  IOException, RemoteException {
        for (BuilderDescriptor builderDescriptor : getAvailableBuilders()) {
            if (name.equals(builderDescriptor.getName())) {
                return new RemoteBuilder(baseUrl, builderDescriptor, getLinks());
            }
        }
        throw new UnknownRemoteException(String.format("Invalid builder name %s", name));
    }

    public List<BuilderDescriptor> getAvailableBuilders() throws  IOException, RemoteException {
        final Link link = getLink(Constants.LINK_REL_AVAILABLE_BUILDERS);
        if (link == null) {
            throw new UnknownRemoteException("Unable get URL for retrieving list of remote builders");
        }

        final JsonDto dto = HttpHelper.request(link);
        if (dto == null || dto.getType() != BuilderDtoTypes.BUILDER_LIST_TYPE) {
            throw new UnknownRemoteException("Invalid response from remote server");
        }
        return ((BuilderList)dto.cast()).getBuilders();
    }
}
