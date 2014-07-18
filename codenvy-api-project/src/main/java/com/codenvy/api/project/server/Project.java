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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
public class Project {

    private static final Set<String> BASIC_PERMISSIONS = new HashSet<>(Arrays.asList("read", "write", "update_acl", "all"));

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

    public String getId() {
        return baseFolder.getId();
    }

    public FolderEntry getBaseFolder() {
        return baseFolder;
    }

    public long getModificationDate() {
        return manager.getProjectMisc(workspace, baseFolder.getPath()).getModificationDate();
    }

    public long getCreationDate() {
        return manager.getProjectMisc(workspace, baseFolder.getPath()).getCreationDate();
    }

    public List<Project> getModules() {
        final List<Project> modules = new ArrayList<>();
        for (FolderEntry child : baseFolder.getChildFolders()) {
            if (child.isProjectFolder()) {
                modules.add(new Project(workspace, child, manager));
            }
        }
        return modules;
    }

    public Project createModule(String name, ProjectDescription projectDescription) throws IOException {
        final FolderEntry projectFolder = baseFolder.createFolder(name);
        final Project module = new Project(workspace, projectFolder, manager);
        module.updateDescription(projectDescription);
        return module;
    }

    public final ProjectDescription getDescription() throws IOException {
        // Create copy of original project descriptor.
        // Mainly do that to be independent to type of ValueProvider.
        // Caller gets description of project update some attributes or(and) type of project than caller sends description back with method
        // updateDescriptor.
        return new ProjectDescription(doGetDescription());
    }

    protected ProjectDescription doGetDescription() throws IOException {
        final ProjectProperties projectProperties = ProjectProperties.load(this);
        final String projectTypeId = projectProperties.getType();
        if (projectTypeId == null) {
            return new ProjectDescription();
        }
        ProjectType projectType = manager.getProjectTypeRegistry().getProjectType(projectTypeId);
        if (projectType == null) {
            // TODO : Decide how should we treat such situation?
            // For now just show type what is set in configuration of project.
            projectType = new ProjectType(projectTypeId, projectTypeId, projectTypeId);
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        final ProjectTypeDescription projectTypeDescription = manager.getTypeDescriptionRegistry().getDescription(projectType);
        final List<Attribute> tmpList = new ArrayList<>();
        // Merge project's attributes.
        // 1. predefined
        for (Attribute attribute : manager.getTypeDescriptionRegistry().getPredefinedAttributes(projectType)) {
            tmpList.add(attribute);
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        // 2. "calculated"
        for (AttributeDescription attributeDescription : projectTypeDescription.getAttributeDescriptions()) {
            final ValueProviderFactory factory = manager.getValueProviderFactories().get(attributeDescription.getName());
            if (factory != null) {
                tmpList.add(new Attribute(attributeDescription.getName(), factory.newInstance(this)));
            }
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        // 3. persistent
        for (ProjectProperty property : projectProperties.getProperties()) {
            tmpList.add(new Attribute(property.getName(), property.getValue()));
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        projectDescription.setDescription(projectProperties.getDescription());
        return projectDescription;
    }

    public final void updateDescription(ProjectDescription projectDescriptionUpdate) throws IOException {
        final ProjectDescription thisProjectDescription = doGetDescription();
        final ProjectProperties projectProperties = new ProjectProperties();
        projectProperties.setType(projectDescriptionUpdate.getProjectType().getId());
        for (Attribute attributeUpdate : projectDescriptionUpdate.getAttributes()) {
            final String attributeName = attributeUpdate.getName();
            Attribute thisAttribute = null;
            if (thisProjectDescription != null) {
                thisAttribute = thisProjectDescription.getAttribute(attributeName);
            }
            if (thisAttribute != null) {
                // Don't store attributes as properties of project if have specific ValueProvider.
                if (manager.getValueProviderFactories().get(attributeName) == null) {
                    // If don't have special ValueProvider then store attribute as project's property.
                    projectProperties.getProperties().add(new ProjectProperty(attributeName, attributeUpdate.getValues()));
                } else {
                    thisAttribute.setValues(attributeUpdate.getValues());
                }
            } else {
                final ValueProviderFactory valueProviderFactory = manager.getValueProviderFactories().get(attributeName);
                if (valueProviderFactory == null) {
                    // New attribute without special behaviour - save it in properties.
                    projectProperties.getProperties().add(new ProjectProperty(attributeName, attributeUpdate.getValues()));
                } else {
                    thisAttribute = new Attribute(attributeName, valueProviderFactory.newInstance(this));
                    thisAttribute.setValues(attributeUpdate.getValues());
                }
            }
        }
        projectProperties.setDescription(projectDescriptionUpdate.getDescription());
        projectProperties.save(this);
    }

    public String getVisibility() {
        try {
            final List<AccessControlEntry> acl = baseFolder.getVirtualFile().getACL();
            if (acl.isEmpty()) {
                return "public";
            }
            final Principal guest = DtoFactory.getInstance().createDto(Principal.class)
                                              .withName("any")
                                              .withType(Principal.Type.USER);
            for (AccessControlEntry ace : acl) {
                if (guest.equals(ace.getPrincipal()) && ace.getPermissions().contains("read")) {
                    return "public";
                }
            }
            return "private";
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void setVisibility(String projectVisibility) {
        try {
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
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public List<AccessControlEntry> getPermissions() {
        //we should use map to join misc and vfs permissions with same principal
        final Map<String, AccessControlEntry> entries = new HashMap<>();
        try {
            for (AccessControlEntry vfsEntry : baseFolder.getVirtualFile().getACL()) {
                entries.put(vfsEntry.getPrincipal().getName(), vfsEntry);
            }
            final ProjectMisc misc = manager.getProjectMisc(workspace, baseFolder.getPath());
            for (String miscACE : misc.getAccessControlList()) {
                final AccessControlEntry entry = DtoFactory.getInstance().createDtoFromJson(miscACE, AccessControlEntry.class);
                final String principalName = entry.getPrincipal().getName();
                if (entries.get(principalName) != null) {
                    entry.getPermissions().addAll(entries.get(principalName).getPermissions());
                }
                entries.put(principalName, entry);
            }
        } catch (VirtualFileSystemException vfsEx) {
            throw new FileSystemLevelException(vfsEx.getMessage(), vfsEx);
        }
        return new ArrayList<>(entries.values());
    }

    /**
     * <p>Sets permissions to project.
     * Given list of permissions can contain either {@link #BASIC_PERMISSIONS}
     * or any custom permissions.
     * Each AccessControlEntry that contains not only {@link #BASIC_PERMISSIONS}
     * will be splited into 2 entries and stored in different places.
     * The first entry with {@link #BASIC_PERMISSIONS} will be stored in project acl,
     * the second one entry with custom permissions will be stored in project misc.</p>
     *
     * @param acl
     *         list of {@link com.codenvy.api.vfs.shared.dto.AccessControlEntry}
     * @throws FileSystemLevelException
     *         when some error occurred while saving basic entries
     * @throws IOException
     *         when some error occurred while saving misc entries
     */
    public void setPermissions(List<AccessControlEntry> acl) throws IOException {
        final Map<Principal, AccessControlEntry> miscEntries = new HashMap<>();
        final Map<Principal, AccessControlEntry> basicEntries = new HashMap<>();
        final DtoFactory dto = DtoFactory.getInstance();
        //split entries on basic and misc
        for (AccessControlEntry entry : acl) {
            requireNotNull(entry, "Permissions");
            requireNotNull(entry.getPrincipal(), "Principal");
            requireNotNull(entry.getPrincipal().getName(), "Principal name");
            requireNotNull(entry.getPrincipal().getType(), "Principal type");
            final Set<String> customPermissions = new HashSet<>();
            final Set<String> basicPermissions = new HashSet<>();
            for (String permission : entry.getPermissions()) {
                if (BASIC_PERMISSIONS.contains(permission)) {
                    basicPermissions.add(permission);
                } else {
                    customPermissions.add(permission);
                }
            }
            final Principal principal = dto.createDto(Principal.class)
                                           .withName(entry.getPrincipal().getName())
                                           .withType(entry.getPrincipal().getType());
            basicEntries.put(principal, dto.createDto(AccessControlEntry.class)
                                           .withPrincipal(principal)
                                           .withPermissions(new ArrayList<>(basicPermissions)));
            if (!customPermissions.isEmpty()) {
                miscEntries.put(principal, dto.createDto(AccessControlEntry.class)
                                              .withPrincipal(principal)
                                              .withPermissions(new ArrayList<>(customPermissions)));
            } else {
                miscEntries.put(principal, null);
            }
        }
        //updating basic permissions
        try {
            final List<AccessControlEntry> existedAcl = baseFolder.getVirtualFile().getACL();
            final Map<Principal, AccessControlEntry> update = new HashMap<>(existedAcl.size());
            for (AccessControlEntry ace : existedAcl) {
                update.put(ace.getPrincipal(), ace);
            }
            //removing entries with empty permissions
            for (AccessControlEntry ace : basicEntries.values()) {
                if (ace.getPermissions().isEmpty()) {
                    update.remove(ace.getPrincipal());
                } else {
                    update.put(ace.getPrincipal(), ace);
                }
            }
            baseFolder.getVirtualFile().updateACL(new ArrayList<>(update.values()), true, null);
        } catch (VirtualFileSystemException vfsEx) {
            throw new FileSystemLevelException(vfsEx.getMessage(), vfsEx);
        }
        //updating misc permissions
        final ProjectMisc misc = manager.getProjectMisc(workspace, baseFolder.getPath());
        for (Map.Entry<Principal, AccessControlEntry> entry : miscEntries.entrySet()) {
            if (entry.getValue() != null) {
                misc.putAccessControlEntry(dto.toJson(entry.getKey()), dto.toJson(entry.getValue()));
            } else {
                misc.putAccessControlEntry(dto.toJson(entry.getKey()), null);
            }
        }
        manager.save(workspace, getName(), misc);
    }

    /**
     * Checks reference is not {@code null}.
     * The main difference with {@link java.util.Objects#requireNonNull(Object, String)} )}
     * is that {@link com.codenvy.api.core.ServerException} will be thrown
     *
     * @param object
     *         reference to check
     * @param name
     *         specified name, will be used in exception message "{name} should not be a null"
     */
    private void requireNotNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(String.format("%s should not be a null", name));
        }
    }
}
