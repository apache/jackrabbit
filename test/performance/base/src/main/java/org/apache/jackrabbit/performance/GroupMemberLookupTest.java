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
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

import java.security.Principal;
import java.util.Random;

public class GroupMemberLookupTest extends AbstractTest {

    private static final int USER_COUNT = getScale(2000);

    private static final Principal GROUP_PRINCIPAL = new Principal() {
        public String getName() {
            return "test_group";
        }
    };

    private final Random rng = new Random();

    private Group group;
    private User[] users;

    @Override
    protected void beforeSuite() throws Exception {
        UserManager userMgr = ((JackrabbitSession) loginWriter()).getUserManager();
        group = userMgr.createGroup(GROUP_PRINCIPAL);
        users = new User[USER_COUNT];
        for (int i = 0; i < users.length; i++) {
            users[i] = userMgr.createUser("user_" + i, "pass");
            group.addMember(users[i]);
        }
    }

    @Override
    protected void runTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            group.isMember(users[rng.nextInt(users.length)]);
        }
    }

    @Override
    protected void afterSuite() throws Exception {
        for (User user : users) {
            user.remove();
        }
        group.remove();
    }

}
