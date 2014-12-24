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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.project.server.handlers.GetItemHandler;
import com.codenvy.api.project.server.type.Attribute2;
import com.codenvy.api.project.server.type.AttributeValue;
import com.codenvy.api.project.server.type.ProjectType2;
import com.codenvy.api.project.server.type.Variable;
import com.codenvy.api.project.shared.Builders;
import com.codenvy.api.project.shared.Runners;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.dto.server.DtoFactory;

import java.util.*;

/**
 * Server side representation for codenvy project.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
public class Project {
    private final FolderEntry    baseFolder;
    private final ProjectManager manager;

    public Project(FolderEntry baseFolder, ProjectManager manager) {
        this.baseFolder = baseFolder;
        this.manager = manager;
    }

    /** Gets id of workspace which this project belongs to. */
    public String getWorkspace() {
        return baseFolder.getWorkspace();
    }

    /** Gets name of project. */
    public String getName() {
        return baseFolder.getName();
    }

    /** Gets path of project. */
    public String getPath() {
        return baseFolder.getPath();
    }

    /** Gets base folder of project. */
    public FolderEntry getBaseFolder() {
        return baseFolder;
    }

    /** Gets creation date of project in unix format or {@code -1} if creation date is unknown. */
    public long getCreationDate() throws ServerException {
        return getMisc().getCreationDate();
    }

    /** Gets most recent modification date of project in unix format or {@code -1} if modification date is unknown. */
    public long getModificationDate() throws ServerException {
        return getMisc().getModificationDate();
    }

    /** @see com.codenvy.api.project.server.ProjectMisc */
    public ProjectMisc getMisc() throws ServerException {
        return manager.getProjectMisc(this);
    }

    /** @see com.codenvy.api.project.server.ProjectMisc */
    public void saveMisc(ProjectMisc misc) throws ServerException {
        manager.saveProjectMisc(this, misc);
    }


    public ProjectConfig getConfig() throws ServerException, ValueStorageException, ProjectTypeConstraintException,
            InvalidValueException {


        final ProjectJson2 projectJson = ProjectJson2.load(this);

        ProjectType2 type = manager.getProjectTypeRegistry().getProjectType(projectJson.getType());

        if(type == null) {
            throw new ProjectTypeConstraintException("No Project Type configured for project : " + this.getPath());
        }


        final Map<String, AttributeValue> attributes = new HashMap<>();

        for(Attribute2 attr : type.getAttributes()) {

            if(attr.isVariable()) {
                Variable var = (Variable) attr;
                final ValueProviderFactory factory = var.getValueProviderFactory();

                List <String> val;
                if (factory != null) {

                    val = factory.newInstance(this).getValues(var.getName());

                    if(val == null)
                        throw new ProjectTypeConstraintException("Value Provider must not produce NULL value of variable "+var.getId());
                 } else {
                    val = projectJson.getAttributes().get(attr.getName());
                }

                if(val == null || val.isEmpty()) {
                    if(var.isRequired())
                        throw new ProjectTypeConstraintException("No Value nor ValueProvider defined for required variable "+var.getId());
                    // else just not add it
                } else {
                    attributes.put(var.getName(), new AttributeValue(val));

                }

            } else {  // Constant

                attributes.put(attr.getName(), attr.getValue());
                //attributes.add(attr);
            }
        }


        Builders builders = (projectJson.getBuilders() == null)?new Builders(type.getDefaultBuilder()):projectJson.getBuilders();
        Runners runners = (projectJson.getRunners() == null)?new Runners(type.getDefaultRunner()):projectJson.getRunners();

        return new ProjectConfig(projectJson.getDescription(), projectJson.getType(),
                attributes, runners, builders, projectJson.getMixinTypes());

    }


    public final void updateConfig(ProjectConfig update) throws ServerException, ValueStorageException,
            ProjectTypeConstraintException, InvalidValueException {


        final ProjectJson2 projectJson = new ProjectJson2();

        projectJson.setType(update.getTypeId());
        projectJson.setBuilders(update.getBuilders());
        projectJson.setRunners(update.getRunners());
        projectJson.setDescription(update.getDescription());

        ProjectType2 type = manager.getProjectTypeRegistry().getProjectType(update.getTypeId());

        for (String attributeName : update.getAttributes().keySet()) {

            AttributeValue attributeValue = update.getAttributes().get(attributeName);
            Attribute2 definition = type.getAttribute(attributeName);

            if(definition != null && definition.isVariable()) {
                Variable var = (Variable)definition;

                final ValueProviderFactory valueProviderFactory = var.getValueProviderFactory();

                if(attributeValue == null && var.isRequired())
                     throw new ProjectTypeConstraintException("Required attribute value is initialized with null value "+var.getId());

                if(valueProviderFactory != null) {
                    valueProviderFactory.newInstance(this).setValues(var.getName(), attributeValue.getList());
                }
                projectJson.getAttributes().put(definition.getName(), attributeValue.getList());

            }
        }

        for(Attribute2 attr : type.getAttributes()) {
            if(attr.isVariable()) {
                // check if required variables initialized
                if(!projectJson.getAttributes().containsKey(attr.getName()) && attr.isRequired()) {
                    throw new ProjectTypeConstraintException("Required attribute value is initialized with null value "+attr.getId());
                }
            } else { // add constants
                projectJson.getAttributes().put(attr.getName(), attr.getValue().getList());
            }

        }

        projectJson.save(this);

    }

    /**
     * Gets visibility of this project, either 'private' or 'public'. Project is considered to be 'public' if any user has read access to
     * it.
     */
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

    /**
     * Updates project privacy.
     *
     * @see #getVisibility()
     */
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

    /**
     * Gets security restriction applied to this project. Method returns empty {@code List} is project doesn't have any security
     * restriction.
     */
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

    public VirtualFileEntry getItem(String path) throws ProjectTypeConstraintException,
             ValueStorageException, ServerException, NotFoundException, ForbiddenException {
        final VirtualFileEntry entry = getVirtualFileEntry(path);
        GetItemHandler handler = manager.getHandlers().getGetItemHandler(getConfig().getTypeId());
        if(handler != null)
            handler.onGetItem(entry);
        //getConfig().getMixinTypes()
        return entry;
    }

    private VirtualFileEntry getVirtualFileEntry(String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry root = manager.getProjectsRoot(this.getWorkspace());
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
        return entry;
    }

}
