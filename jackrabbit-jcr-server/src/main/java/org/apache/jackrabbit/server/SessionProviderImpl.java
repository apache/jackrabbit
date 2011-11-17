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
package org.apache.jackrabbit.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.spi.commons.SessionExtensions;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.util.LinkHeaderFieldParser;

/**
 * This Class implements a default session provider uses a credentials provider.
 */
public class SessionProviderImpl implements SessionProvider {

    public static final String ATTRIBUTE_SESSION_ID = SessionProviderImpl.class + "#sessionid()";
    
    /**
     * the credentials provider
     */
    private CredentialsProvider cp;

    /**
     * Creates a new SessionProvider
     * 
     * @param cp
     */
    public SessionProviderImpl(CredentialsProvider cp) {
        this.cp = cp;
    }

    /**
     * {@inheritDoc }
     */
    public Session getSession(HttpServletRequest request,
            Repository repository, String workspace) throws LoginException,
            RepositoryException, ServletException {
        Credentials creds = cp.getCredentials(request);
        Session s;
        if (creds == null) {
            s = repository.login(workspace);
        } else {
            s = repository.login(creds, workspace);
        }

        // extract information from Link header fields
        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
                request.getHeaders("Link"));
        String userData = getJcrUserData(lhfp);
        s.getWorkspace().getObservationManager().setUserData(userData);

        String sessionId = getSessionIdentifier(lhfp);
        if (s instanceof SessionExtensions) {
            SessionExtensions xs = (SessionExtensions) s;
            xs.setAttribute(ATTRIBUTE_SESSION_ID, sessionId);
        }
        return s;
    }

    /**
     * {@inheritDoc }
     */
    public void releaseSession(Session session) {
        session.logout();
    }

    // find first link relation for JCR User Data
    private String getJcrUserData(LinkHeaderFieldParser lhfp) {
        String jcrUserData = null;
        String target = lhfp
                .getFirstTargetForRelation(JcrRemotingConstants.RELATION_USER_DATA);
        if (target != null) {
            jcrUserData = getJcrUserData(target);
        }

        return jcrUserData;
    }

    // find first link relation for remote session identifier
    private String getSessionIdentifier(LinkHeaderFieldParser lhfp) {
        return lhfp
                .getFirstTargetForRelation(JcrRemotingConstants.RELATION_REMOTE_SESSION_ID);
    }

    // extracts User Data string from RFC 2397 "data" URI
    // only supports the simple case of "data:,..." for now
    private String getJcrUserData(String target) {
        try {
            URI datauri = new URI(target);

            String scheme = datauri.getScheme();

            // Poor Man's data: URI parsing
            if (scheme != null
                    && "data".equals(scheme.toLowerCase(Locale.ENGLISH))) {

                String sspart = datauri.getRawSchemeSpecificPart();

                if (sspart.startsWith(",")) {
                    return Text.unescape(sspart.substring(1));
                }
            }
        } catch (URISyntaxException ex) {
            // not a URI, skip
        }

        return null;
    }
}
