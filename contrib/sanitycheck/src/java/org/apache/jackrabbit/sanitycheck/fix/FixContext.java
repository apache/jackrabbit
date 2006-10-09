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
package org.apache.jackrabbit.sanitycheck.fix;

import org.apache.commons.chain.impl.ContextBase;
import org.apache.jackrabbit.sanitycheck.inconsistency.NodeInconsistency;

/**
 * This class contains the <code>Inconsistency</code> instances to fix.
 */
public class FixContext extends ContextBase
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3763098578666928434L;

    /**
     * Inconsistency to repair
     */
    private NodeInconsistency inconsistency;

    public NodeInconsistency getInconsistency()
    {
        return inconsistency;
    }

    public void setInconsistency(NodeInconsistency inconsistency)
    {
        this.inconsistency = inconsistency;
    }
}
