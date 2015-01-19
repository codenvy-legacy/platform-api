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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.project.server.handlers.CreateModuleHandler;
import com.codenvy.api.project.server.handlers.CreateProjectHandler;
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

        ProjectTypes types = new ProjectTypes(projectJson.getType(), projectJson.getMixinTypes());

        final Map<String, AttributeValue> attributes = new HashMap<>();

        for(ProjectType2 t : types.all) {

            for (Attribute2 attr : t.getAttributes()) {

                if (attr.isVariable()) {
                    Variable var = (Variable) attr;
                    final ValueProviderFactory factory = var.getValueProviderFactory();

                    List<String> val;
                    if (factory != null) {

                        val = factory.newInstance(baseFolder).getValues(var.getName());

                        if (val == null)
                            throw new ProjectTypeConstraintException("Value Provider must not produce NULL value of variable " + var.getId());
                    } else {
                        val = projectJson.getAttributes().get(attr.getName());
                    }

                    if (val == null || val.isEmpty()) {
                        if (var.isRequired())
                            throw new ProjectTypeConstraintException("No Value nor ValueProvider defined for required variable " + var.getId());
                        // else just not add it

                    } else {
                        attributes.put(var.getName(), new AttributeValue(val));

                    }

                } else {  // Constant

                    attributes.put(attr.getName(), attr.getValue());
                }
            }
        }


        Builders builders = (projectJson.getBuilders() == null)?new Builders(types.primary.getDefaultBuilder()):projectJson.getBuilders();
        Runners runners = (projectJson.getRunners() == null)?new Runners(types.primary.getDefaultRunner()):projectJson.getRunners();

        return new ProjectConfig(projectJson.getDescription(), projectJson.getType(),
                attributes, runners, builders, projectJson.getMixinTypes());

    }


    /**
     * Updates Project Config making all necessary validations
     * @param update
     * @throws ServerException
     * @throws ValueStorageException
     * @throws ProjectTypeConstraintException
     * @throws InvalidValueException
     */
    public final void updateConfig(ProjectConfig update) throws ServerException, ValueStorageException,
            ProjectTypeConstraintException, InvalidValueException {

        final ProjectJson2 projectJson = new ProjectJson2();

        ProjectTypes types = new ProjectTypes(update.getTypeId(), update.getMixinTypes());

        // init Provided attributes if any
//        if(ProjectJson2.isReadable(this)) {
//            ProjectJson2 oldJson = ProjectJson2.load(this);
//            ProjectTypes oldTypes = new ProjectTypes(oldJson.getType(), oldJson.getMixinTypes());
//
//            for(ProjectType2 t : types.all) {
//                if(!oldTypes.all.contains(t)) {
//                    for(ValueProviderFactory f : t.getProvidedFactories()) {
//                        f.newInstance(this.baseFolder).setValues();
//                    }
//                }
//            }
//        } else {
//            for (ProjectType2 t : types.all) {
//                for (ValueProviderFactory f : t.getProvidedFactories()) {
//                    f.newInstance(this.baseFolder).init();
//                }
//
//            }
//        }

        projectJson.setType(types.primary.getId());
        projectJson.setBuilders(update.getBuilders());
        projectJson.setRunners(update.getRunners());
        projectJson.setDescription(update.getDescription());

        ArrayList <String> ms = new ArrayList<>();
        ms.addAll(types.mixins.keySet());
        projectJson.setMixinTypes(ms);

        // update attributes
        for (String attributeName : update.getAttributes().keySet()) {

            AttributeValue attributeValue = update.getAttributes().get(attributeName);

            // Try to Find definition in all the types
            Attribute2 definition = null;
            for(ProjectType2 t : types.all) {
                definition = t.getAttribute(attributeName);
                if(definition != null)
                    break;
            }

            // initialize provided attributes
            if(definition != null && definition.isVariable()) {
                Variable var = (Variable)definition;

                final ValueProviderFactory valueProviderFactory = var.getValueProviderFactory();

                if(attributeValue == null && var.isRequired())
                     throw new ProjectTypeConstraintException("Required attribute value is initialized with null value "+var.getId());

                if(valueProviderFactory != null) {
                    valueProviderFactory.newInstance(baseFolder).setValues(var.getName(), attributeValue.getList());
                }
                projectJson.getAttributes().put(definition.getName(), attributeValue.getList());

            }
        }

        for(ProjectType2 t : types.all) {
            for(Attribute2 attr : t.getAttributes()) {
                if(attr.isVariable()) {
                    // check if required variables initialized
//                    if(attr.isRequired() && attr.getValue() == null) {
                    if(!projectJson.getAttributes().containsKey(attr.getName()) && attr.isRequired()) {
                        throw new ProjectTypeConstraintException("Required attribute value is initialized with null value "+attr.getId());

                    }
                } else {
                    // add constants
                    projectJson.getAttributes().put(attr.getName(), attr.getValue().getList());
                }

            }
        }


        // Default builders and runners
        // NOTE we take it from Primary type only (for the time)
        // TODO? let's see for Machine API
        if(projectJson.getBuilders().getDefault() == null)
            projectJson.getBuilders().setDefault(types.primary.getDefaultBuilder());

        if(projectJson.getRunners().getDefault() == null)
            projectJson.getRunners().setDefault(types.primary.getDefaultRunner());


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

    public Project createModule(String name, ProjectConfig moduleConfig, Map<String, String> options)
            throws ConflictException, ForbiddenException, ServerException {

        final FolderEntry moduleFolder = baseFolder.createFolder(name);
        final Project module = new Project(moduleFolder, manager);

        module.updateConfig(moduleConfig);

        final CreateProjectHandler generator = manager.getHandlers().getCreateProjectHandler(moduleConfig.getTypeId());
        if (generator != null) {
            generator.onCreateProject(module.getBaseFolder(), module.getConfig().getAttributes(), options);
        }

        CreateModuleHandler moduleHandler = manager.getHandlers().getCreateModuleHandler(getConfig().getTypeId());
        if (moduleHandler != null) {
            moduleHandler.onCreateModule(module.getBaseFolder(), module.getConfig(), options);
        }
        return module;

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


    private class ProjectTypes {

        ProjectType2 primary;
        Map<String, ProjectType2> mixins = new HashMap<>();
        Set<ProjectType2> all = new HashSet<>();

        ProjectTypes(String pt, List<String> mss) throws ProjectTypeConstraintException {
            if(pt == null)
                throw new ProjectTypeConstraintException("No primary type defined for "+getWorkspace()+" : "+getPath());

            primary = manager.getProjectTypeRegistry().getProjectType(pt);
            if(primary == null)
                throw new ProjectTypeConstraintException("No project type registered for "+pt);
            if(!primary.canBePrimary())
                throw new ProjectTypeConstraintException("Project type "+primary.getId()+" is not allowable to be primary type");
            all.add(primary);

            if(mss == null)
                mss = new ArrayList<>();

            // temporary storage to detect duplicated attributes
            HashMap<String, Attribute2> tmpAttrs = new HashMap<>();
            for(Attribute2 attr : primary.getAttributes()) {
                tmpAttrs.put(attr.getName(), attr);
            }


            for(String m : mss) {
                if(!m.equals(primary.getId())) {

                    ProjectType2 mixin = manager.getProjectTypeRegistry().getProjectType(m);
                    if(mixin == null)
                        throw new ProjectTypeConstraintException("No project type registered for "+m);
                    if(!mixin.canBeMixin())
                        throw new ProjectTypeConstraintException("Project type "+mixin+" is not allowable to be mixin");

                    // detect duplicated attributes
                    for(Attribute2 attr : mixin.getAttributes()) {
                        if(tmpAttrs.containsKey(attr.getName()))
                            throw new ProjectTypeConstraintException("Attribute name conflict. Duplicated attributes detected "+getPath() +
                                    " Attribute "+ attr.getName() + " declared in " + mixin.getId() + " already declared in " +
                                    tmpAttrs.get(attr.getName()).getProjectType());

                        tmpAttrs.put(attr.getName(), attr);
                    }


                    // Silently remove repeated items from mixins if any
                    mixins.put(m, mixin);
                    all.add(mixin);

                }

            }

        }


    }

}
