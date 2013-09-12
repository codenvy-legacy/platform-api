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
package com.codenvy.api.vfs.dto;

import com.codenvy.api.vfs.shared.File;
import com.codenvy.api.vfs.shared.FileImpl;
import com.codenvy.api.vfs.shared.Folder;
import com.codenvy.api.vfs.shared.FolderImpl;
import com.codenvy.api.vfs.shared.Item;
import com.codenvy.api.vfs.shared.ItemImpl;
import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.api.vfs.shared.Link;
import com.codenvy.api.vfs.shared.Project;
import com.codenvy.api.vfs.shared.ProjectImpl;
import com.codenvy.api.vfs.shared.Property;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Restores any type of virtual filesystem items from JSON.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class ItemDto extends ItemImpl implements Item {
    private String            vfsId;
    private String            id;
    private String            name;
    private ItemType          itemType;
    private String            mimeType;
    private String            path;
    private String            parentId;
    private long              creationDate;
    private List<Property>    properties;
    private Map<String, Link> links;
    private Set<String>       permissions;
    //
    private String            versionId;
    private long              length;
    private long              lastModificationDate;
    private boolean           locked;
    //
    private String            projectType;

    public ItemDto() {
        length = -1;
    }

    //

    public File asFile() {
        if (itemType != ItemType.FILE) {
            return null;
        }
        return new FileImpl(vfsId, id, name, path, parentId, creationDate, lastModificationDate, versionId, mimeType, length, locked,
                            properties, links);
    }

    public Folder asFolder() {
        if (itemType == ItemType.FILE) {
            return null;
        }
        return new FolderImpl(vfsId, id, name, mimeType, path, parentId, creationDate, properties, links);
    }


    public Project asProject() {
        if (itemType != ItemType.PROJECT) {
            return null;
        }
        return new ProjectImpl(vfsId, id, name, mimeType, path, parentId, creationDate, properties, links, projectType);
    }

    //

    public String getVfsId() {
        return vfsId;
    }

    public void setVfsId(String vfsId) {
        this.vfsId = vfsId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(long lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    @Override
    public String toString() {
        return "ItemDto{" +
               "vfsId='" + vfsId + '\'' +
               ", id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", itemType=" + itemType +
               ", mimeType='" + mimeType + '\'' +
               ", path='" + path + '\'' +
               ", parentId='" + parentId + '\'' +
               ", creationDate=" + creationDate +
               ", properties=" + properties +
               ", links=" + links +
               ", permissions=" + permissions +
               ", versionId='" + versionId + '\'' +
               ", length=" + length +
               ", lastModificationDate=" + lastModificationDate +
               ", locked=" + locked +
               ", projectType='" + projectType + '\'' +
               '}';
    }
}
