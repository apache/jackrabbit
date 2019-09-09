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
package org.apache.jackrabbit.core.security.user.action;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Iterator;

/**
 * <code>ClearMembershipAction</code>...
 */
public class ClearMembershipAction extends AbstractAuthorizableAction {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ClearMembershipAction.class);

    /**
     * Create a new instance.
     */
    public ClearMembershipAction() {}

    //-------------------------------------------------< AuthorizableAction >---
    /**
     * @see AuthorizableAction#onRemove(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session)
     */
    @Override
    public void onRemove(Authorizable authorizable, Session session) throws RepositoryException {
        Iterator<Group> membership = authorizable.declaredMemberOf();
        while (membership.hasNext()) {
            membership.next().removeMember(authorizable);
        }
    }
}