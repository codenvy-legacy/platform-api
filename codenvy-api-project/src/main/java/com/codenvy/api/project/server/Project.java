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
import com.codenvy.api.project.newproj.Attribute2;
import com.codenvy.api.project.newproj.AttributeValue;
import com.codenvy.api.project.newproj.ProjectConfig;
import com.codenvy.api.project.newproj.ProjectType2;
import com.codenvy.api.project.newproj.server.Variable;
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


//    public ProjectConfig getConfig() throws ServerException, ValueStorageException, ProjectTypeConstraintException,
//            InvalidValueException {
//
//        return doGetConfig();
//
//    }


    public ProjectConfig getConfig() throws ServerException, ValueStorageException, ProjectTypeConstraintException,
            InvalidValueException {

        final ProjectJson2 projectJson = ProjectJson2.load(this);

        ProjectType2 type = manager.getProjectTypeRegistry().getProjectType(projectJson.getType());

        if(type == null) {
            throw new ProjectTypeConstraintException("No Project Type registered for: " + projectJson.getType());
        }


        //final List<Attribute2> attributes = new ArrayList<>();

        final Map<String, AttributeValue> attributes = new HashMap<>();

        for(Attribute2 attr : type.getAttributes()) {

            if(attr.isVariable()) {
                Variable var = (Variable) attr;

                //Variable var = (Variable)type.getAttribute(attr.getName());

                //System.out.println("GET CONFIG1 >> "+var.getName()+" "+var.getValue());

                final ValueProviderFactory factory = var.getValueProviderFactory();
                        //manager.getValueProviderFactories().get(attr.getId());

                List <String> val;
                if (factory != null) {

                    val = factory.newInstance(var.getId(), this).getValues();

                    //System.out.println("GET CONFIG2 >> "+var.getName()+" "+val);

                    if(val == null)
                        throw new ProjectTypeConstraintException("Value Provider must not produce NULL value of variable "+var.getId());
                 } else {
                    val = projectJson.getAttributes().get(attr.getName());
                    //val = new AttributeValue(projectJson.getAttributeValues(attr.getName()));
                }

                if(val == null || val.isEmpty()) {
                    if(var.isRequired())
                        throw new ProjectTypeConstraintException("No Value nor ValueProvider defined for required variable "+var.getId());
                    // else just not add it
                } else {
                    //System.out.println(">>>>>>>>>>>>>>");
                    //var.setValue(new AttributeValue(val), this);
                    //System.out.println("GET CONFIG3 >> "+var.getName()+" "+var.getValue()+" "+var.getValueProviderFactory() +" "+new AttributeValue(val).getList());
                    attributes.put(var.getName(), new AttributeValue(val));
                    //System.out.println(">>>>>>>>>>>>>>");
                    //attributes.add(var);
                }

            } else {  // Constant

                attributes.put(attr.getName(), attr.getValue());
                //attributes.add(attr);
            }
        }

        return new ProjectConfig(projectJson.getDescription(), projectJson.getType(),
                attributes, projectJson.getRunners(), projectJson.getBuilders());

    }




//    /**
//     * Gets project meta-information.
//     * @deprecated
//     **/
//    public ProjectDescription getDescription() throws ServerException, ValueStorageException {
//        // Copy attributes after merging to be independent to type of ValueProvider2. ProjectDescription contains attributes that may use
//        // different ValueProviders. After we have all attributes copy them with DefaultValueProvider. Caller gets description of project
//        // update some attributes or(and) type of project than caller sends description back with method updateDescription.
//        final ProjectDescription projectDescription = doGetDescription();
//        final List<Attribute> attributes = projectDescription.getAttributes();
//        final List<Attribute> copy = new ArrayList<>(attributes.size());
//        for (Attribute attribute : attributes) {
//            copy.add(new Attribute(attribute));
//        }
//        projectDescription.clearAttributes();
//        projectDescription.setAttributes(copy);
//        return projectDescription;
//    }
//
//    private ProjectDescription doGetDescription() throws ServerException {
//        final ProjectJson2 projectJson = ProjectJson2.load(this);
//        final String typeId = projectJson.getType();
//        ProjectType projectType;
//        if (typeId == null) {
//            // Treat type as blank type if type is not set in .codenvy/project.json
//            projectType = ProjectType.BLANK;
//        } else {
//            projectType = manager.getTypeDescriptionRegistry().getProjectType(typeId);
//            if (projectType == null) {
//                // Type is unknown but set in codenvy/.project.json
//                projectType = new ProjectType(typeId);
//            }
//        }
//        final ProjectDescription projectDescription =
//                new ProjectDescription(projectType, projectJson.getBuilders(), projectJson.getRunners());
//        projectDescription.setDescription(projectJson.getDescription());
//
//        final List<Attribute> tmp = new LinkedList<>();
//
//        // Merge project's attributes.
//        // 1. predefined
//        for (Attribute attribute : manager.getTypeDescriptionRegistry().getPredefinedAttributes(projectType)) {
//            tmp.add(attribute);
//        }
//        projectDescription.setAttributes(tmp);
//        tmp.clear();
//
//        // 2. "calculated"
//        for (AttributeDescription attributeDescription : manager.getTypeDescriptionRegistry().getAttributeDescriptions(projectType)) {
//            final ValueProviderFactory factory = manager.getValueProviderFactories().get(attributeDescription.getName());
//            if (factory != null) {
//                tmp.add(new Attribute(attributeDescription.getName(), factory.newInstance(this)));
//            }
//        }
//        projectDescription.setAttributes(tmp);
//        tmp.clear();
//
//        // 3. persistent
//        for (Map.Entry<String, List<String>> e : projectJson.getAttributes().entrySet()) {
//            tmp.add(new Attribute(e.getKey(), e.getValue()));
//        }
//        projectDescription.setAttributes(tmp);
//        tmp.clear();
//
//        return projectDescription;
//    }
//
//    /**
//     * Updates project meta-information.
//     * @deprecated
//     *
//     */
//    public final void updateDescription(ProjectDescription update) throws ServerException, ValueStorageException, InvalidValueException {
//        ProjectDescription thisProjectDescription;
//        try {
//            thisProjectDescription = doGetDescription();
//        } catch (ServerException e) { // in case we have problem with reading/parsing project.json file we going to create new one
//            thisProjectDescription = new ProjectDescription();
//        }
//        final ProjectJson2 projectJson = new ProjectJson2();
//        projectJson.setType(update.getProjectType().getId());
//        projectJson.setBuilders(update.getBuilders());
//        projectJson.setRunners(update.getRunners());
//        projectJson.setDescription(update.getDescription());
//        for (Attribute attributeUpdate : update.getAttributes()) {
//            final String attributeName = attributeUpdate.getName();
//            Attribute thisAttribute = thisProjectDescription.getAttribute(attributeName);
//            if (thisAttribute == null) {
//                final ValueProviderFactory valueProviderFactory = manager.getValueProviderFactories().get(attributeName);
//                if (valueProviderFactory == null) {
//                    // New attribute without special behaviour - setPreferences it in properties.
//                    projectJson.getAttributes().put(attributeName, attributeUpdate.getValues());
//                } else {
//                    thisAttribute = new Attribute(attributeName, valueProviderFactory.newInstance(this));
//                    thisAttribute.setValues(attributeUpdate.getValues());
//                }
//            } else {
//                // Don't store attributes as properties of project if have specific ValueProvider2.
//                if (manager.getValueProviderFactories().get(attributeName) == null) {
//                    // If don't have special ValueProvider2 then store attribute as project's property.
//                    projectJson.getAttributes().put(attributeName, attributeUpdate.getValues());
//                } else {
//                    thisAttribute.setValues(attributeUpdate.getValues());
//                }
//            }
//        }
//        projectJson.save(this);
//    }

    public final void updateConfig(ProjectConfig update) throws ServerException, ValueStorageException,
            ProjectTypeConstraintException, InvalidValueException {

//
//        final ProjectJson2 projectJson;
//        if(ProjectJson2.isReadable(this)) {
//            projectJson =
//        }

        final ProjectJson2 projectJson = new ProjectJson2();

        projectJson.setType(update.getTypeId());
        projectJson.setBuilders(update.getBuilders());
        projectJson.setRunners(update.getRunners());
        projectJson.setDescription(update.getDescription());

        ProjectType2 type = manager.getProjectTypeRegistry().getProjectType(update.getTypeId());

        for (String attributeName : update.getAttributes().keySet()) {

            AttributeValue attributeValue = update.getAttributes().get(attributeName);
            Attribute2 definition = type.getAttribute(attributeName);

            if(definition == null) {
                // Silently ignore
                continue;
                //throw new ProjectTypeConstraintException("No attribute " + attributeName + " defined in Project Type " + type.getId());
            }



            if(definition.isVariable()) {
                Variable var = (Variable)definition;

                final ValueProviderFactory valueProviderFactory = var.getValueProviderFactory();
                        //manager.getValueProviderFactories().get(var.getId());

                //AttributeValue val = attributeUpdate.getValue();

                if(attributeValue == null && var.isRequired())
                     throw new ProjectTypeConstraintException("Required attribute value is initialized with null value "+var.getId());

                if(valueProviderFactory != null) {
                    valueProviderFactory.newInstance(var.getId(), this).setValues(attributeValue.getList());
                }
                projectJson.getAttributes().put(definition.getName(), attributeValue.getList());
//                else {
//                    //throw new ProjectTypeConstraintException("Attribute value factory not found for "+var.getId());
//                    //
//                    projectJson.getAttributes().put(definition.getName(), attributeValue.getList());
//                }

                //var.setValue(val);


            } else { // Constant

                // Silently ignore
                //projectJson.getAttributes().put(attributeUpdate.getName(), attributeUpdate.getValue().getList());
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
}
