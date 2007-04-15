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
package org.apache.jackrabbit.net;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.jcr.Session;

/**
 * The <code>JCRURLHandler</code> is the <code>URLStreamHandler</code> for
 * JCR Repository URLs identified by the scheme <code>jcr</code>.
 * <p>
 * JCR Repository URLs have not been standardized yet and may only be created
 * in the context of an existing <code>Session</code>. Therefore this handler
 * is not globally available and JCR Repository URLs may only be created through
 * the factory methods in the {@link org.apache.jackrabbit.net.URLFactory}
 * class.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 *
 * @author Felix Meschberger
 *
 * @see org.apache.jackrabbit.net.JCRURLConnection
 * @see org.apache.jackrabbit.net.URLFactory
 * @see org.apache.jackrabbit.net.URLFactory#createURL(Session, String)
 */
class JCRURLHandler extends URLStreamHandler {

    /**
     * The session used to create this handler, which is also used to open
     * the connection object.
     *
     * @see #getSession()
     */
    private final Session session;

    /**
     * Creates a new instance of the <code>JCRURLHandler</code> with the
     * given session.
     *
     * @param session The <code>Session</code> supporting this handler. This
     *      must not be <code>null</code>.
     *
     * @throws NullPointerException if <code>session</code> is <code>null</code>.
     */
    JCRURLHandler(Session session) {
        if (session == null) {
            throw new NullPointerException("session");
        }

        this.session = session;
    }

    /**
     * Returns the session supporting this handler.
     */
    Session getSession() {
        return session;
    }

    //---------- URLStreamHandler abstracts ------------------------------------

    /**
     * Gets a connection object to connect to an JCR Repository URL.
     *
     * @param url The JCR Repository URL to connect to.
     *
     * @return An instance of the {@link JCRURLConnection} class.
     *
     * @see JCRURLConnection
     */
    protected URLConnection openConnection(URL url) {
        return new JCRURLConnection(url, this);
    }

    /**
     * Checks the new <code>authority</code> and <code>path</code> before
     * actually setting the values on the url calling the base class
     * implementation.
     * <p>
     * We check the authority to not have been modified from the original URL,
     * as the authority is dependent on the repository <code>Session</code> on
     * which this handler is based and which was used to create the original
     * URL. Likewise the repository and workspace name parts of the path must
     * not have changed.
     *
     * @param u the URL to modify.
     * @param protocol the protocol name.
     * @param host the remote host value for the URL.
     * @param port the port on the remote machine.
     * @param authority the authority part for the URL.
     * @param userInfo the userInfo part of the URL.
     * @param path the path component of the URL.
     * @param query the query part for the URL.
     * @param ref the reference.
     *
     * @throws IllegalArgumentException if the authority or the repository name
     *             or workspace name parts of the path has changed.
     */
    protected void setURL(URL u, String protocol, String host, int port,
        String authority, String userInfo, String path, String query, String ref) {

        // check for authority
        if (u.getAuthority() != authority) {
            if (u.getAuthority() == null) {
                if (authority != null) {
                    throw new IllegalArgumentException("Authority " +
                        authority + " not supported by this handler");
                }
            } else if (!u.getAuthority().equals(authority)) {
                throw new IllegalArgumentException("Authority " +
                    authority + " not supported by this handler");
            }
        }

        // check for repository and/or workspace modifications
        FileParts newParts = new FileParts(path);
        if (!"_".equals(newParts.getRepository())) {
            throw new IllegalArgumentException("Repository " +
                newParts.getRepository() + " not supported by this handler");
        }
        if (!session.getWorkspace().getName().equals(newParts.getWorkspace())) {
            throw new IllegalArgumentException("Workspace " +
                newParts.getWorkspace() + " not supported by this handler");
        }

        // finally set the new values on the URL
        super.setURL(u, protocol, host, port, authority, userInfo, path, query,
            ref);
    }
}
