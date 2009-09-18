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
package org.apache.jackrabbit.jcr2spi.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Map;

/**
 * <code>EffectiveNodeTypeProvider</code>...
 */
public interface EffectiveNodeTypeProvider {

    /**
     * Build the <code>EffectiveNodeType</code> from the given
     * <code>NodeType</code> name.
     *
     * @param ntName
     * @return
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(Name ntName)
            throws NoSuchNodeTypeException;

    /**
     * Build the <code>EffectiveNodeType</code> from the given array of
     * <code>NodeType</code> names.
     *
     * @param ntNames
     * @return
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(Name[] ntNames)
            throws ConstraintViolationException, NoSuchNodeTypeException;

    /**
     * @param ntNames
     * @param ntdMap
     * @return
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(Name[] ntNames, Map<Name, QNodeTypeDefinition> ntdMap)
            throws ConstraintViolationException, NoSuchNodeTypeException;

    /**
     * Builds an effective node type representation from the given node type
     * definition. Whereas all referenced node types must exist (i.e. must be
     * present in the specified map), the definition itself is not required to
     * be registered.
     *
     * @param ntd
     * @param ntdMap
     * @return
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(QNodeTypeDefinition ntd,
                                                  Map<Name, QNodeTypeDefinition> ntdMap)
            throws ConstraintViolationException, NoSuchNodeTypeException;
}
