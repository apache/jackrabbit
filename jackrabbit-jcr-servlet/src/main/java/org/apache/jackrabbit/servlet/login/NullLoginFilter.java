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
import javax.servlet.http.HttpServletRequest;

/**
 * Login filter that always uses <code>null</code> credentials for logging in
 * to the content repository. This is useful for example for public web sites
 * where all repository access is performed using anonymous sessions. Another
 * use case for this login filter is when login information is made available
 * to the content repository through JAAS or some other out-of-band mechanism.
 *
 * @since Apache Jackrabbit 1.6
 */
public class NullLoginFilter extends AbstractLoginFilter {

    /**
     * Always returns <code>null</code>.
     *
     * @param request ignored
     * @return <code>null</code> credentials
     */
    protected Credentials getCredentials(HttpServletRequest request) {
        return null;
    }

}
