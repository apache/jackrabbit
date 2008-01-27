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


/**
 *
 * CollectionDescriptor is used by the mapper to read general information on a collection field
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 *
 */
public class CollectionDescriptor implements ChildNodeDefDescriptor, PropertyDefDescriptor
{

     private String fieldName;
     private String jcrName;
     private String elementClassName;
     private String collectionConverterClassName;
     private String collectionClassName;
     private boolean proxy;
     private boolean autoRetrieve = true;
     private boolean autoUpdate = true;
     private boolean autoInsert = true;
     private String jcrType;
     private boolean jcrAutoCreated;
     private boolean jcrMandatory;
     private String jcrOnParentVersion;
     private boolean jcrProtected;
     private boolean jcrSameNameSiblings;
     private boolean jcrMultiple;
     private String defaultPrimaryType;

     private ClassDescriptor classDescriptor;

    /**
     * @return Returns the fieldName.
     */
    public String getFieldName()
    {
        return fieldName;
    }
    /**
     * @param fieldName The fieldName to set.
     */
    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }
    /**
     * @return Returns the jcrName.
     */
    public String getJcrName()
    {
        return jcrName;
    }
    /**
     * @param jcrName The jcrName to set.
     */
    public void setJcrName(String jcrName)
    {
        this.jcrName = jcrName;
    }


    /**
     * @return Returns the elementClassName.
     */
    public String getElementClassName()
    {
        return elementClassName;
    }
    /**
     * @param elementClassName The collection element class name to set.
     *
     */
    public void setElementClassName(String elementClassName)
    {
        this.elementClassName = elementClassName;
    }

    /**
     * @return Returns the proxy.
     */
    public boolean isProxy()
    {
        return proxy;
    }
    /**
     * @param proxy The proxy to set.
     */
    public void setProxy(boolean proxy)
    {
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
     *
     * @return The collection converter class name
     */
    public String getCollectionConverter()
    {
        return collectionConverterClassName;
    }

    /**
     * Set the collection converter class name
     * @param collectionConverterClassName The converter to set
     */
    public void setCollectionConverter(String collectionConverterClassName)
    {
        this.collectionConverterClassName = collectionConverterClassName;
    }

    /**
     *
     * @return the collection class name (can be also a Map)
     */
    public String getCollectionClassName()
    {
        return collectionClassName;
    }

    /**
     * Set the collection class name.
     * This collection class has to implement {@link org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection}
     * @param collectionClassName The collection class name to set
     */
    public void setCollectionClassName(String collectionClassName)
    {
        this.collectionClassName = collectionClassName;
    }

    /**
     *
     * @return The associated class descriptor
     */
    public ClassDescriptor getClassDescriptor()
    {
        return classDescriptor;
    }

    /**
     * Set the associated class descriptor
     * @param classDescriptor the class descriptor to set
     */
    public void setClassDescriptor(ClassDescriptor classDescriptor)
    {
        this.classDescriptor = classDescriptor;
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
    public boolean isJcrAutoCreated()
    {
        return jcrAutoCreated;
    }

    /** Setter for property jcrAutoCreated.
     *
     * @param value jcrAutoCreated
     */
    public void setJcrAutoCreated(boolean value)
    {
        this.jcrAutoCreated = value;
    }

    /** Getter for property jcrMandatory.
     *
     * @return jcrMandatory
     */
    public boolean isJcrMandatory()
    {
        return jcrMandatory;
    }

    /** Setter for property jcrMandatory.
     *
     * @param value jcrMandatory
     */
    public void setJcrMandatory(boolean value)
    {
        this.jcrMandatory = value;
    }

    /** Getter for property jcrOnParentVersion.
     *
     * @return jcrOnParentVersion
     */
    public String getJcrOnParentVersion()
    {
        return jcrOnParentVersion;
    }

    /** Setter for property jcrOnParentVersion.
     *
     * @param value jcrOnParentVersion
     */
    public void setJcrOnParentVersion(String value)
    {
        this.jcrOnParentVersion = value;
    }

    /** Getter for property jcrProtected.
     *
     * @return jcrProtected
     */
    public boolean isJcrProtected()
    {
        return jcrProtected;
    }

    /** Setter for property jcrProtected.
     *
     * @param value jcrProtected
     */
    public void setJcrProtected(boolean value)
    {
        this.jcrProtected = value;
    }

    /** Getter for property jcrSameNameSiblings.
     *
     * @return jcrSameNameSiblings
     */
    public boolean isJcrSameNameSiblings()
    {
        return jcrSameNameSiblings;
    }

    /** Setter for property jcrSameNameSiblings.
     *
     * @param value jcrSameNameSiblings
     */
    public void setJcrSameNameSiblings(boolean value)
    {
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

	public String toString() {
		
		return "Collection Descriptor : " +  this.getFieldName();
	}

    public String getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    public void setDefaultPrimaryType(String defaultPrimaryType) {
        this.defaultPrimaryType = defaultPrimaryType;
    }
}
