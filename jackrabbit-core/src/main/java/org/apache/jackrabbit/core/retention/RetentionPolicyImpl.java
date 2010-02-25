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
package org.apache.jackrabbit.core.retention;

import javax.jcr.retention.RetentionPolicy;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.Session;

/**
 * Basic implementation of the <code>RetentionPolicy</code> interface.
 */
public class RetentionPolicyImpl implements RetentionPolicy {

    private final Name name;
    private final NodeId nodeId;
    private final NameResolver resolver;

    private int hashCode = 0;

    /**
     * Creates a new <code>RetentionPolicy</code> that can be applied to a
     * <code>Node</code> using {@link javax.jcr.retention.RetentionManager#setRetentionPolicy(String, javax.jcr.retention.RetentionPolicy)}.
     *
     * @param jcrName The name of the policy. It must be a valid JCR name.
     * @param session The editing <code>Session</code> from which the retention
     * manager will be obtained.
     * @return a new <code>RetentionPolicy</code>
     * @throws RepositoryException If the jcr name isn't valid or if same other
     * error occurs.
     */
    public static RetentionPolicy createRetentionPolicy(String jcrName, Session session) throws RepositoryException {
        NameResolver resolver;
        if (session instanceof NameResolver) {
            resolver = (NameResolver) session;
        } else {
            resolver = new DefaultNamePathResolver(session);
        }
        return new RetentionPolicyImpl(jcrName, null, resolver);
    }

    RetentionPolicyImpl(String jcrName, NodeId nodeId, NameResolver resolver) throws IllegalNameException, NamespaceException {
        this(resolver.getQName(jcrName), nodeId, resolver);
    }

    private RetentionPolicyImpl(Name name, NodeId nodeId, NameResolver resolver) {
        this.name = name;
        this.nodeId = nodeId;
        this.resolver = resolver;
    }

    NodeId getNodeId() {
        return nodeId;
    }

    //----------------------------------------------------< RetentionPolicy >---
    /**
     * @see javax.jcr.retention.RetentionPolicy#getName()
     */
    public String getName() throws RepositoryException {
        return resolver.getJCRName(name);
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int h = 17;
            h = 37 * h + name.hashCode();
            h = 37 * h + nodeId.hashCode();
            hashCode = h;
        }
        return hashCode;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof RetentionPolicyImpl) {
            RetentionPolicyImpl other = (RetentionPolicyImpl) obj;
            return name.equals(other.name) && ((nodeId == null) ? other.nodeId == null : nodeId.equals(other.nodeId));
        }
        return false;
    }
}