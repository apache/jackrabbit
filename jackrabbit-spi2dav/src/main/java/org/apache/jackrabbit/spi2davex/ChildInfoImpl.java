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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/** <code>ChildInfoImpl</code>... */
public class ChildInfoImpl implements ChildInfo {

    /**
     * The name of this child info.
     */
    private final Name name;

    /**
     * The unique id for this child info or <code>null</code> if it does not
     * have a unique id.
     */
    private final String uniqueId;

    /**
     * 1-based index of this child info.
     */
    private final int index;

    private int hashCode;

    /**
     * Creates a new serializable <code>ChildInfoImpl</code>.
     *
     * @param name     the name of the child node.
     * @param uniqueId the unique id of the child node or <code>null</code>.
     * @param index    the index of the child node.
     */
    public ChildInfoImpl(Name name, String uniqueId, int index) {
        if (name == null || index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.uniqueId = uniqueId;
        this.index = (index == Path.INDEX_UNDEFINED) ? Path.INDEX_DEFAULT : index;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getUniqueID() {
        return uniqueId;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        // build hashCode (format: <name>/<name>/<index>/<uniqueID>)
        if (hashCode == 0) {
            StringBuffer sb = new StringBuffer();
            sb.append(name.toString());
            sb.append("/");
            sb.append(index);
            sb.append("/");
            if (uniqueId != null) {
                sb.append(uniqueId);
            }
            hashCode = sb.toString().hashCode();
        }
        return hashCode;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ChildInfoImpl) {
            ChildInfoImpl ci = (ChildInfoImpl) object;
            boolean sameUID = (uniqueId == null) ? ci.uniqueId == null : uniqueId.equals(ci.uniqueId);
            return sameUID && name.equals(ci.name) && index == ci.index;
        }
        return false;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(name.toString());
        sb.append(" : ").append(index);
        sb.append(" : ").append((uniqueId == null) ? "-" : uniqueId);
        return sb.toString();
    }
}
