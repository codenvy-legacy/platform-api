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
package com.codenvy.api.organization.dummy.server;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.project.server.Constants;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.dto.Folder;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;


@Path("project/{ws-name}")
public class DummyProjectService extends Service {

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<ProjectReference> getProjects(@PathParam("ws-name") String workspace) throws
                                                                                      VirtualFileSystemException {
        List<ProjectReference> result = new ArrayList<>();
        result.add(DtoFactory.getInstance().createDto(ProjectReference.class).withName("project1").withUrl("http://codenvy.com/api/project/" + workspace + "/description?name=project1"));
        result.add(DtoFactory.getInstance().createDto(ProjectReference.class).withName("project2").withUrl("http://codenvy.com/api/project/" + workspace + "/description?name=project2"));
        result.add(DtoFactory.getInstance().createDto(ProjectReference.class).withName("project3").withUrl("http://codenvy.com/api/project/" + workspace + "/description?name=project3"));
        return result;
    }
}
