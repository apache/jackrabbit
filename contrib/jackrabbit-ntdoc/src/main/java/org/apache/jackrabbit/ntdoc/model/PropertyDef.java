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
package org.apache.jackrabbit.ntdoc.model;

import java.util.*;

/**
 * This class implements the property definition.
 */
public final class PropertyDef
        extends ItemDef {
    /**
     * Property types.
     */
    public final static int TYPE_STRING = 0;
    public final static int TYPE_BINARY = 1;
    public final static int TYPE_LONG = 2;
    public final static int TYPE_DOUBLE = 3;
    public final static int TYPE_BOOLEAN = 4;
    public final static int TYPE_DATE = 5;
    public final static int TYPE_NAME = 6;
    public final static int TYPE_PATH = 7;
    public final static int TYPE_REFERENCE = 8;
    public final static int TYPE_UNDEFINED = 9;

    /**
     * Property type strings.
     */
    private final static String[] TYPE_NAMES = {
            "STRING", "BINARY", "LONG", "DOUBLE", "BOOLEAN", "DATE",
            "NAME", "PATH", "REFERENCE", "UNDEFINED"
    };

    /**
     * Required type.
     */
    private int reqType;

    /**
     * Default values.
     */
    private final HashSet defValues;

    /**
     * Constraints.
     */
    private final HashSet constraints;

    /**
     * Construct the item.
     */
    public PropertyDef(String name) {
        super(name);
        this.defValues = new HashSet();
        this.constraints = new HashSet();
    }

    /**
     * Return the type.
     */
    public int getRequiredType() {
        return this.reqType;
    }

    /**
     * Set the required type.
     */
    public void setRequiredType(int reqType) {
        if ((reqType >= TYPE_STRING) && (reqType <= TYPE_UNDEFINED)) {
            this.reqType = reqType;
        }
    }

    /**
     * Return the type.
     */
    public String getRequiredTypeString() {
        if ((this.reqType >= 0) && (this.reqType < TYPE_NAMES.length)) {
            return TYPE_NAMES[this.reqType];
        } else {
            return TYPE_NAMES[0];
        }
    }

    /**
     * Return the default values.
     */
    public String[] getDefaultValues() {
        return (String[]) this.defValues.toArray(new String[this.defValues.size()]);
    }

    /**
     * Set the default values.
     */
    public void setDefaultValues(String[] defValues) {
        setDefaultValues(Arrays.asList(defValues));
    }

    /**
     * Set the default values.
     */
    public void setDefaultValues(Collection defValues) {
        this.defValues.clear();
        this.defValues.addAll(defValues != null ? defValues : Collections.EMPTY_LIST);
    }

    /**
     * Add default value.
     */
    public void addDefaultValue(String defValue) {
        this.defValues.add(defValue);
    }

    /**
     * Return the constraints.
     */
    public String[] getConstraints() {
        return (String[]) this.constraints.toArray(new String[this.constraints.size()]);
    }

    /**
     * Set the constraints.
     */
    public void setConstraints(String[] constraints) {
        setConstraints(Arrays.asList(constraints));
    }

    /**
     * Set the constraints.
     */
    public void setConstraints(Collection constraints) {
        this.constraints.clear();
        this.constraints.addAll(constraints != null ? constraints : Collections.EMPTY_LIST);
    }

    /**
     * Add constraint value.
     */
    public void addConstraint(String constraint) {
        this.constraints.add(constraint);
    }
}
