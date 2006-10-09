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

/**
 * This class defines the item type.
 */
public abstract class ItemDef
        implements Comparable {
    /**
     * On parent version options.
     */
    public final static int OPV_COPY = 0;
    public final static int OPV_VERSION = 1;
    public final static int OPV_INITIALIZE = 2;
    public final static int OPV_COMPUTE = 3;
    public final static int OPV_IGNORE = 4;
    public final static int OPV_ABORT = 5;

    /**
     * On parent version names.
     */
    private final static String[] OPV_NAMES = {
            "COPY", "VERSION", "INITIALIZE", "COMPUTE",
            "IGNORE", "ABORT"
    };

    /**
     * Name of item.
     */
    private final String name;

    /**
     * Declaring node type.
     */
    private NodeType declNodeType;

    /**
     * On parent version.
     */
    private int onParentVersion;

    /**
     * Auto created.
     */
    private boolean autoCreated;

    /**
     * Mandatory.
     */
    private boolean mandatory;

    /**
     * Protected.
     */
    private boolean prot;

    /**
     * Primary item.
     */
    private boolean primary;

    /**
     * Multiple.
     */
    private boolean multiple;

    /**
     * Construct the item def.
     */
    public ItemDef(String name) {
        this.name = name;
    }

    /**
     * Return the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the on parent version.
     */
    public int getOnParentVersion() {
        return this.onParentVersion;
    }

    /**
     * Return the on parent version.
     */
    public String getOnParentVersionString() {
        return OPV_NAMES[this.onParentVersion];
    }

    /**
     * Return true if auto created.
     */
    public boolean isAutoCreated() {
        return this.autoCreated;
    }

    /**
     * Return true if mandatory.
     */
    public boolean isMandatory() {
        return this.mandatory;
    }

    /**
     * Return true if protected.
     */
    public boolean isProtected() {
        return this.prot;
    }

    /**
     * Return true if primary.
     */
    public boolean isPrimary() {
        return this.primary;
    }

    /**
     * Return true if multiple.
     */
    public boolean isMultiple() {
        return this.multiple;
    }

    /**
     * Set the on parent version.
     */
    public void setOnParentVersion(int onParentVersion) {
        if ((onParentVersion >= OPV_COPY) && (onParentVersion <= OPV_ABORT)) {
            this.onParentVersion = onParentVersion;
        }
    }

    /**
     * Set true if auto created.
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Set true if mandatory.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Set true if protected.
     */
    public void setProtected(boolean prot) {
        this.prot = prot;
    }

    /**
     * Set true if primary.
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * Set true if multiple.
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /**
     * Return the declaring node type.
     */
    public NodeType getDeclaringNodeType() {
        return this.declNodeType;
    }

    /**
     * Set the declaring node type.
     */
    public void setDeclaringNodeType(NodeType nodeType) {
        this.declNodeType = nodeType;
    }

    /**
     * Compare to.
     */
    public int compareTo(Object o) {
        return this.name.compareTo(((ItemDef) o).name);
    }
}
