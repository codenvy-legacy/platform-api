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
 * Selects the 'best' RemoteRunner from the List according to implementation. RunnerManager uses implementation of this interface fo
 * find the 'best' slave-runner for processing incoming request for running application. If more then one slave-runner available then
 * RunnerManager collects them (their front-ends which are represented by RemoteRunner) and passes to implementation of this interface.
 * This implementation should select the 'best' one.
 * <p/>
 * FQN of implementation of this interface must be placed in file META-INF/services/com.codenvy.api.runner.RunnerSelectionStrategy
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface RunnerSelectionStrategy {
    RemoteRunner select(List<RemoteRunner> remoteRunners);
}
