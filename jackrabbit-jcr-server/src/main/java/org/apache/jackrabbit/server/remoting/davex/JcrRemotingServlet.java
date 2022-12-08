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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.server.util.RequestData;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl;
import org.apache.jackrabbit.webdav.observation.SubscriptionManager;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>JcrRemotingServlet</code> is an extended version of the
 * {@link org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet JCR Remoting Servlet}
 * that provides improved
 * <ul>
 * <li><a href="#bread">Batch read</a></li>
 * <!-- <li><a href="#mread">Multi read</a></li> -->
 * <li><a href="#bwrite">Batch write</a></li>
 * </ul>
 * functionality and supports cross workspace copy and cloning.
 * 
 * <h2 id="bread">Batch Read</h2>
 *
 * Upon RepositoryService.getItemInfos a JSON object is composed containing
 * the information for the requested node and its child items up to a
 * specified or configuration determined depth.
 * <p>
 * Batch read is triggered by adding a '.json' extension to the resource href.
 * Optionally the client may explicitly specify the desired batch read depth
 * by appending '.depth.json' extension. If no json extension is present the
 * GET request is processed by the base servlet.
 * <p>
 * The JSON writer applies the following rules:
 * 
 * <pre>
 * - Nodes are represented as JSON objects.
 *
 * - Each Node has its properties included as JSON key/value pairs.
 *
 * - Single valued Properties are simple key/value pairs.
 *
 * - Multi valued Properties are represented as JSON array.
 *
 * - Each Node has its child nodes included as long a maximal depths is not reached.
 * 
 * - Nodes without any child nodes get a special JSON member named
 *   ::NodeIteratorSize, whose value is zero.
 *
 * - If the maximal depth is reached only name, index and unique id of the
 *   direct child are included (incomplete node info). In order to obtain
 *   the complete information the client sends another GET with .json extension.
 * </pre>
 * 
 * Same name sibling nodes and properties whose type cannot be unambiguously be
 * extracted from the JSON on the client side need some special handling:
 * 
 * <pre>
 * - Node with index &gt; 1, get a JSON key consisting of
 *   Node.getName() + "[" + Node.getIndex() + "]" 
 *
 * - Binary Property
 *   JSON value = length of the JCR value.
 *   The JCR value must be retrieved separately.
 *
 * - Name, Path, Reference and Date Property
 *   The JSON member representing the Property (name, value) is preceded by a
 *   special member consisting of
 *   JSON key = ":" + Property.getName()
 *   JSON value = PropertyType.nameFromValue(Property.getType())
 *
 * - Multi valued properties with Property.getValues().length == 0 will be
 *   treated as special property types above (extra property indicating the
 *   type of the property).
 *
 * - Double Property
 *   JSON value must not have any trailing ".0" removed.
 * </pre>
 *
 * <h2 id="mread">Multi Read</h2>
 * <p>
 * Since Jackrabbit 2.3.6 it is also possible to request multiple subtrees
 * in a single request. This is done by adding one or more ":include"
 * parameters to a batch read request describe above. These extra parameters
 * specify the (relative) paths of all the nodes to be included in the
 * response. The response is a JSON object whose "nodes" property contains
 * all the selected nodes keyed by path. Missing nodes are not included in
 * the response. Each included node is serialized as defined above for
 * <a href="#bread">batch read</a>.
 * <p>
 * Example:
 * <pre>
 * $ curl 'http://.../parent.json?:path=child1&amp;:path=child2'
 * {"nodes":{"/parent/child1":{...},"/parent/child2":{...}}}
 * </pre>
 *
 * <h2 id="bwrite">Batch Write</h2>
 *
 * The complete SPI Batch is sent to the server in a single request, currently a
 * POST request containing a custom ":diff" parameter.
 * <br>
 * <i>NOTE</i> that this is targeted to be replaced by a PATCH request.
 *
 * <h3>Diff format</h3>
 *
 * The diff parameter currently consists of JSON-like key-value pairs with the
 * following special requirements:
 *
 * <pre>
 *   diff       ::= members
 *   members    ::= pair | pairs
 *   pair       ::= key " : " value
 *   pairs      ::= pair line-end pair | pair line-end pairs
 *   line-end   ::= "\r\n" | "\n" | "\r"
 *   key        ::= diffchar path
 *   diffchar   ::= "+" | "^" | "-" | "&gt;"
 *   path       ::= abspath | relpath
 *   abspath    ::= * absolute path to an item *
 *   relpath    ::= * relpath from item at request URI to an item *
 *   value      ::= value+ | value- | value^ | value&gt;
 *   value+     ::= * a JSON object *
 *   value-     ::= ""
 *   value^     ::= * any JSON value except JSON object *
 *   value&gt;     ::= path | path "#before" | path "#after" | "#first" | "#last"
 * </pre>
 *
 * In other words:
 * <ul>
 * <li>diff consists of one or more key-value pair(s)</li>
 * <li>key must start with a diffchar followed by a rel. or abs. item path</li>
 * <li>diffchar being any of "+", "^", "-" or "&gt;" representing the transient
 * item modifications as follows
 * <pre>
 *   "+" addNode
 *   "^" setProperty / setValue / removeProperty
 *   "-" remove Item
 *   "&gt;" move / reorder Nodes
 * </pre>
 * </li>
 * <li>key must be separated from the value by a ":" surrounded by whitespace.</li>
 * <li>two pairs must be separated by a line end</li>
 * <li>the format of the value depends on the diffchar</li>
 * <li>for moving around node the value must consist of a abs. or rel. path.
 * in contrast reordering of existing nodes is achieved by appending a trailing
 * order position hint (#first, #last, #before or #after)</li>
 * </ul>
 *
 * <i>NOTE</i> the following special handling of JCR properties of type
 * Binary, Name, Path, Date and Reference:
 * <ul>
 * <li>the JSON value must be missing</li>
 * <li>the POST request is expected to contain extra multipart(s) or request
 * parameter(s) for the property value(s)</li>
 * <li>the content type of the extra parts/params must reflect the property
 * type:"jcr-value/" + PropertyType.nameFromValue(Property.getType).toLowerCase()</li>
 * </ul>
 * 
 * @see <a href="http://www.json.org/">www.json.org</a> for the definition of
 * JSON object and JSON value.
 */
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

    /**
     * the 'protectedhandlers-config' init paramter. this parameter contains the XML
     * configuration file for protected item remove handlers.
     */
    public static final String INIT_PARAM_PROTECTED_HANDLERS_CONFIG = "protectedhandlers-config";
    
    private static final String PARAM_DIFF = ":diff";
    private static final String PARAM_COPY = ":copy";
    private static final String PARAM_CLONE = ":clone";
    private static final String PARAM_INCLUDE = ":include";

    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    private BatchReadConfig brConfig;
    private ProtectedRemoveManager protectedRemoveManager;

    @Override
    public void init() throws ServletException {
        super.init();

        brConfig = new BatchReadConfig();
        String brConfigParam = getServletConfig().getInitParameter(INIT_PARAM_BATCHREAD_CONFIG);
        if (brConfigParam == null) {
            // TODO: define default values.
            log.debug("batchread-config missing -> initialize defaults.");
            brConfig.setDepth("nt:file", BatchReadConfig.DEPTH_INFINITE);
            brConfig.setDefaultDepth(5);
        } else {
            try {
                InputStream in = getServletContext().getResourceAsStream(brConfigParam);
                if (in != null) {
                    brConfig.load(in);
                }
            } catch (IOException e) {
                log.debug("Unable to build BatchReadConfig from " + brConfigParam + ".");
            }
        }

        String protectedHandlerConfig = getServletConfig().getInitParameter(INIT_PARAM_PROTECTED_HANDLERS_CONFIG);
        InputStream in = null;
        try {
            in = getServletContext().getResourceAsStream(protectedHandlerConfig);
            if (in != null){
                protectedRemoveManager = new ProtectedRemoveManager();
                protectedRemoveManager.load(in);
            } else {
                //Config might be direct class implementation
                protectedRemoveManager = new ProtectedRemoveManager(protectedHandlerConfig);
            }
        } catch (IOException e) {
            log.debug("Unable to create ProtectedRemoveManager from " + protectedHandlerConfig , e);
        } finally{
            if (in != null){
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }

        // Determine the configured location for temporary files used when
        // processing file uploads. Since JCR-3029 the default is the
        // standard java.io.tmpdir location, but the presence of explicit
        // configuration parameters restores the original behavior.
        File tmp = null;
        ServletConfig config = getServletConfig();
        String paramHome = config.getInitParameter(INIT_PARAM_HOME);
        String paramTemp = config.getInitParameter(INIT_PARAM_TMP_DIRECTORY);
        if (paramHome != null || paramTemp != null) {
            if (paramHome == null) {
                log.debug("Missing init-param " + INIT_PARAM_HOME
                        + ". Using default: 'jackrabbit'");
                paramHome = "jackrabbit";
            } else if (paramTemp == null) {
                log.debug("Missing init-param " + INIT_PARAM_TMP_DIRECTORY
                        + ". Using default: 'tmp'");
                paramTemp = "tmp";
            }

            tmp = new File(paramHome, paramTemp);
            try {
                tmp = tmp.getCanonicalFile();
                tmp.mkdirs();
                log.debug("  temp-directory = " + tmp.getPath());
            } catch (IOException e) {
                log.warn("Invalid temporary directory " + tmp.getPath()
                        + ", using system default instead", e);
                tmp = null;
            }
        }
        getServletContext().setAttribute(ATTR_TMP_DIRECTORY, tmp);

        // force usage of custom locator factory.
        super.setLocatorFactory(new DavLocatorFactoryImpl(getResourcePathPrefix()));
    }

    protected String getResourcePathPrefix() {
        return getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX);
    }

    @Override
    public DavResourceFactory getResourceFactory() {
        return new ResourceFactoryImpl(txMgr, subscriptionMgr);
    }

    @Override
    protected void doGet(WebdavRequest webdavRequest,
                         WebdavResponse webdavResponse,
                         DavResource davResource) throws IOException, DavException {
        if (canHandle(DavMethods.DAV_GET, webdavRequest, davResource)) {
            // return json representation of the requested resource
            DavResourceLocator locator = davResource.getLocator();
            String path = locator.getRepositoryPath();

            Session session = getRepositorySession(webdavRequest);
            try {
                Node node = session.getNode(path);
                int depth = ((WrappingLocator) locator).getDepth();

                webdavResponse.setContentType(CONTENT_TYPE_APPLICATION_JSON);
                webdavResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
                webdavResponse.setStatus(DavServletResponse.SC_OK);
                JsonWriter writer = new JsonWriter(webdavResponse.getWriter());

                String[] includes = webdavRequest.getParameterValues(PARAM_INCLUDE);
                if (includes == null) {
                    if (depth < BatchReadConfig.DEPTH_INFINITE) {
                        NodeType type = node.getPrimaryNodeType();
                        depth = brConfig.getDepth(type.getName());
                    }
                    writer.write(node, depth);
                } else {
                    writeMultiple(writer, node, includes, depth);
                }
            } catch (PathNotFoundException e) {
                // properties cannot be requested as json object.
                throw new JcrDavException(
                        new ItemNotFoundException("No node at " + path),
                        DavServletResponse.SC_NOT_FOUND);
            } catch (RepositoryException e) {
                // should only get here if the item does not exist.
                log.debug(e.getMessage());
                throw new JcrDavException(e);
            }
        } else {
            super.doGet(webdavRequest, webdavResponse, davResource);
        }
    }

    private void writeMultiple(
            JsonWriter writer, Node node, String[] includes, int depth)
            throws RepositoryException, IOException {
        Collection<Node> nodes = new ArrayList<Node>();
        Set<String> alreadyAdded = new HashSet<String>();
        for (String include : includes) {
            try {
                Node n;
                if (include.startsWith("/")) {
                    n = node.getSession().getNode(include);
                } else {
                    n = node.getNode(include);
                }
                String np = n.getPath();
                if (!alreadyAdded.contains(np)) {
                    nodes.add(n);
                    alreadyAdded.add(np);
                }
            } catch (PathNotFoundException e) {
                // skip missing node
            }
        }
        writer.write(nodes, depth);
    }

    @Override
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
                String[] includes = null; // multi-read over POST
                if ((pValues = data.getParameterValues(PARAM_CLONE)) != null) {
                    loc = clone(session, pValues, davResource.getLocator());
                } else if ((pValues = data.getParameterValues(PARAM_COPY)) != null) {
                    loc = copy(session, pValues, davResource.getLocator());
                } else if (data.getParameterValues(PARAM_DIFF) != null) {
                    String targetPath = davResource.getLocator().getRepositoryPath();
                    processDiff(session, targetPath, data, protectedRemoveManager);
                } else if ((pValues = data.getParameterValues(PARAM_INCLUDE)) != null
                        && canHandle(DavMethods.DAV_GET, webdavRequest, davResource)) {
                    includes = pValues;
                } else {
                    String targetPath = davResource.getLocator().getRepositoryPath();
                    loc = modifyContent(session, targetPath, data, protectedRemoveManager);
                }

                // TODO: append entity
                if (loc == null) {
                    webdavResponse.setStatus(HttpServletResponse.SC_OK);
                    if (includes != null) {
                        webdavResponse.setContentType(CONTENT_TYPE_APPLICATION_JSON);
                        webdavResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        JsonWriter writer = new JsonWriter(webdavResponse.getWriter());

                        DavResourceLocator locator = davResource.getLocator();
                        String path = locator.getRepositoryPath();

                        Node node = session.getNode(path);
                        int depth = ((WrappingLocator) locator).getDepth();

                        writeMultiple(writer, node, includes, depth);
                    }
                } else {
                    webdavResponse.setHeader(DeltaVConstants.HEADER_LOCATION, loc);
                    webdavResponse.setStatus(HttpServletResponse.SC_CREATED);
                }
            } catch (RepositoryException e) {
                log.warn(e.getMessage(), e);
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
                return davResource.exists() && (locator instanceof WrappingLocator)
                        && ((WrappingLocator) locator).isJsonRequest;
            case DavMethods.DAV_POST:
                String ct = request.getContentType();
                if (ct == null) {
                    return false;
                } else {
                    int semicolon = ct.indexOf(';');
                    if (semicolon >= 0) {
                        ct = ct.substring(0, semicolon);
                    }
                    ct = ct.trim().toLowerCase(Locale.ENGLISH);
                    return "multipart/form-data".equals(ct) || "application/x-www-form-urlencoded".equals(ct);
                }
            default:
                return false;
        }
    }

    private static String clone(Session session, String[] cloneArgs, DavResourceLocator reqLocator) throws RepositoryException {
        Workspace wsp = session.getWorkspace();
        String destPath = null;
        for (String cloneArg : cloneArgs) {
            String[] args = cloneArg.split(",");
            if (args.length == 4) {
                wsp.clone(args[0], args[1], args[2], Boolean.valueOf(args[3]));
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
        for (String copyArg : copyArgs) {
            String[] args = copyArg.split(",");
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

    private static void processDiff(Session session, String targetPath, RequestData data, ProtectedRemoveManager protectedRemoveManager)
            throws RepositoryException, DiffException, IOException {

        String[] diffs = data.getParameterValues(PARAM_DIFF);
        DiffHandler handler = new JsonDiffHandler(session, targetPath, data, protectedRemoveManager);
        DiffParser parser = new DiffParser(handler);

        for (String diff : diffs) {
            boolean success = false;
            try {
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
     * TODO: doesn't work properly with intermediate SNS-nodes
     * TODO: doesn't respect jcr:uuid properties.
     */
    private static String modifyContent(Session session, String targetPath, RequestData data, ProtectedRemoveManager protectedRemoveManager)
            throws RepositoryException, DiffException {

        JsonDiffHandler dh = new JsonDiffHandler(session, targetPath, data, protectedRemoveManager);
        boolean success = false;
        try {
            for (Iterator<String> pNames = data.getParameterNames(); pNames.hasNext();) {
                String paramName = pNames.next();
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

    private static void createNode(Session session, String nodePath, RequestData data) throws RepositoryException {
        Node parent = session.getRootNode();
        String[] smgts = Text.explode(nodePath, '/');

        for (String nodeName : smgts) {
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
     * Locator factory that specially deals with hrefs having a .json extension.
     */
    private static class DavLocatorFactoryImpl extends org.apache.jackrabbit.webdav.jcr.DavLocatorFactoryImpl {

        public DavLocatorFactoryImpl(String s) {
            super(s);
        }

        @Override
        public DavResourceLocator createResourceLocator(String prefix, String href) {
            return createResourceLocator(prefix, href, false);
        }

        @Override
        public DavResourceLocator createResourceLocator(String prefix, String href, boolean forDestination) {
            DavResourceLocator loc = super.createResourceLocator(prefix, href);
            if (!forDestination && endsWithJson(href)) {
                loc = new WrappingLocator(super.createResourceLocator(prefix, href));
            }
            return loc;
        }

        @Override
        public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
            DavResourceLocator loc = super.createResourceLocator(prefix, workspacePath, path, isResourcePath);
            if (isResourcePath && endsWithJson(path)) {
                loc = new WrappingLocator(loc);
            }
            return loc;
        }

        private static boolean endsWithJson(String s) {
            return s.endsWith(".json");
        }
    }

    /**
     * Resource locator that removes trailing .json extensions and depth
     * selector that do not form part of the repository path.
     * As the locator and it's factory do not have access to a JCR session
     * the <code>extraJson</code> flag may be reset later on.
     *
     * @see ResourceFactoryImpl#getItem(org.apache.jackrabbit.webdav.jcr.JcrDavSession, org.apache.jackrabbit.webdav.DavResourceLocator)  
     */
    private static class WrappingLocator implements DavResourceLocator {

        private final DavResourceLocator loc;
        private boolean isJsonRequest = true;
        private int depth = Integer.MIN_VALUE;
        private String repositoryPath;

        private WrappingLocator(DavResourceLocator loc) {
            this.loc = loc;
        }

        private void extract() {
            String rp = loc.getRepositoryPath();
            rp = rp.substring(0, rp.lastIndexOf('.'));
            int pos = rp.lastIndexOf('.');
            if (pos > -1) {
                String depthStr = rp.substring(pos + 1);
                try {
                    depth = Integer.parseInt(depthStr);
                    rp = rp.substring(0, pos);
                } catch (NumberFormatException e) {
                    // apparently no depth-info -> ignore
                }
            }
            repositoryPath = rp;
        }

        private int getDepth() {
            if (isJsonRequest) {
                if (repositoryPath == null) {
                    extract();
                }
                return depth;
            } else {
                return Integer.MIN_VALUE;
            }
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
            if (isJsonRequest) {
                if (repositoryPath == null) {
                    extract();
                }
                return repositoryPath;
            } else {
                return loc.getRepositoryPath();
            }
        }
    }

    /**
     * Resource factory used to make sure that the .json extension was properly
     * interpreted.
     */
    private static class ResourceFactoryImpl extends org.apache.jackrabbit.webdav.jcr.DavResourceFactoryImpl {

        /**
         * Create a new <code>DavResourceFactoryImpl</code>.
         *
         * @param txMgr
         * @param subsMgr
         */
        public ResourceFactoryImpl(TxLockManagerImpl txMgr, SubscriptionManager subsMgr) {
            super(txMgr, subsMgr);
        }

        @Override
        protected Item getItem(JcrDavSession sessionImpl, DavResourceLocator locator) throws PathNotFoundException, RepositoryException {
            if (locator instanceof WrappingLocator && ((WrappingLocator)locator).isJsonRequest) {
                // check if the .json extension has been correctly interpreted.
                Session s = sessionImpl.getRepositorySession();
                try {
                    if (s.itemExists(((WrappingLocator)locator).loc.getRepositoryPath())) {
                        // an item exists with the original calculated repo-path
                        // -> assume that the repository item path ends with .json
                        // or .depth.json. i.e. .json wasn't an extra extension
                        // appended to request the json-serialization of the node.
                        // -> change the flag in the WrappingLocator correspondingly.
                        ((WrappingLocator) locator).isJsonRequest = false;
                    }
                } catch (RepositoryException e) {
                    // if the unmodified repository path isn't valid (e.g. /a/b[2].5.json)
                    // -> ignore.
                }
            }
            return super.getItem(sessionImpl, locator);
        }
    }
}
