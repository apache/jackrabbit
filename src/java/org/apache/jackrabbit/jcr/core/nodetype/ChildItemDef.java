/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr.core.nodetype;

import org.apache.jackrabbit.jcr.core.QName;

import javax.jcr.version.OnParentVersionAction;

/**
 * An <code>ItemDef</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.1 $, $Date: 2004/09/09 15:23:43 $
 */
abstract class ChildItemDef implements Cloneable {

    protected QName declaringNodeType = null;
    private QName name = null;
    private boolean autoCreate = false;
    private int onParentVersion = OnParentVersionAction.COPY;
    private boolean writeProtected = false;
    private boolean mandatory = false;
    private boolean primaryItem = false;

    protected Object clone() throws CloneNotSupportedException {
	// delegate to superclass which does a shallow copy;
	// but since all fields are either primitives or immutables
	// this is sufficient
	return super.clone();
    }

    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof ChildItemDef) {
	    ChildItemDef other = (ChildItemDef) obj;
	    return (declaringNodeType == null ? other.declaringNodeType == null : declaringNodeType.equals(other.declaringNodeType)) &&
		    (name == null ? other.name == null : name.equals(other.name)) &&
		    autoCreate == other.autoCreate &&
		    onParentVersion == other.onParentVersion &&
		    writeProtected == other.writeProtected &&
		    mandatory == other.mandatory &&
		    primaryItem == other.primaryItem;
	}
	return false;
    }

    public void setDeclaringNodeType(QName declaringNodeType) {
	this.declaringNodeType = declaringNodeType;
    }

    public void setName(QName name) {
	this.name = name;
    }

    public void setAutoCreate(boolean autoCreate) {
	this.autoCreate = autoCreate;
    }

    public void setOnParentVersion(int onParentVersion) {
	this.onParentVersion = onParentVersion;
    }

    public void setProtected(boolean writeProtected) {
	this.writeProtected = writeProtected;
    }

    public void setMandatory(boolean mandatory) {
	this.mandatory = mandatory;
    }

    public void setPrimaryItem(boolean primaryItem) {
	this.primaryItem = primaryItem;
    }

    public QName getDeclaringNodeType() {
	return declaringNodeType;
    }

    public QName getName() {
	return name;
    }

    public boolean isAutoCreate() {
	return autoCreate;
    }

    public int getOnParentVersion() {
	return onParentVersion;
    }

    public boolean isProtected() {
	return writeProtected;
    }

    public boolean isMandatory() {
	return mandatory;
    }

    public boolean isPrimaryItem() {
	return primaryItem;
    }

    public abstract boolean definesNode();
}
