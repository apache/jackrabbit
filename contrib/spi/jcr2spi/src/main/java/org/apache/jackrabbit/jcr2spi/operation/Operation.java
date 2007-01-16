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
package org.apache.jackrabbit.jcr2spi.operation;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;

import javax.jcr.version.VersionException;
import java.util.Collection;

/**
 * <code>Operation</code>...
 */
public interface Operation {

    /**
     * Returns the name of <code>this</code> operation.
     *
     * @return the name of <code>this</code> operation.
     */
    public String getName();

    /**
     * Calls the appropriate <code>visit</code> method on <code>visitor</code>
     * based on the type of this operation.
     *
     * @param visitor the visitor to call back.
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException;

    /**
     * A collection of {@link ItemState}s that are affected by this operation.
     *
     * @return collection of affected <code>ItemState</code>s.
     */
    public Collection getAffectedItemStates();

    /**
     * In case of {@link CacheBehaviour#INVALIDATE}
     * the result of the operation will not be pushed by observation events.
     * Instead the workspace operations must make sure, that the affected
     * item states are properly refreshed or invalidated.
     */
    public void persisted();
}