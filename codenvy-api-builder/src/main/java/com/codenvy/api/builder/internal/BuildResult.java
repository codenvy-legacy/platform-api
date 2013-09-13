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
package com.codenvy.api.builder.internal;

import com.codenvy.api.core.rest.FileAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents result of build or analysis dependencies process.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class BuildResult {
    private final boolean success;

    private List<FileAdapter> artifacts;
    private FileAdapter       report;

    public BuildResult(boolean success, List<FileAdapter> artifacts, FileAdapter report) {
        this.success = success;
        if (artifacts != null) {
            this.artifacts = new ArrayList<>(artifacts);
        } else {
            this.artifacts = null;
        }
        this.report = report;
    }

    public BuildResult(boolean success, List<FileAdapter> artifacts) {
        this(success, artifacts, null);
    }

    public BuildResult(boolean success, FileAdapter report) {
        this(success, null, report);
    }

    public BuildResult(boolean success) {
        this(success, null, null);
    }

    /**
     * Reports whether build process successful or failed.
     *
     * @return {@code true} if build successful and {@code false} otherwise
     */
    public boolean isSuccessful() {
        return success;
    }

    /** Build artifacts or {@code null} if build failed or there is no any result of build process. */
    public List<FileAdapter> getResultUnits() {
        if (artifacts == null) {
            artifacts = new ArrayList<>();
        }
        return artifacts;
    }

    /**
     * Reports whether build report is available or not. In case if this method returns {@code false} method {@link #getBuildReport()}
     * always returns {@code null}.
     *
     * @return {@code true} if build report is available and {@code false} otherwise
     */
    public boolean hasBuildReport() {
        return null != report;
    }

    /**
     * Provides report about build process. If {@code Builder} does not support reports or report for particular build is not available
     * this method always returns {@code null}.
     *
     * @return report about build or {@code null}
     */
    public FileAdapter getBuildReport() {
        return report;
    }

    public void setBuildReport(FileAdapter report) {
        this.report = report;
    }
}
