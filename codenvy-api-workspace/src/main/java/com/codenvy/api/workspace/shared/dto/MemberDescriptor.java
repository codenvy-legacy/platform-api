package com.codenvy.api.workspace.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author Eugene Voevodin
 */
@DTO
public interface MemberDescriptor {

    String getUserId();

    void setUserId(String userId);

    MemberDescriptor withUserId(String userId);

    WorkspaceReference getWorkspaceReference();

    void setWorkspaceReference(WorkspaceReference wsRef);

    MemberDescriptor withWorkspaceReference(WorkspaceReference wsRef);

    List<String> getRoles();

    void setRoles(List<String> roles);

    MemberDescriptor withRoles(List<String> roles);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    MemberDescriptor withLinks(List<Link> links);
}
