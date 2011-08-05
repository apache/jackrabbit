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
package org.apache.jackrabbit.webdav.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>CSRFUtil</code>...
 */
public class CSRFUtil {

    /**
     * Constant used to
     */
    public static final String DISABLED = "disabled";

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(CSRFUtil.class);

    /**
     * Disable referrer based CSRF protection
     */
    private final boolean disabled;

    /**
     * Additional allowed referrer hosts for CSRF protection
     */
    private final Set<String> allowedReferrerHosts;

    /**
     * Creates a new instance from the specified configuration, which defines
     * the behaviour of the referrer based CSRF protection as follows:
     * <ol>
     * <li>If config is <code>null</code> or empty string the default
     * behaviour is to allow only requests with an empty referrer header or a
     * referrer host equal to the server host</li>
     * <li>A comma separated list of additional allowed referrer hosts which are
     * valid in addition to default behaviour (see above).</li>
     * <li>The value {@link #DISABLED} may be used to disable the referrer checking altogether</li>
     * </ol>
     *
     * @param config The configuration value which may be any of the following:
     * <ul>
     * <li><code>null</code> or empty string for the default behaviour, which
     * only allows requests with an empty referrer header or a
     * referrer host equal to the server host</li>
     * <li>A comma separated list of additional allowed referrer hosts which are
     * valid in addition to default behaviour (see above).</li>
     * <li>{@link #DISABLED} in order to disable the referrer checking altogether</li>
     * </ul>
     */
    public CSRFUtil(String config) {
        if (config == null || config.length() == 0) {
            disabled = false;
            allowedReferrerHosts = Collections.emptySet();
        } else {
            if (DISABLED.equalsIgnoreCase(config.trim())) {
                disabled = true;
                allowedReferrerHosts = Collections.emptySet();
            } else {
                disabled = false;
                String[] allowed = config.split(",");
                allowedReferrerHosts = new HashSet<String>(allowed.length);                
                for (String entry : allowed) {
                    allowedReferrerHosts.add(entry.trim());
                }
            }
        }
    }

    public boolean isValidRequest(HttpServletRequest request) throws MalformedURLException {
        if (disabled) {
            return true;
        } else {
            String refHeader = request.getHeader("Referer");
            if (refHeader == null) {
                // empty referrer is always allowed
                return true;
            } else {
                String host = new URL(refHeader).getHost();
                // test referrer-host equelst server or
                // if it is contained in the set of explicitly allowed host names
                return host.equals(request.getServerName()) || allowedReferrerHosts.contains(host);
            }
        }
    }
}