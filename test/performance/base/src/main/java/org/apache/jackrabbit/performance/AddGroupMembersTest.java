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

public class AddGroupMembersTest extends AbstractTest {

    private static final int SCALE = getScale(100);

    private static final Principal GROUP_PRINCIPAL = new Principal() {
        public String getName() {
            return "test_group";
        }
    };

    private UserManager userMgr;
    private Group group;
    private User[] users = new User[SCALE];
    private int userCount;

    @Override
    protected void beforeSuite() throws Exception {
        userMgr = ((JackrabbitSession) loginWriter()).getUserManager();
        group = userMgr.createGroup(GROUP_PRINCIPAL);
    }

    @Override
    protected void beforeTest() throws Exception {
        for (int i = 0; i < users.length; i++, userCount++) {
            users[i] = userMgr.createUser("user_" + userCount, "pass");
        }
    }

    @Override
    protected void runTest() throws Exception {
        for (User user : users) {
            group.addMember(user);
        }
    }

    @Override
    protected void afterSuite() throws Exception {
        for (int i = 0; i < userCount; i++) {
            userMgr.getAuthorizable("user_" + i).remove();
        }
    }

}
