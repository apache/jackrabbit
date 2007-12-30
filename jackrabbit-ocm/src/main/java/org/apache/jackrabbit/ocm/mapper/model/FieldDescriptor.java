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
 *
 * FieldDescriptor is used by the mapper to read general information on a atomic field
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 *
 */
public class FieldDescriptor implements PropertyDefDescriptor {
    private String fieldName;    
    private String jcrName;
    private String jcrType;
    private boolean jcrAutoCreated;
    private boolean jcrMandatory;
    private String jcrOnParentVersion;
    private boolean jcrProtected;
    private boolean jcrMultiple;
    private ClassDescriptor classDescriptor;
    private boolean id;
    private boolean path;
    private boolean uuid;
    private String converter;
    private String jcrDefaultValue; 
    private String[] jcrValueConstraints = new String[0];
   

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
     * 
     * @return The atomic type converter to use, can be null
     */
    public String getConverter() {
		return converter;
	}

    /**
     * Set the atomic converter 
     * @param converter the atomic converter to use
     */
	public void setConverter(String converter) {
		this.converter = converter;
	}

	/**
     *
     * @return the associated class descriptor
     */
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    /**
     * Set the associated class descriptor
     * @param classDescriptor  the class descriptor to set
     */
    public void setClassDescriptor(ClassDescriptor classDescriptor) {
        this.classDescriptor = classDescriptor;
    }

    /**
     * @return true if the field is the class ID
     */
    public boolean isId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(boolean id) {
        this.id = id;
    }

    /**
     * @return Returns true if the field is the object JCR path.
     */
    public boolean isPath() {
        return path;
    }

    /**
     * @param path The path to set.
     */
    public void setPath(boolean path) {
        this.path = path;
    }

    /**
     * @return Returns true if the field is the UUID.
     */
    public boolean isUuid() {
        return uuid;
    }

    /**
     * @param path The path to set.
     */
    public void setUuid(boolean uuid) {
        this.uuid = uuid;
    }    
    
    /** Getter for property jcrType.
     *
     * @return jcrType
     */
    public String getJcrType() {
        return jcrType;
    }

    /** Setter for property jcrType.
     *
     * @param value jcrType
     */
    public void setJcrType(String value) {
        this.jcrType = value;
    }

    /** Getter for propery jcrAutoCreated.
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

    /** Getter for property jcrMultiple.
     *
     * @return jcrMultiple
     */
    public boolean isJcrMultiple() {
        return jcrMultiple;
    }

    /** Setter for property jcrMultiple.
     *
     * @param value jcrMultiple
     */
    public void setJcrMultiple(boolean value) {
        this.jcrMultiple = value;
    }
    
    public String getJcrDefaultValue() {
		return jcrDefaultValue;
	}

	public void setJcrDefaultValue(String defaultValue) {
		this.jcrDefaultValue = defaultValue;
	}

	public String[] getJcrValueConstraints() {
		return jcrValueConstraints;
	}

	public void setJcrValueConstraints(String[] jcrValueConstraints) {
        if (null != jcrValueConstraints && jcrValueConstraints.length == 1) {
        	this.jcrValueConstraints = jcrValueConstraints[0].split(" *, *");
        }

	}
	
    public void setJcrValueConstraints(String jcrValueConstraints) {                
    	if (jcrValueConstraints != null && ! jcrValueConstraints.equals(""))
    	{
    		this.jcrValueConstraints = jcrValueConstraints.split(" *, *");
    	}
    }

    
	public String toString() {
		
		return "Field Descriptor : " +  this.getFieldName();
	}    
}
