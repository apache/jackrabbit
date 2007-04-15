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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.jcr.Session;

/**
 * The <code>JCRJarURLHandler</code> is the <code>URLStreamHandler</code> for
 * Java Archive URLs for archives from a JCR Repository URLs (JCRJar URL). The
 * scheme for such ULRs will be <code>jar</code> while the file part of the URL
 * has the scheme <code>jcr</code>.
 * <p>
 * JCRJar URLs have not been standardized yet and may only be created in the
 * context of an existing <code>Session</code>. Therefore this handler is not
 * globally available and JCR Repository URLs may only be created through the
 * factory methods in the {@link org.apache.jackrabbit.net.URLFactory} class.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 *
 * @author Felix Meschberger
 *
 * @see org.apache.jackrabbit.net.JCRJarURLConnection
 * @see org.apache.jackrabbit.net.URLFactory
 * @see org.apache.jackrabbit.net.URLFactory#createJarURL(Session, String, String)
 */
class JCRJarURLHandler extends JCRURLHandler {

    /**
     * Creates an instance of this handler class.
     *
     * @param session The <code>Session</code> supporting this handler. This
     *      must not be <code>null</code>.
     *
     * @throws NullPointerException if <code>session</code> is <code>null</code>.
     */
    JCRJarURLHandler(Session session) {
        super(session);
    }

    //---------- URLStreamHandler abstracts ------------------------------------

    /**
     * Gets a connection object to connect to an JCRJar URL.
     *
     * @param url The JCRJar URL to connect to.
     *
     * @return An instance of the {@link JCRJarURLConnection} class.
     *
     * @see JCRJarURLConnection
     */
    protected URLConnection openConnection(URL url) {
        return new JCRJarURLConnection(url, this);
    }

    /**
     * Parses the string representation of a <code>URL</code> into a
     * <code>URL</code> object.
     * <p>
     * If there is any inherited context, then it has already been copied into
     * the <code>URL</code> argument.
     * <p>
     * The <code>parseURL</code> method of <code>URLStreamHandler</code>
     * parses the string representation as if it were an <code>http</code>
     * specification. Most URL protocol families have a similar parsing. A
     * stream protocol handler for a protocol that has a different syntax must
     * override this routine.
     *
     * @param url the <code>URL</code> to receive the result of parsing the
     *            spec.
     * @param spec the <code>String</code> representing the URL that must be
     *            parsed.
     * @param start the character index at which to begin parsing. This is just
     *            past the '<code>:</code>' (if there is one) that specifies
     *            the determination of the protocol name.
     * @param limit the character position to stop parsing at. This is the end
     *            of the string or the position of the "<code>#</code>"
     *            character, if present. All information after the sharp sign
     *            indicates an anchor.
     */
    protected void parseURL(URL url, String spec, int start, int limit) {
        // protected void parseURL(URL url, String s, int i, int j)

        String file = null;
        String ref = null;

        // split the reference and file part
        int hash = spec.indexOf('#', limit);
        boolean emptyFile = hash == start;
        if (hash > -1) {
            ref = spec.substring(hash + 1, spec.length());
            if (emptyFile) {
                file = url.getFile();
            }
        }

        boolean isSpecAbsolute = spec.substring(0, 4).equalsIgnoreCase("jar:");
        spec = spec.substring(start, limit);

        if (isSpecAbsolute) {

            // get the file part from the absolute spec
            file = parseAbsoluteSpec(spec);

        } else if (!emptyFile) {

            // build the file part from the url and relative spec
            file = parseContextSpec(url, spec);

            // split archive and entry names
            int bangSlash = indexOfBangSlash(file);
            String archive = file.substring(0, bangSlash);
            String entry = file.substring(bangSlash);

            // collapse /../, /./ and //
            entry = canonizeString(entry);

            file = archive + entry;

        }

        setURL(url, "jar", "", -1, null, null, file, null, ref);
    }

    //---------- internal -----------------------------------------------------

    /**
     * Finds the position of the bang slash (!/) in the file part of the URL.
     */
    static int indexOfBangSlash(String file) {

        for (int i = file.length(); (i = file.lastIndexOf('!', i)) != -1; i--) {
            if (i != file.length() - 1 && file.charAt(i + 1) == '/') {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * Parses the URL spec and checks whether it contains a bang slash and
     * whether it would get a valid URL. Returns the same value if everything is
     * fine else a <code>NullPointerException</code> is thrown.
     *
     * @param spec The URL specification to check.
     * @return The <code>spec</code> if everything is ok.
     * @throws NullPointerException if either no bang slash is contained in the
     *             spec or if the spec without the bang slash part would not be
     *             a valid URL.
     */
    private String parseAbsoluteSpec(String spec) {

        // find and check bang slash
        int bangSlash = indexOfBangSlash(spec);
        if (bangSlash == -1) {
            throw new NullPointerException("no !/ in spec");
        }

        try {

            String testSpec = spec.substring(0, bangSlash - 1);
            URI uri = new URI(testSpec);

            // verify the scheme is the JCR Repository Scheme
            if (!URLFactory.REPOSITORY_SCHEME.equals(uri.getScheme())) {
                throw new URISyntaxException(testSpec,
                    "Unsupported Scheme " + uri.getScheme(), 0);
            }

        } catch (URISyntaxException use) {

            throw new NullPointerException("invalid url: " + spec + " (" + use
                + ")");

        }

        return spec;
    }

    /**
     * Merges the specification and the file part of the URL respecting the bang
     * slashes. If the specification starts with a slash, it is regarded as a
     * complete path of a archive entry and replaces an existing archive entry
     * specification in the url. Examples :<br>
     * <table>
     * <tr>
     * <th align="left">file
     * <th align="left">spec
     * <th align="left">result
     * <tr>
     * <td>/some/file/path.jar!/
     * <td>/some/entry/path
     * <td>/some/file/path.jar!/some/entry/path
     * <tr>
     * <td>/some/file/path.jar!/some/default
     * <td>/some/entry/path
     * <td>/some/file/path.jar!/some/entry/path </table>
     * <p>
     * If the specification is not absolutes it replaces the last file name part
     * if the file name does not end with a slash. Examples :<br>
     * <table>
     * <tr>
     * <th align="left">file
     * <th align="left">spec
     * <th align="left">result
     * <tr>
     * <td>/some/file/path.jar!/
     * <td>/some/entry/path
     * <td>/some/file/path.jar!/some/entry/path
     * <tr>
     * <td>/some/file/path.jar!/some/default
     * <td>/some/entry/path
     * <td>/some/file/path.jar!/some/entry/path </table>
     *
     * @param url The <code>URL</code> whose file part is used
     * @param spec The specification to merge with the file part
     * @throws NullPointerException If the specification starts with a slash and
     *             the URL does not contain a slash bang or if the specification
     *             does not start with a slash and the file part of the URL does
     *             is not an absolute file path.
     */
    private String parseContextSpec(URL url, String spec) {

        // spec is relative to this file
        String file = url.getFile();

        // if the spec is absolute path, it is an absolute entry spec
        if (spec.startsWith("/")) {

            // assert the bang slash in the original URL
            int bangSlash = indexOfBangSlash(file);
            if (bangSlash == -1) {
                throw new NullPointerException("malformed context url:" + url
                    + ": no !/");
            }

            // remove bang slash part from the original file
            file = file.substring(0, bangSlash);
        }

        // if the file is not a directory and spec is a relative file path
        if (!file.endsWith("/") && !spec.startsWith("/")) {

            // find the start of the file name in the url file path
            int lastSlash = file.lastIndexOf('/');
            if (lastSlash == -1) {
                throw new NullPointerException("malformed context url:" + url);
            }

            // cut off the file name from the URL file path
            file = file.substring(0, lastSlash + 1);
        }

        // concat file part and the spec now
        return file + spec;
    }

    public String canonizeString(String s) {
        int i = 0;
        int k = s.length();
        while ((i = s.indexOf("/../")) >= 0)
            if ((k = s.lastIndexOf('/', i - 1)) >= 0)
                s = s.substring(0, k) + s.substring(i + 3);
            else
                s = s.substring(i + 3);
        while ((i = s.indexOf("/./")) >= 0)
            s = s.substring(0, i) + s.substring(i + 2);
        while (s.endsWith("/..")) {
            int j = s.indexOf("/..");
            int l;
            if ((l = s.lastIndexOf('/', j - 1)) >= 0)
                s = s.substring(0, l + 1);
            else
                s = s.substring(0, j);
        }
        if (s.endsWith("/.")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
