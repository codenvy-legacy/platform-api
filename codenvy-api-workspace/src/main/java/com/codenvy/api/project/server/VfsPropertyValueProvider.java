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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.shared.dto.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class VfsPropertyValueProvider implements PersistentValueProvider {

    private List<String> values;

    public VfsPropertyValueProvider(List<String> values) {
        this.values = new ArrayList<>(values);
    }

    public VfsPropertyValueProvider(String... values) {
        if (values != null) {
            this.values = new ArrayList<>();
            Collections.addAll(this.values, values);
        }
    }

    @Override
    public void store(Project project, VirtualFileSystem vfs) {
        // TODO
    }

    public List<String> getValues() {
        if (values == null) {
            values = new ArrayList<>(2);
        }
        return values;
    }

    public void setValues(List<String> values) {
        if (this.values == null) {
            if (values != null) {
                this.values = values;
            }
        } else {
            this.values.clear();
            if (!(values == null || values.isEmpty())) {
                this.values.addAll(values);
            }
        }
    }
}
