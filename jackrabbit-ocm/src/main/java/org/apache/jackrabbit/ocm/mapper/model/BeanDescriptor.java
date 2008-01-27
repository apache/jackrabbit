/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.ocm.mapper.model;

import org.apache.jackrabbit.ocm.manager.beanconverter.BeanConverter;

/**
 * BeanDescriptor is used by the mapper to read general information on a bean field
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class BeanDescriptor implements ChildNodeDefDescriptor, PropertyDefDescriptor {
    private ClassDescriptor classDescriptor;

    private String fieldName;
    private String jcrName;
    private boolean proxy;
    private boolean autoRetrieve = true;
    private boolean autoUpdate = true;
    private boolean autoInsert = true;
    private String converter;
    private BeanConverter beanConverter;
    private String jcrType;
    private boolean jcrAutoCreated;
    private boolean jcrMandatory;
    private String jcrOnParentVersion;
    private boolean jcrProtected;
    private boolean jcrSameNameSiblings;
    private boolean jcrMultiple;
    private String defaultPrimaryType;

    /**
     * @return Returns the fieldName.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @param fieldName The fieldName to set.
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @return Returns the jcrName.
     */
    public String getJcrName() {
        return jcrName;
    }

    /**
     * @param jcrName The jcrName to set.
     */
    public void setJcrName(String jcrName) {
        this.jcrName = jcrName;
    }

    /**
     * @return Returns the proxy.
     */
    public boolean isProxy() {
        return proxy;
    }

    /**
     * @param proxy The proxy to set.
     */
    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }


    public boolean isAutoInsert() {
		return autoInsert;
	}

	public void setAutoInsert(boolean autoInsert) {
		this.autoInsert = autoInsert;
	}

	public boolean isAutoRetrieve() {
		return autoRetrieve;
	}

	public void setAutoRetrieve(boolean autoRetrieve) {
		this.autoRetrieve = autoRetrieve;
	}

	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	public void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
	}


    /**
     * Get the <code>BeanConverter</code> fully qualified name or <tt>null</tt>
     * if none specified by the bean descriptor.
     *
     * @return fully qualified class name or <tt>null</tt>
     */
    public String getConverter() {
        return this.converter;
    }

    /**
     * Sets the fully qualified name of a <code>BeanConverter</code> to be used.
     *
     * @param converterClass a fully qualified class name
     */
    public void setConverter(String converterClass) {
        this.converter = converterClass;
    }


    /**
     * Getter for property jcrType.
     *
     * @return jcrType
     */
    public String getJcrType() {
        return jcrType;
    }

    /**
     * Setter for property jcrType.
     *
     * @param value jcrType
     */
    public void setJcrType(String value) {
        this.jcrType = value;
    }

    /** Getter for property jcrAutoCreated.
     *
     * @return jcrAutoCreated
     */
    public boolean isJcrAutoCreated() {
        return jcrAutoCreated;
    }

    /** Setter for property jcrAutoCreated.
     *
     * @param value jcrAutoCreated
     */
    public void setJcrAutoCreated(boolean value) {
        this.jcrAutoCreated = value;
    }

    /** Getter for property jcrMandatory.
     *
     * @return jcrMandatory
     */
    public boolean isJcrMandatory() {
        return jcrMandatory;
    }

    /** Setter for property jcrMandatory.
     *
     * @param value jcrMandatory
     */
    public void setJcrMandatory(boolean value) {
        this.jcrMandatory = value;
    }

    /** Getter for property jcrOnParentVersion.
     *
     * @return jcrOnParentVersion
     */
    public String getJcrOnParentVersion() {
        return jcrOnParentVersion;
    }

    /** Setter for property jcrOnParentVersion.
     *
     * @param value jcrOnParentVersion
     */
    public void setJcrOnParentVersion(String value) {
        this.jcrOnParentVersion = value;
    }

    /** Getter for property jcrProtected.
     *
     * @return jcrProtected
     */
    public boolean isJcrProtected() {
        return jcrProtected;
    }

    /** Setter for property jcrProtected.
     *
     * @param value jcrProtected
     */
    public void setJcrProtected(boolean value) {
        this.jcrProtected = value;
    }

    /** Getter for property jcrSameNameSiblings.
     *
     * @return jcrSameNameSiblings
     */
    public boolean isJcrSameNameSiblings() {
        return jcrSameNameSiblings;
    }

    /** Setter for property jcrSameNameSiblings.
     *
     * @param value jcrSameNameSiblings
     */
    public void setJcrSameNameSiblings(boolean value) {
        this.jcrSameNameSiblings = value;
    }

    /**
     * Getter for property jcrMultiple.
     *
     * @return jcrMultiple
     */
    public boolean isJcrMultiple() {
        return jcrMultiple;
    }

    /**
     * Setter for property jcrMultiple.
     *
     * @param value jcrMultiple
     */
    public void setJcrMultiple(boolean value) {
        this.jcrMultiple = value;
    }

    /**
     * @param descriptor
     */
    public void setClassDescriptor(ClassDescriptor descriptor) {
        this.classDescriptor = descriptor;
    }

    /**
     * @return Returns the classDescriptor.
     */
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

	public String toString() {
		
		return "Bean Descriptor : " +  this.fieldName;
	}

    public String getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    public void setDefaultPrimaryType(String defaultPrimaryType) {
        this.defaultPrimaryType = defaultPrimaryType;
    }
}
