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
package com.codenvy.core.api.config;

/**
 * Essence which may be configured with {@link Configuration}.
 * Usage example:
 * <pre>
 * Configurable configurable = ...
 * Configuration cfg = configurable.getDefaultConfiguration();
 * // update configuration if need
 * configurable.setConfiguration(cfg);
 * </pre>
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface Configurable {
    /**
     * Default configuration. Implementation must always return the same {@code Configuration}. Modification of returned {@code
     * Configuration} must not effect internal {@code Configuration}.
     *
     * @return default configuration
     */
    Configuration getDefaultConfiguration();

    /**
     * Set configuration to be used by this instance.
     *
     * @param configuration
     *         new configuration
     * @throws IllegalStateException
     *         if this method is called at inappropriate time, e.g. if object already initialized and new configuration may not be applied
     */
    void setConfiguration(Configuration configuration);

    /**
     * Get configuration applied to this instance. Modification of returned {@code Configuration} must not effect internal {@code
     * Configuration}.
     *
     * @return configuration applied to this instance
     */
    Configuration getConfiguration();
}
