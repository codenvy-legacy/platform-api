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
package com.codenvy.api.project.server;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.project.shared.*;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
public class Project {

    private final String         workspace;
    private final FolderEntry    baseFolder;
    private final ProjectManager manager;

    public Project(String workspace, FolderEntry baseFolder, ProjectManager manager) {
        this.workspace = workspace;
        this.baseFolder = baseFolder;
        this.manager = manager;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getName() {
        return baseFolder.getName();
    }

    public String getPath() {
        return baseFolder.getPath();
    }

    public FolderEntry getBaseFolder() {
        return baseFolder;
    }

    public long getModificationDate() throws ServerException {
        return getMisc().getModificationDate();
    }

    public long getCreationDate() throws ServerException {
        return getMisc().getCreationDate();
    }

    public ProjectMisc getMisc() throws ServerException {
        return manager.getProjectMisc(this);
    }

    public void saveMisc(ProjectMisc misc) throws ServerException {
        manager.saveProjectMisc(this, misc);
    }

    public final ProjectDescription getDescription() throws ServerException, ValueStorageException {
        // Create copy of original project descriptor.
        // Mainly do that to be independent to type of ValueProvider.
        // Caller gets description of project update some attributes or(and) type of project than caller sends description back with method
        // updateDescriptor.
        return new ProjectDescription(doGetDescription());
    }

    protected ProjectDescription doGetDescription() throws ServerException {
        final ProjectJson projectJson = ProjectJson.load(this);
        final String projectTypeId = projectJson.getProjectTypeId();
        if (projectTypeId == null) {
            return new ProjectDescription();
        }
        ProjectType projectType = manager.getTypeDescriptionRegistry().getProjectType(projectTypeId);
        if (projectType == null) {
            // TODO : Decide how should we treat such situation?
            // For now just show type what is set in configuration of project.
            projectType = new ProjectType(projectTypeId, projectTypeId, projectTypeId);
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        projectDescription.setBuilder(projectJson.getBuilder());
        projectDescription.setRunner(projectJson.getRunner());
        projectDescription.setDefaultBuilderEnvironment(projectJson.getDefaultBuilderEnvironment());
        projectDescription.setDefaultRunnerEnvironment(projectJson.getDefaultRunnerEnvironment());
        projectDescription.setBuilderEnvironmentConfigurations(projectJson.getBuilderEnvironmentConfigurations());
        projectDescription.setRunnerEnvironmentConfigurations(projectJson.getRunnerEnvironmentConfigurations());
        projectDescription.setDescription(projectJson.getDescription());
        final List<Attribute> tmpList = new ArrayList<>();
        // Merge project's attributes.
        // 1. predefined
        for (Attribute attribute : manager.getTypeDescriptionRegistry().getPredefinedAttributes(projectType)) {
            tmpList.add(attribute);
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        // 2. "calculated"
        final ProjectTypeDescription projectTypeDescription = manager.getTypeDescriptionRegistry().getDescription(projectType);
        if (projectTypeDescription != null) {
            for (AttributeDescription attributeDescription : projectTypeDescription.getAttributeDescriptions()) {
                final ValueProviderFactory factory = manager.getValueProviderFactories().get(attributeDescription.getName());
                if (factory != null) {
                    tmpList.add(new Attribute(attributeDescription.getName(), factory.newInstance(this)));
                }
            }
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        // 3. persistent
        for (Map.Entry<String, List<String>> e : projectJson.getAttributes().entrySet()) {
            tmpList.add(new Attribute(e.getKey(), e.getValue()));
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        return projectDescription;
    }

    public final void updateDescription(ProjectDescription projectDescriptionUpdate) throws ServerException, ValueStorageException, InvalidValueException {
        final ProjectDescription thisProjectDescription = doGetDescription();
        final ProjectJson projectJson = new ProjectJson();
        projectJson.setProjectTypeId(projectDescriptionUpdate.getProjectType().getId());
        projectJson.setBuilder(projectDescriptionUpdate.getBuilder());
        projectJson.setRunner(projectDescriptionUpdate.getRunner());
        projectJson.setDefaultBuilderEnvironment(projectDescriptionUpdate.getDefaultBuilderEnvironment());
        projectJson.setDefaultRunnerEnvironment(projectDescriptionUpdate.getDefaultRunnerEnvironment());
        projectJson.setBuilderEnvironmentConfigurations(projectDescriptionUpdate.getBuilderEnvironmentConfigurations());
        projectJson.setRunnerEnvironmentConfigurations(projectDescriptionUpdate.getRunnerEnvironmentConfigurations());
        projectJson.setDescription(projectDescriptionUpdate.getDescription());
        for (Attribute attributeUpdate : projectDescriptionUpdate.getAttributes()) {
            final String attributeName = attributeUpdate.getName();
            Attribute thisAttribute = null;
            if (thisProjectDescription != null) {
                thisAttribute = thisProjectDescription.getAttribute(attributeName);
            }
            if (thisAttribute == null) {
                final ValueProviderFactory valueProviderFactory = manager.getValueProviderFactories().get(attributeName);
                if (valueProviderFactory == null) {
                    // New attribute without special behaviour - setPreferences it in properties.
                    projectJson.getAttributes().put(attributeName, attributeUpdate.getValues());
                } else {
                    thisAttribute = new Attribute(attributeName, valueProviderFactory.newInstance(this));
                    thisAttribute.setValues(attributeUpdate.getValues());
                }
            } else {
                // Don't store attributes as properties of project if have specific ValueProvider.
                if (manager.getValueProviderFactories().get(attributeName) == null) {
                    // If don't have special ValueProvider then store attribute as project's property.
                    projectJson.getAttributes().put(attributeName, attributeUpdate.getValues());
                } else {
                    thisAttribute.setValues(attributeUpdate.getValues());
                }
            }
        }
        projectJson.save(this);
    }

    public String getVisibility() throws ServerException {
        final List<AccessControlEntry> acl = baseFolder.getVirtualFile().getACL();
        if (acl.isEmpty()) {
            return "public";
        }
        final Principal guest = DtoFactory.getInstance().createDto(Principal.class).withName("any").withType(Principal.Type.USER);
        for (AccessControlEntry ace : acl) {
            if (guest.equals(ace.getPrincipal()) && ace.getPermissions().contains("read")) {
                return "public";
            }
        }
        return "private";
    }

    public void setVisibility(String projectVisibility) throws ServerException, ForbiddenException {
        switch (projectVisibility) {
            case "private":
                final List<AccessControlEntry> acl = new ArrayList<>(1);
                final Principal developer = DtoFactory.getInstance().createDto(Principal.class)
                                                      .withName("workspace/developer")
                                                      .withType(Principal.Type.GROUP);
                acl.add(DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                  .withPrincipal(developer)
                                  .withPermissions(Arrays.asList("all")));
                baseFolder.getVirtualFile().updateACL(acl, true, null);
                break;
            case "public":
                // Remove ACL. Default behaviour of underlying virtual filesystem: everyone can read but can't update.
                baseFolder.getVirtualFile().updateACL(Collections.<AccessControlEntry>emptyList(), true, null);
                break;
        }
    }

    static final String   ALL_PERMISSIONS      = BasicPermissions.ALL.value();
    static final String[] ALL_PERMISSIONS_LIST = {BasicPermissions.READ.value(), BasicPermissions.WRITE.value(),
                                                  BasicPermissions.UPDATE_ACL.value(), "build", "run"};

    public List<AccessControlEntry> getPermissions() throws ServerException {
        return getPermissions(baseFolder.getVirtualFile());
    }

    private List<AccessControlEntry> getPermissions(VirtualFile virtualFile) throws ServerException {
        while (virtualFile != null) {
            final List<AccessControlEntry> acl = virtualFile.getACL();
            if (!acl.isEmpty()) {
                for (AccessControlEntry ace : acl) {
                    final List<String> permissions = ace.getPermissions();
                    // replace "all" shortcut with list
                    if (permissions.remove(ALL_PERMISSIONS)) {
                        final Set<String> set = new LinkedHashSet<>(permissions);
                        Collections.addAll(set, ALL_PERMISSIONS_LIST);
                        permissions.clear();
                        permissions.addAll(set);
                    }
                }
                return acl;
            } else {
                virtualFile = virtualFile.getParent();
            }
        }
        return new ArrayList<>(4);
    }

    /**
     * Sets permissions to project.
     *
     * @param acl
     *         list of {@link com.codenvy.api.vfs.shared.dto.AccessControlEntry}
     */
    public void setPermissions(List<AccessControlEntry> acl) throws ServerException, ForbiddenException {
        final VirtualFile virtualFile = baseFolder.getVirtualFile();
        if (virtualFile.getACL().isEmpty()) {
            // Add permissions from closest parent file if project don't have own.
            final List<AccessControlEntry> l = new LinkedList<>();
            l.addAll(acl);
            l.addAll(getPermissions(virtualFile.getParent()));
            virtualFile.updateACL(l, true, null);
        } else {
            baseFolder.getVirtualFile().updateACL(acl, false, null);
        }
    }
}
