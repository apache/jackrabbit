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
package org.apache.jackrabbit.servlet.login;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

/**
 * Login filter that relies on container authentication to provide the
 * authenticated username of a request. This username is associated with
 * a dummy password (empty by default, configurable through the init
 * parameter "password") in a {@link SimpleCredentials} object that is
 * used to log in to the underlying content repository. If no authenticated
 * user is found, then <code>null</code> credentials are used.
 * <p>
 * It is expected that the underlying repository is configured to simply
 * trust the given username. If the same repository is also made available
 * for direct logins, then a special secret password that allows logins with
 * any username could be configured just for this filter.
 *
 * @since Apache Jackrabbit 1.6
 */
public class ContainerLoginFilter extends AbstractLoginFilter {

    /**
     * The dummy password used for the repository login. Empty by default.
     */
    private char[] password = new char[0];

    public void init(FilterConfig config) {
        super.init(config);

        String password = config.getInitParameter("password");
        if (password != null) {
            this.password = password.toCharArray();
        }
    }

    protected Credentials getCredentials(HttpServletRequest request) {
        String user = request.getRemoteUser();
        if (user != null) {
            return new SimpleCredentials(user, password);
        } else {
            return null;
        }
    }

}
