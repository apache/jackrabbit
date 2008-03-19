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
package org.apache.jackrabbit.api.security.user;

import org.apache.jackrabbit.api.security.user.Authorizable;

import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * A Group is a collection of {@link #getMembers() Authorizable}s.
 */
public interface Group extends Authorizable {

    /**
     * @return Iterator of <code>Authorizable</code>s which are getMembers of
     * this Group.
     * @throws RepositoryException
     */
    Iterator getMembers() throws RepositoryException;

    /**
     * @return true if the Authorizable to test is a member of this Group.
     * @throws RepositoryException
     */
    boolean isMember(Authorizable authorizable) throws RepositoryException;

    /**
     * Add a member to this Group.<br>
     * Changes will be persisted immediately.
     *
     * @return true if the <code>Authorizable</code> has successfully been added
     * to this Group, false otherwise (e.g. unknown implemention
     * or if it already is a member or if the passed authorizable is the
     * group itself or for some implementation specific constraint).
     * @throws RepositoryException If an error occurs.
     */
    boolean addMember(Authorizable authorizable) throws RepositoryException;

    /**
     * Remove a member to this Group.<br>
     * Changes will be persisted immediately.
     *
     * @return true if the Authorizable was successfully removed. False otherwise.
     * @throws RepositoryException
     */
    boolean removeMember(Authorizable authorizable) throws RepositoryException;
}
