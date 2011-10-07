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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.id.NodeId;
import javax.jcr.version.Version;

import java.util.Calendar;
import java.util.List;

/**
 * This interface defines the internal version.
 */
public interface InternalVersion extends InternalVersionItem {

    /**
     * Returns the name of this version.
     *
     * @return the name of this version.
     */
    Name getName();

    /**
     * Returns the frozen node of this version.
     *
     * @return the frozen node.
     */
    InternalFrozenNode getFrozenNode();

    /**
     * Returns the node id of the frozen node.
     *
     * @return the node id of the frozen node;
     */
    NodeId getFrozenNodeId();

    /**
     * Equivalent to {@link javax.jcr.version.Version#getCreated()}
     *
     * @see javax.jcr.version.Version#getCreated()
     * @return the created date
     */
    Calendar getCreated();

    /**
     * Equivalent to {@link javax.jcr.version.Version#getSuccessors()}}
     *
     * @see javax.jcr.version.Version#getSuccessors()
     * @return the successors as internal versions
     */
    List<InternalVersion> getSuccessors();

    /**
     * Equivalent to {@link Version#getLinearSuccessor()}.
     *
     * @param baseVersion base version to determine single line of descent
     * @return the successor as internal version
     *
     * @see Version#getLinearSuccessor()
     */
    InternalVersion getLinearSuccessor(InternalVersion baseVersion);

    /**
     * Equivalent to {@link javax.jcr.version.Version#getPredecessors()}}
     *
     * @see javax.jcr.version.Version#getPredecessors()
     * @return the predecessors as internal versions
     */
    InternalVersion[] getPredecessors();

    /**
     * Equivalent to {@link Version#getLinearPredecessor()}
     *
     * @see Version#getLinearPredecessor()
     * @return the predecessor as internal version
     */
    InternalVersion getLinearPredecessor();

    /**
     * Checks if this version is more recent than the given version <code>v</code>.
     * A version is more recent if and only if it is a successor (or a successor
     * of a successor, etc., to any degree of separation) of the compared one.
     *
     * @param v the version to check
     * @return <code>true</code> if the version is more recent;
     *         <code>false</code> otherwise.
     */
    boolean isMoreRecent(InternalVersion v);

    /**
     * returns the internal version history in which this version lives in.
     *
     * @return the version history for this version.
     */
    InternalVersionHistory getVersionHistory();

    /**
     * checks if this is the root version.
     *
     * @return <code>true</code> if this version is the root version;
     *         <code>false</code> otherwise.
     */
    boolean isRootVersion();

    /**
     * Checks, if this version has the given label associated
     *
     * @param label the label to check.
     * @return <code>true</code> if the label is assigned to this version;
     *         <code>false</code> otherwise.
     */
    boolean hasLabel(Name label);

    /**
     * returns the labels that are assigned to this version
     *
     * @return a string array of labels.
     */
    Name[] getLabels();
}
