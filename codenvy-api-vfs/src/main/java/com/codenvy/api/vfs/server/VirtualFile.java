/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package com.codenvy.api.vfs.server;

import com.codenvy.api.core.util.Pair;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Item of Virtual Filesystem.
 *
 * @author andrew00x
 */
public interface VirtualFile extends Comparable<VirtualFile> {
    /**
     * Get unique id.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String getId() throws VirtualFileSystemException;

    /**
     * Get name.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String getName() throws VirtualFileSystemException;

    /**
     * Get path. Path of root folder is "/".
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String getPath() throws VirtualFileSystemException;

    /** Get internal representation of path of item. */
    Path getVirtualFilePath() throws VirtualFileSystemException;

    /**
     * Tests whether this VirtualFile exists.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    boolean exists() throws VirtualFileSystemException;

    /**
     * Tests whether this VirtualFile is a root folder.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    boolean isRoot() throws VirtualFileSystemException;

    /**
     * Tests whether this VirtualFile is a regular file.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    boolean isFile() throws VirtualFileSystemException;

    /**
     * Tests whether this VirtualFile is a folder. Folder may contain other files.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    boolean isFolder() throws VirtualFileSystemException;

    /**
     * Get parent folder. If this folder is root folder this method always returns <code>null</code>.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     * @see #isRoot()
     */
    VirtualFile getParent() throws VirtualFileSystemException;

    /**
     * Get iterator over files in this folder. If this VirtualFile is not folder this method returns empty iterator. If current user has
     * not read access to some child they should not be included in returned result.
     *
     * @param filter
     *         virtual files filter
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    LazyIterator<VirtualFile> getChildren(VirtualFileFilter filter) throws VirtualFileSystemException;

    /**
     * Get child by name. If this VirtualFile is not folder this method returns <code>null</code>.
     *
     * @param path
     *         child item path
     * @return child
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if current user has not read permission to the child
     * @throws VirtualFileSystemException
     *         if other error occurs
     */
    VirtualFile getChild(String path) throws VirtualFileSystemException;

    /**
     * Get content of the file.
     *
     * @return content ot he file
     * @throws VirtualFileSystemException
     *         if this VirtualFile denotes folder or other error occurs
     * @see #isFile()
     */
    ContentStream getContent() throws VirtualFileSystemException;

    /**
     * Update content of the file.
     *
     * @param mediaType
     *         media type of content
     * @param content
     *         content
     * @param lockToken
     *         lock token. This parameter is required if the file is locked
     * @return VirtualFile after updating content
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         if item <code>id</code> is locked and <code>lockToken</code> is <code>null</code> or does not matched
     * @throws VirtualFileSystemException
     *         if this VirtualFile denotes folder or other error occurs
     * @see #isFile()
     */
    VirtualFile updateContent(String mediaType, InputStream content, String lockToken) throws VirtualFileSystemException;

    /**
     * Get media type of the VirtualFile. This method should not return <code>null</code>.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String getMediaType() throws VirtualFileSystemException;

    /**
     * Set media type of the VirtualFile.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    VirtualFile setMediaType(String mediaType) throws VirtualFileSystemException;

    /**
     * Get creation time in long format or <code>-1</code> if creation time is unknown.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    long getCreationDate() throws VirtualFileSystemException;

    /**
     * Get time of last modification in long format or <code>-1</code> if time is unknown.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    long getLastModificationDate() throws VirtualFileSystemException;

    /**
     * Get length of content of the file. Always returns <code>0</code> for folders.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    long getLength() throws VirtualFileSystemException;

    /**
     * Get properties of the file.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     * @see PropertyFilter
     */
    List<Property> getProperties(PropertyFilter filter) throws VirtualFileSystemException;

    /**
     * Update properties of the file.
     *
     * @param properties
     *         list of properties to update
     * @param lockToken
     *         lock token. This parameter is required if the file is locked
     * @return VirtualFile after updating properties
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         if item <code>id</code> is locked and <code>lockToken</code> is <code>null</code> or does not matched
     * @throws VirtualFileSystemException
     *         if other error occurs
     */
    VirtualFile updateProperties(List<Property> properties, String lockToken) throws VirtualFileSystemException;

    /**
     * Get value of property. If property has multiple values this method returns the first value in the set.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     * @see #getPropertyValues(String)
     */
    String getPropertyValue(String name) throws VirtualFileSystemException;

    /**
     * Get multiple values of property.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String[] getPropertyValues(String name) throws VirtualFileSystemException;

    /**
     * Copy this file to the new parent.
     *
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission to the specified <code>parent</code>
     * @throws VirtualFileSystemException
     *         if the specified <code>parent</code> does not denote a folder or other error occurs
     * @see #isFolder()
     */
    VirtualFile copyTo(VirtualFile parent) throws VirtualFileSystemException;

    /**
     * Move this file to the new parent.
     *
     * @param parent
     *         parent to copy
     * @param lockToken
     *         lock token. This parameter is required if the file is locked
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission to the specified <code>parent</code> or this VirtualFile (include any of its children)
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         if this VirtualFile is regular locked file and <code>lockToken</code> is invalid or if this VirtualFile is folder and
     *         contains at least one locked child
     * @throws com.codenvy.api.vfs.server.exceptions.ItemAlreadyExistException
     *         if destination folder already contains item this the same name as this VirtualFile
     * @throws VirtualFileSystemException
     *         if the specified <code>parent</code> does not denote a folder or other error occurs
     * @see #isFolder()
     */
    VirtualFile moveTo(VirtualFile parent, String lockToken) throws VirtualFileSystemException;

    /**
     * Rename and (or) update media type of this VirtualFile.
     *
     * @param newName
     *         new name
     * @param newMediaType
     *         new media type, may be <code>null</code> if need change name only
     * @param lockToken
     *         lock token. This parameter is required if the file is locked
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission for this VirtualFile (include any of its children)
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         if this VirtualFile is regular locked file and <code>lockToken</code> is invalid or if this VirtualFile is folder and
     *         contains at least one locked child
     * @throws VirtualFileSystemException
     *         if other error occurs
     */
    VirtualFile rename(String newName, String newMediaType, String lockToken) throws VirtualFileSystemException;

    /**
     * Delete this VirtualFile.
     *
     * @param lockToken
     *         lock token. This parameter is required if the file is locked
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission for this VirtualFile (include any of its children)
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         if this VirtualFile is regular locked file and <code>lockToken</code> is invalid or if this VirtualFile is folder and
     *         contains at least one locked child
     * @throws VirtualFileSystemException
     *         if other error occurs
     */
    void delete(String lockToken) throws VirtualFileSystemException;

    /**
     * Get zipped content of folder denoted by this VirtualFile.
     *
     * @param filter
     *         filter of file. Only files that are matched to the filter are added in the zip archive
     * @return zipped content of folder denoted by this VirtualFile
     * @throws IOException
     *         if i/o error occurs
     * @throws VirtualFileSystemException
     *         if this VirtualFile does not denote a folder or other error occurs
     */
    ContentStream zip(VirtualFileFilter filter) throws IOException, VirtualFileSystemException;

    /**
     * Import ZIP content to the folder denoted by this VirtualFile.
     *
     * @param zipped
     *         ZIP content
     * @param overwrite
     *         overwrite or not existing files
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission for this VirtualFile (include any of its children)
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         this folder contains at least one locked child
     * @throws com.codenvy.api.vfs.server.exceptions.ItemAlreadyExistException
     *         if <code>overwrite</code> is <code>false</code> and any item in zipped content conflicts with existed item
     * @throws IOException
     *         if i/o error occurs
     * @throws VirtualFileSystemException
     *         if this VirtualFile does not denote a folder or other error occurs
     */
    void unzip(InputStream zipped, boolean overwrite) throws IOException, VirtualFileSystemException;

    /**
     * Lock this VirtualFile.
     *
     * @param timeout
     *         lock timeout in milliseconds, pass <code>0</code> to create lock without timeout
     * @return lock token. User should pass this token when tries update, delete or unlock locked file
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission for this VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         this VirtualFile is already locked
     * @throws com.codenvy.api.vfs.server.exceptions.NotSupportedException
     *         if locking feature is not supported
     * @throws VirtualFileSystemException
     *         if this VirtualFile does not denote a regular file or other error occurs
     */
    String lock(long timeout) throws VirtualFileSystemException;

    /**
     * Unlock this VirtualFile.
     *
     * @param lockToken
     *         lock token
     * @return VirtualFile after unlock
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         this VirtualFile is not locked or specified <code>lockToken</code> is invalid
     * @throws com.codenvy.api.vfs.server.exceptions.NotSupportedException
     *         if locking feature is not supported
     * @throws VirtualFileSystemException
     *         if this VirtualFile does not denote a regular file or other error occurs
     */
    VirtualFile unlock(String lockToken) throws VirtualFileSystemException;

    /**
     * Tests whether this VirtualFile is locked.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    boolean isLocked() throws VirtualFileSystemException;

    /**
     * Get permissions of this VirtualFile.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    Map<Principal, Set<BasicPermissions>> getPermissions() throws VirtualFileSystemException;

    /**
     * Get ACL.
     *
     * @return ACL
     * @throws com.codenvy.api.vfs.server.exceptions.NotSupportedException
     *         if ACL feature is not supported
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    List<AccessControlEntry> getACL() throws VirtualFileSystemException;

    /**
     * Update ACL.
     *
     * @param acl
     *         ACL
     * @param override
     *         if <code>true</code> clear old ACL and apply new ACL, otherwise merge existed ACL and new one
     * @param lockToken
     *         lock token. This parameter is required if the file is locked
     * @return VirtualFile after updating ACL
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not update_acl permission for this VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.LockException
     *         this VirtualFile is locked and <code>lockToken</code> invalid
     * @throws com.codenvy.api.vfs.server.exceptions.NotSupportedException
     *         if managing ACL feature is not supported
     * @throws VirtualFileSystemException
     *         if other error occurs
     */
    VirtualFile updateACL(List<AccessControlEntry> acl, boolean override, String lockToken) throws VirtualFileSystemException;

    /**
     * Get unique id of version of this VirtualFile.
     *
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String getVersionId() throws VirtualFileSystemException;

    /**
     * Get all versions of this VirtualFile. If versioning is not supported this iterator always contains just one item which denotes this
     * VirtualFile.
     *
     * @param filter
     *         virtual files filter
     * @return iterator over all versions
     * @throws VirtualFileSystemException
     *         if this VirtualFile is not regular file or other error occurs
     * @see #isFile()
     */
    LazyIterator<VirtualFile> getVersions(VirtualFileFilter filter) throws VirtualFileSystemException;

    /**
     * Get single version of VirtualFile. If versioning is not supported this method should return <code>this</code> instance if specified
     * <code>versionId</code> equals to the value returned by method {@link #getVersionId()}. If versioning is not supported and
     * <code>versionId</code> is not equals to the version id of this file {@link com.codenvy.api.vfs.server.exceptions.NotSupportedException}
     * should be thrown.
     *
     * @param versionId
     *         id of version
     * @return single version of VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.InvalidArgumentException
     *         if there is no version with <code>versionId</code>
     * @throws com.codenvy.api.vfs.server.exceptions.NotSupportedException
     *         if versioning is not supported and <code>versionId</code> is not equals to the version id of this VirtualFile
     * @throws VirtualFileSystemException
     *         if this VirtualFile is not regular file or other error occurs
     * @see #isFile()
     */
    VirtualFile getVersion(String versionId) throws VirtualFileSystemException;

    /**
     * Create new VirtualFile which denotes regular file and use this one as parent folder.
     *
     * @param name
     *         name
     * @param mediaType
     *         media type of content, may be {@code null}
     * @param content
     *         content. In case of {@code null} empty file's created.
     * @return newly create VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission for this VirtualFile
     * @throws VirtualFileSystemException
     *         if this VirtualFile does not denote a folder or other error occurs
     */
    VirtualFile createFile(String name, String mediaType, InputStream content) throws VirtualFileSystemException;

    /**
     * Create new VirtualFile which denotes folder and use this one as parent folder.
     *
     * @param name
     *         name
     * @return newly create VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user has not write permission for this VirtualFile
     * @throws VirtualFileSystemException
     *         if this VirtualFile does not denote a folder or other error occurs
     */
    VirtualFile createFolder(String name) throws VirtualFileSystemException;

    /**
     * Get {@link MountPoint} to which this VirtualFile belongs.
     *
     * @return MountPoint
     */
    MountPoint getMountPoint();

    /**
     * Accepts an <code>VirtualFileVisitor</code>. Calls the <code>VirtualFileVisitor</code> <code>visit</code> method.
     *
     * @param visitor
     *         VirtualFileVisitor to be accepted
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    void accept(VirtualFileVisitor visitor) throws VirtualFileSystemException;

    /**
     * Traverse recursively all files in current folder and count md5sum for each file. Method returns
     * <code>Pair&lt;String, String&gt;</code> for each file, all folders are omitted. Each <code>Pair</code> contains following structure:
     * <pre>
     *     Pair&lt;String,String&gt; pair = ...
     *     pair.first // md5sum of file represented as HEX String
     *     pair.second // Path of file that is relative to this file
     * </pre>
     * If this VirtualFile is not a folder this method returns empty iterator. Note: any order of items in the returned iterator is not
     * guaranteed.
     *
     * @throws VirtualFileSystemException
     *         if any error occurs
     */
    LazyIterator<Pair<String, String>> countMd5Sums() throws VirtualFileSystemException;
}
