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
package org.apache.jackrabbit.server.remoting.davex;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.server.util.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/** <code>JcrRemotingServlet</code>... */
public abstract class JcrRemotingServlet extends JCRWebdavServerServlet {

    private static Logger log = LoggerFactory.getLogger(JcrRemotingServlet.class);

    /**
     * the home init parameter. other relative filesystem paths are
     * relative to this location.
     */
    public static final String INIT_PARAM_HOME = "home";
    /**
     * the 'temp-directory' init parameter
     */
    public static final String INIT_PARAM_TMP_DIRECTORY = "temp-directory";
    /**
     * temp-dir attribute to be set to the servlet-context
     */
    public static final String ATTR_TMP_DIRECTORY = "remoting-servlet.tmpdir";

    /**
     * the 'temp-directory' init parameter
     */
    public static final String INIT_PARAM_BATCHREAD_CONFIG = "batchread-config";

    private static final String PARAM_DIFF = ":diff";
    private static final String PARAM_COPY = ":copy";
    private static final String PARAM_CLONE = ":clone";

    private BatchReadConfig brConfig;

    public void init() throws ServletException {
        super.init();

        brConfig = new BatchReadConfig();
        String brConfigParam = getServletConfig().getInitParameter(INIT_PARAM_BATCHREAD_CONFIG);
        if (brConfigParam == null) {
            log.debug("batchread-config missing -> initialize defaults.");
            brConfig.setDepth("nt:file", BatchReadConfig.DEPTH_INFINITE);
            brConfig.setDepth("nt:folder", 1);
            brConfig.setDefaultDepth(5);
        } else {
            try {
                InputStream in = getServletContext().getResourceAsStream(brConfigParam);
                if (in != null) {
                    brConfig.load(in);
                }
            } catch (IOException e) {
                log.debug("Unable to build resource filter provider.");
            }
        }

        // setup home directory
        String paramHome = getServletConfig().getInitParameter(INIT_PARAM_HOME);
        if (paramHome == null) {
            log.debug("missing init-param " + INIT_PARAM_HOME + ". using default: jr_home");
            paramHome = "jr_home";
        }
        File home;
        try {
            home = new File(paramHome).getCanonicalFile();
        } catch (IOException e) {
            throw new ServletException(INIT_PARAM_HOME + " invalid." + e.toString());
        }
        home.mkdirs();

        String tmp = getServletConfig().getInitParameter(INIT_PARAM_TMP_DIRECTORY);
        if (tmp == null) {
            log.warn("No " + INIT_PARAM_TMP_DIRECTORY + " specified. using 'tmp'");
            tmp = "tmp";
        }
        File tmpDirectory = new File(home, tmp);
        tmpDirectory.mkdirs();
        log.info("  temp-directory = " + tmpDirectory.getPath());
        getServletContext().setAttribute(ATTR_TMP_DIRECTORY, tmpDirectory);

        // force usage of custom locator factory.
        super.setLocatorFactory(new DavLocatorFactoryImpl(getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX)));
    }

    protected void doGet(WebdavRequest webdavRequest,
                         WebdavResponse webdavResponse,
                         DavResource davResource) throws IOException, DavException {
        if (canHandle(DavMethods.DAV_GET, webdavRequest, davResource)) {
            // return json representation of the requested resource
            try {
                Item item = ((JcrDavSession) webdavRequest.getDavSession()).getRepositorySession().getItem(davResource.getLocator().getRepositoryPath());
                if (item.isNode()) {
                    webdavResponse.setContentType("text/plain;charset=utf-8");
                    webdavResponse.setStatus(DavServletResponse.SC_OK);
                    
                    JsonWriter writer = new JsonWriter(webdavResponse.getWriter());
                    int depth = ((WrappingLocator) davResource.getLocator()).depth;
                    if (depth < BatchReadConfig.DEPTH_INFINITE) {
                        depth = getDepth((Node) item);
                    }
                    writer.write((Node) item, depth);
                } else {
                    // properties cannot be requested as json object.
                    throw new JcrDavException(new ItemNotFoundException("No node at " + item.getPath()), DavServletResponse.SC_NOT_FOUND);
                }
            } catch (RepositoryException e) {
                // should only get here if the item does not exist.
                log.debug(e.getMessage());
                throw new JcrDavException(e);
            }
        } else {
            super.doGet(webdavRequest, webdavResponse, davResource);
        }
    }

    protected void doPost(WebdavRequest webdavRequest, WebdavResponse webdavResponse, DavResource davResource)
            throws IOException, DavException {
        if (canHandle(DavMethods.DAV_POST, webdavRequest, davResource)) {
            // special remoting request: the defined parameters are exclusive
            // and cannot be combined.
            Session session = getRepositorySession(webdavRequest);
            RequestData data = new RequestData(webdavRequest, getTempDirectory(getServletContext()));
            String loc = null;
            try {
                String[] pValues;
                if ((pValues = data.getParameterValues(PARAM_CLONE)) != null) {
                    loc = clone(session, pValues, davResource.getLocator());
                } else if ((pValues = data.getParameterValues(PARAM_COPY)) != null) {
                    loc = copy(session, pValues, davResource.getLocator());
                } else if (data.getParameterValues(PARAM_DIFF) != null) {
                    String targetPath = davResource.getLocator().getRepositoryPath();
                    processDiff(session, targetPath, data);
                } else {
                    String targetPath = davResource.getLocator().getRepositoryPath();
                    loc = modifyContent(session, targetPath, data);
                }

                // TODO: append entity
                if (loc == null) {
                    webdavResponse.setStatus(HttpServletResponse.SC_OK);
                } else {
                    webdavResponse.setHeader(DeltaVConstants.HEADER_LOCATION, loc);
                    webdavResponse.setStatus(HttpServletResponse.SC_CREATED);
                }
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
                throw new JcrDavException(e);
            } catch (DiffException e) {
                log.warn(e.getMessage());
                Throwable cause = e.getCause();
                if (cause instanceof RepositoryException) {
                    throw new JcrDavException((RepositoryException) cause);
                } else {
                    throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid diff format.");
                }
            } finally {
                data.dispose();
            }
        } else {
            super.doPost(webdavRequest, webdavResponse, davResource);
        }
    }

    private boolean canHandle(int methodCode, WebdavRequest request, DavResource davResource) {
        DavResourceLocator locator = davResource.getLocator();
        switch (methodCode) {
            case DavMethods.DAV_GET:
                return davResource.exists() && ((WrappingLocator) locator).isJson;
            case DavMethods.DAV_POST:
                String ct = request.getContentType();
                return ct.startsWith("multipart/form-data") ||
                       ct.startsWith("application/x-www-form-urlencoded");
            default:
                return false;
        }
    }

    private int getDepth(Node node) throws RepositoryException {
        return brConfig.getDepth(node.getPrimaryNodeType().getName());
    }

    private static String clone(Session session, String[] cloneArgs, DavResourceLocator reqLocator) throws RepositoryException {
        Workspace wsp = session.getWorkspace();
        String destPath = null;
        for (int i = 0; i < cloneArgs.length; i++) {
            String[] args = cloneArgs[i].split(",");
            if (args.length == 4) {
                wsp.clone(args[0], args[1], args[2], new Boolean(args[3]).booleanValue());
                destPath = args[2];
            } else {
                throw new RepositoryException(":clone parameter must have a value consisting of the 4 args needed for a Workspace.clone() call.");
            }
        }
        return buildLocationHref(session, destPath, reqLocator);
    }

    private static String copy(Session session, String[] copyArgs, DavResourceLocator reqLocator) throws RepositoryException {
        Workspace wsp = session.getWorkspace();
        String destPath = null;
        for (int i = 0; i < copyArgs.length; i++) {
            String[] args = copyArgs[i].split(",");
            switch (args.length) {
                case 2:
                    wsp.copy(args[0], args[1]);
                    destPath = args[1];
                    break;
                case 3:
                    wsp.copy(args[0], args[1], args[2]);
                    destPath = args[2];
                    break;
                default:
                    throw new RepositoryException(":copy parameter must have a value consisting of 2 jcr paths or workspaceName plus 2 jcr paths separated by ','.");
            }
        }
        return buildLocationHref(session, destPath, reqLocator);
    }

    private static String buildLocationHref(Session s, String destPath, DavResourceLocator reqLocator) throws RepositoryException {
        if (destPath != null) {
            NodeIterator it = s.getRootNode().getNodes(destPath.substring(1));
            Node n = null;
            while (it.hasNext()) {
                n = it.nextNode();
            }
            if (n != null) {
                DavResourceLocator loc = reqLocator.getFactory().createResourceLocator(reqLocator.getPrefix(), reqLocator.getWorkspacePath(), n.getPath(), false);
                return loc.getHref(true);
            }
        }

        // unable to determine -> no location header sent back.
        return null;
    }

    private static void processDiff(Session session, String targetPath, RequestData data)
            throws RepositoryException, DiffException, IOException {

        String[] diffs = data.getParameterValues(PARAM_DIFF);
        DiffHandler handler = new JsonDiffHandler(session, targetPath, data);
        DiffParser parser = new DiffParser(handler);

        for (int i = 0; i < diffs.length; i++) {
            boolean success = false;
            try {
                String diff = diffs[i];
                parser.parse(diff);

                session.save();
                success = true;
            } finally {
                if (!success) {
                    session.refresh(false);
                }
            }
        }
    }

    /**
     * TODO: doesn't work properly with intermedite SNS-nodes
     * TODO: doesn't respect jcr:uuid properties.
     *
     * @param session
     * @param targetPath
     * @param data
     * @throws RepositoryException
     * @throws DiffException
     */
    private static String modifyContent(Session session, String targetPath, RequestData data)
            throws RepositoryException, DiffException {

        JsonDiffHandler dh = new JsonDiffHandler(session, targetPath, data);
        boolean success = false;
        try {
            for (Iterator pNames = data.getParameterNames(); pNames.hasNext();) {
                String paramName = pNames.next().toString();
                String propPath = dh.getItemPath(paramName);
                String parentPath = Text.getRelativeParent(propPath, 1);

                if (!session.itemExists(parentPath) || !session.getItem(parentPath).isNode()) {
                    createNode(session, parentPath, data);
                }

                if (JcrConstants.JCR_PRIMARYTYPE.equals(Text.getName(propPath))) {
                    // already handled by createNode above -> ignore
                    continue;
                }
                // none of the special properties -> let the diffhandler take care
                // of the property creation/modification.
                dh.setProperty(paramName, null);
            }

            // save the complete set of modifications
            session.save();
            success = true;
        } finally {
            if (!success) {
                session.refresh(false);
            }
        }
        return null; // TODO build loc-href if items were created.
    }

    /**
     * 
     * @param session
     * @param nodePath
     * @param data
     * @throws RepositoryException
     */
    private static void createNode(Session session, String nodePath, RequestData data) throws RepositoryException {
        Node parent = session.getRootNode();
        String[] smgts = Text.explode(nodePath, '/');

        for (int i = 0; i < smgts.length; i++) {
            String nodeName = smgts[i];
            if (parent.hasNode(nodeName)) {
                parent = parent.getNode(nodeName);
            } else {
                // need to create the node
                // TODO: won't work for SNS
                String nPath = parent.getPath() + "/" + nodeName;
                String ntName = data.getParameter(nPath + "/" + JcrConstants.JCR_PRIMARYTYPE);
                if (ntName == null) {
                    parent = parent.addNode(nodeName);
                } else {
                    parent = parent.addNode(nodeName, ntName);
                }
            }
        }
    }

    /**
     *
     * @param request
     * @return
     * @throws DavException
     */
    private static Session getRepositorySession(WebdavRequest request) throws DavException {
        DavSession ds = request.getDavSession();
        return JcrDavSession.getRepositorySession(ds);
    }

    /**
     * Returns the temp directory
     *
     * @return the temp directory
     */
    private static File getTempDirectory(ServletContext servletCtx) {
        return (File) servletCtx.getAttribute(ATTR_TMP_DIRECTORY);
    }

    //--------------------------------------------------------------------------
    /**
     * TODO: TOBEFIXED will not behave properly if resource path (i.e. item name)
     * TODO            ends with .json extension and/or contains a depth-selector pattern.
     */
    private static class DavLocatorFactoryImpl extends org.apache.jackrabbit.webdav.jcr.DavLocatorFactoryImpl {

        public DavLocatorFactoryImpl(String s) {
            super(s);
        }

        public DavResourceLocator createResourceLocator(String string, String string1) {
            return new WrappingLocator(super.createResourceLocator(string, string1), isJson(string1), getDepth(string1));
        }

        public DavResourceLocator createResourceLocator(String string, String string1, String string2) {
            return super.createResourceLocator(string, string1, string2);
        }

        public DavResourceLocator createResourceLocator(String string, String string1, String string2, boolean b) {
            return super.createResourceLocator(string, string1, string2, b);
        }

        protected String getRepositoryPath(String resourcePath, String wspPath) {
            if (resourcePath == null) {
                return null;
            }
            String rp = resourcePath;
            if (isJson(rp)) {
                rp = resourcePath.substring(0, resourcePath.lastIndexOf('.'));
                int pos = rp.lastIndexOf(".");
                if (pos > -1) {
                    String depthStr = rp.substring(pos + 1);
                    try {
                        Integer.parseInt(depthStr);
                        rp = rp.substring(0, pos);
                    } catch (NumberFormatException e) {
                        // ignore return rp
                    }
                }
            }
            return super.getRepositoryPath(rp, wspPath);
        }

        private static boolean isJson(String s) {
            return s.endsWith(".json");
        }

        private static int getDepth(String s) {
            int depth = Integer.MIN_VALUE;
            if (isJson(s)) {
                String tmp = s.substring(0, s.lastIndexOf('.'));
                int pos = tmp.lastIndexOf(".");
                if (pos > -1) {
                    String depthStr = tmp.substring(pos + 1);
                    try {
                        depth = Integer.parseInt(depthStr);
                    } catch (NumberFormatException e) {
                        // missing depth
                    }
                }
            }
            return depth;
        }
    }

    private static class WrappingLocator implements DavResourceLocator {

        private final DavResourceLocator loc;
        private final boolean isJson;
        private final int depth;

        private WrappingLocator(DavResourceLocator loc, boolean isJson, int depth) {
            this.loc = loc;
            this.isJson = isJson;
            this.depth = depth;
        }
        public String getPrefix() {
            return loc.getPrefix();
        }
        public String getResourcePath() {
            return loc.getResourcePath();
        }
        public String getWorkspacePath() {
            return loc.getWorkspacePath();
        }
        public String getWorkspaceName() {
            return loc.getWorkspaceName();
        }
        public boolean isSameWorkspace(DavResourceLocator davResourceLocator) {
            return loc.isSameWorkspace(davResourceLocator);
        }
        public boolean isSameWorkspace(String string) {
            return loc.isSameWorkspace(string);
        }
        public String getHref(boolean b) {
            return loc.getHref(b);
        }
        public boolean isRootLocation() {
            return loc.isRootLocation();
        }
        public DavLocatorFactory getFactory() {
            return loc.getFactory();
        }
        public String getRepositoryPath() {
            return loc.getRepositoryPath();
        }
    }
}
