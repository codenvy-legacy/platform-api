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
package com.codenvy.api.factory.store;

import com.codenvy.api.factory.AdvancedFactoryUrl;
import com.codenvy.api.factory.FactoryImage;

import java.util.Set;

public class SavedFactoryData {
    private AdvancedFactoryUrl factoryUrl;
    private Set<FactoryImage>  images;

    public SavedFactoryData(AdvancedFactoryUrl factoryUrl, Set<FactoryImage> images) {
        this.factoryUrl = factoryUrl;
        this.images = images;
    }

    public AdvancedFactoryUrl getFactoryUrl() {
        return factoryUrl;
    }

    public void setFactoryUrl(AdvancedFactoryUrl factoryUrl) {
        this.factoryUrl = factoryUrl;
    }

    public Set<FactoryImage> getImages() {
        return images;
    }

    public void setImages(Set<FactoryImage> images) {
        this.images = images;
    }
}
