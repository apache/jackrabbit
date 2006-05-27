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

import java.net.MalformedURLException;
import java.net.URL;

import javax.jcr.Session;

/**
 * The <code>URLFactory</code> class provides factory methods for creating
 * JCR Repository and JCRJar URLs.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 *
 * @author Felix Meschberger
 */
public final class URLFactory {

    /**
     * The scheme for JCR Repository URLs (value is "jcr").
     */
    public static final String REPOSITORY_SCHEME = "jcr";

    /**
     * The scheme for JCRJar URLs (value is "jar").
     */
    public static final String REPOSITORY_JAR_SCHEME = "jar";

    /** Private default constructor, not to be instantiated */
    private URLFactory() {
    }

    /**
     * Creates a new JCR Repository URL for the given session and item path.
     *
     * @param session The repository session providing access to the item.
     * @param path The absolute path to the item. This must be an absolute
     *      path with a leading slash character. If this is <code>null</code>
     *      the root node path - <code>/</code> - is assumed.
     *
     * @return The JCR Repository URL
     *
     * @throws MalformedURLException If an error occurrs creating the
     *      <code>URL</code> instance.
     */
    public static URL createURL(Session session, String path)
        throws MalformedURLException {

        return new URL(REPOSITORY_SCHEME, "", -1,
            new FileParts(session, path, null).toString(),
            new JCRURLHandler(session));
    }

    /**
     * Creates a new JCRJar URL for the given session, archive and entry.
     *
     * @param session The repository session providing access to the archive.
     * @param path The absolute path to the archive. This must either be the
     *      property containing the archive or an item which resolves to such
     *      a property through its primary item chain. This must be an absolute
     *      path with a leading slash character. If this is <code>null</code>
     *      the root node path - <code>/</code> - is assumed.
     * @param entry The entry within the archive. If <code>null</code>, the URL
     *      provides access to the archive itself.
     *
     * @return The JCRJar URL
     *
     * @throws MalformedURLException If an error occurrs creating the
     *      <code>URL</code> instance.
     */
    public static URL createJarURL(Session session, String path, String entry)
        throws MalformedURLException {

        JCRJarURLHandler handler = new JCRJarURLHandler(session);
        String file = createURL(session, path).toExternalForm();

        // append entry spec if not null
        if (entry != null) {
            file += "!/" + entry;
        }

        return new URL(REPOSITORY_JAR_SCHEME, "", -1, file, handler);
    }
}
