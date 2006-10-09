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
package org.apache.jackrabbit.sanitycheck.inconsistency.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.sanitycheck.inconsistency.ParentEntryInconsistency;

/**
 * <p>
 * No such parent inconsistency.
 * </p>
 */
public class NoSuchParentInconsistency extends AbstractNodeInconsistency
        implements ParentEntryInconsistency
{
    private Log log = LogFactory.getLog(NoSuchParentInconsistency.class);

    private String parentUUID;

    /**
     * @inheritDoc
     */
    public String getDescription()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("The node ");
        sb.append(((NodeState) node).getUUID());
        sb.append(" contains a non-existent parent (");
        sb.append(this.parentUUID);
        sb.append(").");
        return sb.toString();
    }

    public String getParentUUID()
    {
        return parentUUID;
    }

    public void setParentUUID(String parentUUID)
    {
        this.parentUUID = parentUUID;
    }
}
