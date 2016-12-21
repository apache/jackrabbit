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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;

/**
 * This Class implements a version selector that selects a version by label.
 *
 * <pre>
 * V1.0
 * V1.1 - "foo"
 *
 * new LabelVersionSelector("foo").select() --&gt; V1.1
 * new LabelVersionSelector("bar").select() --&gt; null
 *
 * </pre>
 */
public class LabelVersionSelector implements VersionSelector {

    /**
     * a versionlabel hint
     */
    private Name label = null;

    /**
     * Creates a <code>LabelVersionSelector</code> that will try to select a
     * version with the given label.
     *
     * @param label label hint
     */
    public LabelVersionSelector(Name label) {
        this.label = label;
    }

    /**
     * Returns the label hint
     *
     * @return the label hint.
     */
    public Name getLabel() {
        return label;
    }

    /**
     * Sets the label hint
     *
     * @param label label hint
     */
    public void setLabel(Name label) {
        this.label = label;
    }

    /**
     * {@inheritDoc}
     *
     * Selects a version from the given version history using the previously
     * assigned hint in the following order: name, label, date, latest.
     */
    public InternalVersion select(InternalVersionHistory versionHistory)
            throws RepositoryException {
        return selectByLabel(versionHistory, label);
    }

    /**
     * Selects a version by label
     *
     * @param history history to select from
     * @param label desired label
     * @return the version with the given label or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    public static InternalVersion selectByLabel(InternalVersionHistory history, Name label)
            throws RepositoryException {
        return history.getVersionByLabel(label);
    }

    /**
     * returns debug information
     * @return debug information
     */
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("LabelVersionSelector(");
        ret.append("label=");
        ret.append(label);
        ret.append(")");
        return ret.toString();
    }
}
