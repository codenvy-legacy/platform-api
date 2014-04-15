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

package com.codenvy.api.workspace.server.dao;

import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Workspace;

import java.util.List;


/**
 * DAO interface offers means to perform CRUD operations with {@link com.codenvy.api.workspace.shared.dto.Workspace} data. The
 * implementation is not
 * required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 */
public interface WorkspaceDao {
    /**
     * Adds workspace to persistent layer.
     *
     * @param workspace
     *         - POJO representation of workspace entity
     * @throws com.codenvy.api.workspace.server.exception.WorkspaceException
     *         if any issue occurred during performing an operation
     */
    void create(Workspace workspace) throws WorkspaceException;

    /**
     * Updates already present in persistent layer workspace.
     *
     * @param workspace
     *         - POJO representation of workspace entity
     * @throws com.codenvy.api.workspace.server.exception.WorkspaceException
     *         if any issue occurred during performing an operation
     */
    void update(Workspace workspace) throws WorkspaceException;

    /**
     * Removes workspace from persistent layer.
     *
     * @param id
     *         - workspace identifier
     * @throws com.codenvy.api.workspace.server.exception.WorkspaceException
     *         if any issue occurred during performing an operation
     */
    void remove(String id) throws WorkspaceException;

    /**
     * Gets workspace from persistent layer.
     *
     * @param id
     *         - workspace identifier
     * @return workspace POJO, or <code>null</code> if nothing is found
     * @throws com.codenvy.api.workspace.server.exception.WorkspaceException
     *         if any issue occurred during performing an operation
     */
    Workspace getById(String id) throws WorkspaceException;

    /**
     * Gets workspace from persistent layer.
     *
     * @param name
     *         - workspace identifier
     * @return workspace POJO, or <code>null</code> if nothing is found
     * @throws com.codenvy.api.workspace.server.exception.WorkspaceException
     *         if any issue occurred during performing an operation
     */
    Workspace getByName(String name) throws WorkspaceException;


    /**
     * Gets workspaces from persistent layer related to specified account.
     *
     * @param accountId
     *         - account identifier
     * @return List of workspaces
     * @throws com.codenvy.api.workspace.server.exception.WorkspaceException
     *         if any issue occurred during performing an operation
     */
    List<Workspace> getByAccount(String accountId) throws WorkspaceException;
}
