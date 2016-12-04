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
package org.apache.jackrabbit.spi.commons.query.qom;

import javax.jcr.query.qom.Selector;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;

/**
 * <code>SelectorImpl</code>...
 */
public class SelectorImpl extends SourceImpl implements Selector {

    /**
     * The name of the required node type.
     */
    private final Name nodeTypeName;

    /**
     * The selector name.
     */
    private final Name selectorName;

    SelectorImpl(NamePathResolver resolver,
                 Name nodeTypeName,
                 Name selectorName) {
        super(resolver);
        this.nodeTypeName = nodeTypeName;
        this.selectorName = selectorName;
    }

    /**
     * Gets the name of the required node type.
     *
     * @return the node type name; non-null
     */
    public Name getNodeTypeQName() {
        return nodeTypeName;
    }

    /**
     * Gets the selector name.
     * <p>
     * A selector's name can be used elsewhere in the query to identify the
     * selector.
     *
     * @return the selector name; non-null
     */
    public Name getSelectorQName() {
        return selectorName;
    }

    //---------------------------< SourceImpl >---------------------------------

    /**
     * {@inheritDoc}
     */ 
    public SelectorImpl[] getSelectors() {
        return new SelectorImpl[]{this};
    }

    //-----------------------------< Selector >---------------------------------

    /**
     * Gets the name of the required node type.
     *
     * @return the node type name; non-null
     */
    public String getNodeTypeName() {
        return getJCRName(nodeTypeName);
    }

    /**
     * Gets the selector name.
     * <p>
     * A selector's name can be used elsewhere in the query to identify the
     * selector.
     *
     * @return the selector name; non-null
     */
    public String getSelectorName() {
        return getJCRName(selectorName);
    }

    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }

    //------------------------< Object >----------------------------------------

    public String toString() {
        return quote(nodeTypeName) + " AS " + getSelectorName();
    }

}
