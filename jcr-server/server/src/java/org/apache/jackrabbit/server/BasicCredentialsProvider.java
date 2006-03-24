/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.server;

import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.webdav.DavConstants;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This Class implements a credentials provider that extracts the credentials
 * from the 'WWW-Authenticate' header and only supports 'Basic' authentication.
 */
public class BasicCredentialsProvider implements CredentialsProvider {

    private final String defaultHeaderValue;

    /**
     * Constructs a new BasicCredentialsProvider with the given default
     * value {@see #getCredentials} for details.
     *
     * @param defaultHeaderValue
     */
    public BasicCredentialsProvider(String defaultHeaderValue) {
        this.defaultHeaderValue = defaultHeaderValue;
    }

    /**
     * {@inheritDoc}
     *
     * Build a {@link Credentials} object for the given authorization header.
     * The creds may be used to login to the repository. If the specified header
     * string is <code>null</code> or not of the required format the behaviour
     * depends on the {@link #defaultHeaderValue} field:<br>
     * <ul>
     * <li> if this field is <code>null</code>, a LoginException is thrown.
     *      This is suiteable for clients (eg. webdav clients) for with
     *      sending a proper authorization header is not possible, if the
     *      server never send a 401.
     * <li> if this an empty string, null-credentials are returned, thus
     *      forcing an null login on the repository
     * <li> if this field has a 'user:password' value, the respective
     *      simple credentials are generated.
     * </ul>
     *
     * @param request the servlet request
     * @return credentials or <code>null</code>.
     * @throws ServletException If an IOException occured while decoding the
     *         Authorization header.
     * @throws LoginException if no suitable auth header and missing-auth-mapping
     *         is not present
     */
    public Credentials getCredentials(HttpServletRequest request)
            throws LoginException, ServletException {
        try {
            String authHeader = request.getHeader(DavConstants.HEADER_AUTHORIZATION);
            if (authHeader != null) {
                String[] authStr = authHeader.split(" ");
                if (authStr.length >= 2 && authStr[0].equalsIgnoreCase(HttpServletRequest.BASIC_AUTH)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Base64.decode(authStr[1].toCharArray(), out);
                    String decAuthStr = out.toString("ISO-8859-1");
                    int pos = decAuthStr.indexOf(':');
                    String userid = decAuthStr.substring(0, pos);
                    String passwd = decAuthStr.substring(pos + 1);
                    return new SimpleCredentials(userid, passwd.toCharArray());
                }
                throw new ServletException("Unable to decode authorization.");
            } else {
                // check special handling
                if (defaultHeaderValue == null) {
                    throw new LoginException();
                } else if (defaultHeaderValue.equals("")) {
                    return null;
                } else {
                    int pos = defaultHeaderValue.indexOf(':');
                    if (pos<0) {
                        return new SimpleCredentials(defaultHeaderValue, null);
                    } else {
                        return new SimpleCredentials(
                                defaultHeaderValue.substring(0, pos),
                                defaultHeaderValue.substring(pos+1).toCharArray()
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new ServletException("Unable to decode authorization: " + e.toString());
        }
    }

}
