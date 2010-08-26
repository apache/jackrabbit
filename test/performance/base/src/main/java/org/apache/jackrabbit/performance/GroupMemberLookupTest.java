/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.performance;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;

import javax.jcr.Session;

import java.security.Principal;
import java.util.Random;

public class GroupMemberLookupTest extends AbstractTest {
    private static int USER_COUNT = 2000;
    private static final Principal GROUP_PRINCIPAL = new Principal() {
        public String getName() {
            return "test_group";
        }
    };

    private final Random rng = new Random();

    private Session session;
    private UserManager userMgr;
    private Group group;
    private Authorizable user;

    @Override
    protected void beforeSuite() throws Exception {
        session = getRepository().login(getCredentials());
        userMgr = ((JackrabbitSession) session).getUserManager();
        group = userMgr.createGroup(GROUP_PRINCIPAL);
        for (int k = 0; k < USER_COUNT; k++) {
            group.addMember(userMgr.createUser("user_" + k, "pass"));
        }
    }

    @Override
    protected void beforeTest() throws Exception {
        String id = "user_" + rng.nextInt(USER_COUNT);
        user = userMgr.getAuthorizable(id);
    }

    @Override
    protected void runTest() throws Exception {
        group.isMember(user);
    }

    @Override
    protected void afterSuite() throws Exception {
        for (int k = 0; k < USER_COUNT; k++) {
            userMgr.getAuthorizable("user_" + k).remove();
        }
        group.remove();
        session.logout();
    }

}
