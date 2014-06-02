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
package com.codenvy.api.runner;

import java.util.List;

/**
 * Selects the 'best' RemoteRunner from the List according to implementation. RunQueue uses implementation of this interface fo
 * find the 'best' slave-runner for processing incoming request for running application. If more then one slave-runner available then
 * RunQueue collects them (their front-ends which are represented by RemoteRunner) and passes to implementation of this interface.
 * This implementation should select the 'best' one.
 *
 * @author andrew00x
 */
public interface RunnerSelectionStrategy {
    RemoteRunner select(List<RemoteRunner> remoteRunners);
}
