/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.QName;

import javax.jcr.version.Version;
import java.util.Calendar;

/**
 * This interface defines the internal version.
 */
public interface InternalVersion extends InternalVersionItem {

    /**
     * Returns the name of this version.
     *
     * @return the name of this version.
     */
    public QName getName();

    /**
     * Returns the frozen node of this version or <code>null</code> if this is
     * the root version.
     *
     * @return the frozen node.
     */
    public InternalFrozenNode getFrozenNode();

    /**
     * Aequivalent to {@link javax.jcr.version.Version#getCreated()}
     *
     * @see Version#getCreated()
     */
    public Calendar getCreated();

    /**
     * Aequivalent to {@link javax.jcr.version.Version#getSuccessors()}}
     *
     * @see Version#getSuccessors()
     */
    public InternalVersion[] getSuccessors();

    /**
     * Aequivalent to {@link javax.jcr.version.Version#getPredecessors()}}
     *
     * @see javax.jcr.version.Version#getPredecessors()
     */
    public InternalVersion[] getPredecessors();

    /**
     * Checks if this version is more recent than the given version <code>v</code>.
     * A version is more recent if and only if it is a successor (or a successor
     * of a successor, etc., to any degree of separation) of the compared one.
     *
     * @param v the version to check
     * @return <code>true</code> if the version is more recent;
     *         <code>false</code> otherwise.
     */
    public boolean isMoreRecent(InternalVersion v);

    /**
     * returns the internal version history in wich this version lifes in.
     *
     * @return the version history for this version.
     */
    public InternalVersionHistory getVersionHistory();

    /**
     * checks if this is the root version.
     *
     * @return <code>true</code> if this version is the root version;
     *         <code>false</code> otherwise.
     */
    public boolean isRootVersion();

    /**
     * Checks, if this version has the given label assosiated
     *
     * @param label the label to check.
     * @return <code>true</code> if the label is assigned to this version;
     *         <code>false</code> otherwise.
     */
    public boolean hasLabel(String label);

    /**
     * returns the labels that are assigned to this version
     *
     * @return a string array of labels.
     */
    public String[] getLabels();
}
