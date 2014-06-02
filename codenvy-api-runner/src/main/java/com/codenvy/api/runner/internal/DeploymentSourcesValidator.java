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
package com.codenvy.api.runner.internal;

/**
 * Validates DeploymentSources before running application.
 *
 * @author andrew00x
 * @see com.codenvy.api.runner.internal.DeploymentSources
 * @see com.codenvy.api.runner.internal.Runner#getDeploymentSourcesValidator()
 */
public interface DeploymentSourcesValidator {
    /**
     * Validates application bundle.
     *
     * @param deployment
     *         application bundle for validation
     * @return {@code true} is application is valid and {@code false} otherwise
     */
    boolean isValid(DeploymentSources deployment);
}
