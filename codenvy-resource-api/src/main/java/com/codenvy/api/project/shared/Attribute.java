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
package com.codenvy.api.project.shared;

import com.codenvy.api.resources.shared.AttributeProvider;
import com.codenvy.api.resources.shared.AttributeProviderRegistry;

/**
 * Attribute of Resource.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see AttributeProvider
 * @see AttributeProviderRegistry
 */

public class Attribute {
	
	private AttributeValueProvider valueProvider;
	private String name;
	
	public Attribute(String name, AttributeValueProvider valueProvider) {
		this.name = name;
		this.valueProvider = valueProvider;
	}
	
	
	public Attribute(Property property) {
		this.name = property.getName();
		this.valueProvider = new DefaultValueProvider(property);
	}
	
    /** Name of attribute. */
    public String getName() {
    	return name;
    }


    /**
     * Get value of attribute.
     *
     * @return current value of attribute
     */
    public final String getValue() {
    	return valueProvider.getValue();
    }

    /**
     * Get value of attribute.
     *
     * @param value
     *         new value of attribute
     * @see #isUpdated()
     */
    public final void setValue(String value) {
    	valueProvider.setValue(value);
    }
    
    private static class DefaultValueProvider implements AttributeValueProvider {
    	
    	private Property property;
    	
    	private DefaultValueProvider(Property property) {
    		this.property = property;
    	}
    	
    	public String getValue() {
    		return property.getValue();
    	}
    	
    	public void setValue(String value) {
    		property.setValue(value);
    	}
    }

}
