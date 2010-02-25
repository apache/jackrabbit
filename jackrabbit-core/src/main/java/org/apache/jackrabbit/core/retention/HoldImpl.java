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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import javax.jcr.retention.Hold;

import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

/**
 * <code>HoldImpl</code>...
 */
class HoldImpl implements Hold {

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();
    
    private static final String DEEP = "D_";
    private static final String SHALLOW = "S_";

    private final Name name;
    private final boolean isDeep;
    private final NodeId nodeId;

    private final NameResolver resolver;

    private int hashCode = 0;

    HoldImpl(Name name, boolean isDeep, NodeId nodeId, NameResolver resolver) {
        this.name = name;
        this.isDeep = isDeep;
        this.nodeId = nodeId;
        this.resolver = resolver;
    }

    NodeId getNodeId() {
        return nodeId;
    }

    Value toValue(ValueFactory valueFactory) throws RepositoryException {
        String str = ((isDeep) ? DEEP : SHALLOW) + name.toString();
        return valueFactory.createValue(str);
    }

    static HoldImpl createFromValue(Value val, NodeId nodeId, NameResolver resolver) throws RepositoryException {
        String str = val.getString();
        Name name = NAME_FACTORY.create(str.substring(2));
        boolean isDeep = str.startsWith(DEEP);
        return new HoldImpl(name, isDeep, nodeId, resolver);
    }

    static HoldImpl[] createFromProperty(PropertyImpl property, NodeId nodeId) throws RepositoryException {
        Value[] vs = property.getValues();
        HoldImpl[] holds = new HoldImpl[vs.length];
        for (int i = 0; i < vs.length; i++) {
            holds[i] = createFromValue(vs[i], nodeId, (SessionImpl) property.getSession());
        }
        return holds;
    }

    //---------------------------------------------------------------< Hold >---
    /**
     * @see javax.jcr.retention.Hold#isDeep()
     */
    public boolean isDeep() throws RepositoryException {
        return isDeep;
    }

    /**
     * @see javax.jcr.retention.Hold#getName()
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
            h = 37 * h + Boolean.valueOf(isDeep).hashCode();
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

        if (obj instanceof HoldImpl) {
            HoldImpl other = (HoldImpl) obj;
            return isDeep == other.isDeep && name.equals(other.name) && nodeId.equals(other.nodeId);
        }
        return false;
    }
}