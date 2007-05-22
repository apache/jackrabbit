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
package org.apache.jackrabbit.tools.importexport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeWriter;
import org.apache.jackrabbit.name.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JcrImportExport tool for JCR-level import and export of content.
 *
 * <h1>Build and install</h1>
 * After building, the jar file jackrabbit-jcr-import-export-tool-1.4-SNAPSHOT.jar can
 * be "installed" by unpacking it's lib directory which contains libraries.
 *
 * <h1>Export</h1>
 * Options (all required):
 * <ul>
 * <li>-c filename  : specifies the filename of the source repository configuration (repository.xml) </li>
 * <li>-d directory : specifies the source repository home directory </li>
 * <li>-nt          : specifies to include the nodetypes in the export</li>
 * <li>-ns          : specifies to include the namespace mappings in the export</li>
 * <li>-e filename [path list] : specifies the name of the zip file to which the exported content will be written,
 * and also a list of absolute paths of nodes that will be exported</li>
 * </ul>
 *
 * To export the <code>/myroot</code> node and all namespace mappings and custom nodetypes:
 * <br>
 * <code>java -Xmx512M -jar JcrImportExportTool-1.4-SNAPSHOT.jar -c repository.xml -d repository/ -ns -nt -e export.zip /myroot</code>
 *
 *
 * <h1>Import</h1>
 * Options (all required):
 * <ul>
 * <li>-c filename  : specifies the filename of the target repository configuration (repository.xml) </li>
 * <li>-d directory : specifies the target repository home directory </li>
 * <li>-nt          : specifies to include the nodetypes in the import</li>
 * <li>-ns          : specifies to include the namespace mappings in the import</li>
 * <li>-i filename  : specifies the name of the zip file from which the content will be read</li>
 * </ul>
 *
 * To import the content in the previously generated <code>export.zip</code> in another repository:
 * <br>
 * <code>java -Xmx512M -jar JcrImportExportTool-1.4-SNAPSHOT.jar -c repository2.xml -d repository2/ -ns -nt -i export.zip</code>
 *
 * <h1>Things to do</h1>
 * <ul>
 * <li> Think about using Workspace.importXML method to avoid out-of-memory errors. This
 * might, however, give reference constraint violations when multiple nodes are imported.</li>
 * </ul>
 *
 */
public class JcrImportExportTool {

    // Some String constants
    private static final String MAPPING = "mapping.txt";
    private static final String NAMESPACES = "namespaces.xml";
    private static final String NODETYPES = "custom_nodetypes.xml";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ENCODING = "utf-8";

    /**
     * A static logger for this class.
     */
    private static Logger LOG = LoggerFactory.getLogger(JcrImportExportTool.class);

    /**
     * The repository.xml configuration file.
     */
    private final String repoConfig;

    /**
     * The home directory of the repository.
     */
    private final String repoHome;

    /**
     * The name of the file to import from or export to.
     */
    private final String fileName;

    /**
     * Constructs a JcrImportExportTool.
     *
     * @param config the repository configuration file
     * @param home the repository home directory
     * @param file the import/export file
     */
    public JcrImportExportTool(String config, String home, String file) {
        repoConfig = config;
        repoHome = home;
        fileName = file;
    }

    /**
     * Performs the import.
     *
     * @param nameSpaces whether namespace mappings should be imported
     * @param nodeTypes whether custom nodetypes should be imported
     */
    public void doImport(boolean nameSpaces, boolean nodeTypes) {

        Session jcrSession = null;

        try {
            Repository repository = new TransientRepository(repoConfig, repoHome);
            jcrSession = repository.login(new SimpleCredentials(USER, PASSWORD.toCharArray()));
            Workspace workspace = jcrSession.getWorkspace();

            UncloseableInputStream in = new UncloseableInputStream(new ZipInputStream(new FileInputStream(new File(fileName))));
            Map entryToPath = readMapping(in);

            // Assumes an order on the ZipEntries!
            ZipEntry entry = registerNamespaces(workspace, in, nameSpaces);
            entry = registerNodetypes(workspace, in, entry, nodeTypes);
            importContent(jcrSession, in, entry, entryToPath);

            in.myClose();

        } catch (Exception e) {
            // import failed: rollback the session!
            LOG.error("import failed: rollback", e);
            try {
                jcrSession.refresh(false);
            } catch (RepositoryException ee) {
                LOG.error("rollback failed", ee);
            }
        } finally {
            if (jcrSession != null) {
                try {
                    LOG.info("saving session");
                    jcrSession.save();
                } catch (RepositoryException e) {
                    LOG.error("save failed", e);
                }
                jcrSession.logout();
            }
        }
    }

    /**
     * Reads the mapping.txt entry.
     *
     * @param in the InputStream of the import file
     * @throws IOException on IO error
     */
    private Map readMapping(UncloseableInputStream in) throws IOException {

        Map entryToPath = new HashMap();
        ZipEntry zipEntry = in.getNextEntry();

        // check whether the entry is the mapping entry
        if (zipEntry != null && MAPPING.equals(zipEntry.getName())) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, ENCODING));

            // Read all lines of the entry
            String entry = reader.readLine();
            while (entry != null) {
                String[] tokens = entry.split("\t");
                if (tokens.length != 2) {
                    throw new IOException(MAPPING + " in invalid format");
                } else {
                    entryToPath.put(tokens[0], tokens[1]);
                }
                entry = reader.readLine();
            }
        } else {
            throw new IOException(MAPPING + " could not be found in file");
        }
        return entryToPath;
    }

    /**
     * Registers the namespaces, if nameSpaces==true.
     *
     * @param jcrSession a JCR session
     * @param in the InputStream of the import file
     * @param nameSpaces whether the namespaces should be imported
     * @throws RepositoryException on JCR error
     * @throws IOException on IO error
     * @return the ZipEntry of the entry after the namespace entry
     */
    private ZipEntry registerNamespaces(Workspace workspace, UncloseableInputStream in, boolean nameSpaces) throws RepositoryException, IOException {

        NamespaceRegistry namespaceReg = workspace.getNamespaceRegistry();
        ZipEntry zipEntry = in.getNextEntry();

        // Create a set of reserved prefixes:
        Set reservedPrefixes = new HashSet();
        reservedPrefixes.add("xml");
        reservedPrefixes.add("mix");
        reservedPrefixes.add("nt");
        reservedPrefixes.add("fn");
        reservedPrefixes.add("xs");
        reservedPrefixes.add("sv");
        reservedPrefixes.add("rep");
        reservedPrefixes.add("jcr");
        reservedPrefixes.add("");

        // check whether the entry is the namespaces entry
        if (zipEntry != null && NAMESPACES.equals(zipEntry.getName())) {

            // only import the namespaces if requested
            if (nameSpaces) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, ENCODING));

                // Read all lines of the entry:
                String entry = reader.readLine();
                while (entry != null) {

                    String[] tokens = entry.split("\t");
                    if (entry.compareTo("\t")==0) {
                        tokens = new String[]{"", ""};
                    }
                    if (tokens.length != 2) {
                        throw new IOException(NAMESPACES + " in invalid format");
                    } else {
                        try {
                            if (!reservedPrefixes.contains(tokens[0])) {
                                namespaceReg.registerNamespace(tokens[0], tokens[1]);
                                LOG.info("registering namespace: "+tokens[0]+" -> "+tokens[1]);
                            }
                        } catch (NamespaceException e) {
                            throw new IOException("could not register namespace mapping "+tokens[0]+" -> "+tokens[1]);
                        }
                    }
                    entry = reader.readLine();
                }
            }
            // get the next ZipEntry (because this one is the namespaces entry)
            zipEntry = in.getNextEntry();

        // the entry is not the namespaces entry
        } else if (nameSpaces){
            throw new IOException(NAMESPACES+" entry not found");
        }
        return zipEntry;
    }

    /**
     * Registers the custom nodetypes, if nodeTypes==true.
     *
     * @param jcrSession a JCR session
     * @param in the InputStream of the import file
     * @param zipEntry the current zipEntry
     * @param nodeTypes whether the nodetypes should be imported
     * @throws IOException on IO error
     * @throws RepositoryException on JCR error
     * @throws InvalidNodeTypeDefException on a JCR error caused by a wrong nodetype definition
     * @return the entry after the nodetype entry
     */
    private ZipEntry registerNodetypes(Workspace workspace, UncloseableInputStream in, ZipEntry zipEntry, boolean nodeTypes) throws IOException, RepositoryException, InvalidNodeTypeDefException {

        // check whether the current entry is the nodetypes entry
        if (zipEntry != null && NODETYPES.equals(zipEntry.getName())) {

            // only register the nodetypes if required
            if (nodeTypes) {
                NodeTypeManagerImpl ntm = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
                NodeTypeRegistry ntr = ntm.getNodeTypeRegistry();

                NodeTypeDef[] defs = NodeTypeReader.read(in);
                Set coll = new HashSet();
                for (int i=0; i<defs.length; i++ ) {
                    coll.add(defs[i]);
                    LOG.info("marked nodetype \""+defs[i].getName().toString()+"\" for registration");
                }

                LOG.info("registering "+coll.size()+" nodetypes");
                ntr.registerNodeTypes(coll);
            }

            // get the next ZipEntry (because this one is the nodetypes entry
            zipEntry = in.getNextEntry();

        } else if (nodeTypes){
            throw new IOException(NODETYPES+" entry not found");
        }
        return zipEntry;
    }

    /**
     * Imports the content entries: deletes the existing content!
     *
     * @param jcrSession a JCR session
     * @param in the InputStream of the import file
     * @param zipEntry the first content entry
     * @param entryToPath the mapping of entry names to JCR paths
     * @throws IOException on IO error
     * @throws RepositoryException on JCR error
     */
    private void importContent(Session jcrSession, UncloseableInputStream in, ZipEntry zipEntry, Map entryToPath) throws IOException, RepositoryException {

        while (zipEntry != null) {

            String entryName = zipEntry.getName();
            String absPath = (String) entryToPath.get(entryName);

            if (absPath == null) {
                throw new IOException("absolute path for entry \""+entryName+"\" not found");
            }

            String targetPath = null;

            try {
                // remove the item at absPath
                Item item = jcrSession.getItem(absPath);
                targetPath = item.getParent().getPath();
                LOG.info("removing item at path: \""+absPath+"\"");
                item.remove();
            } catch (PathNotFoundException e) {
                int lastSlashIndex = absPath.indexOf("/");
                while (absPath.indexOf("/", lastSlashIndex+1) != -1) {
                    lastSlashIndex = absPath.indexOf("/", lastSlashIndex+1);
                }
                targetPath = absPath.substring(0, lastSlashIndex+1);
            }

            // TODO: think about using Workspace.importXML method to prevent
            // out-of-memory exceptions. This might, however, trigger reference
            // constraint violations if multiple trees are imported
            LOG.info("importing content of "+entryName+": \""+absPath+"\"");
            jcrSession.importXML(targetPath, in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

            zipEntry = in.getNextEntry();
        }
    }

    /**
     * Performs the export.
     *
     * @param paths an array og absolute paths of nodes to export
     * @param nameSpaces whether namespace mappings should be exported
     * @param nodeTypes whether custom nodetypes should be exported
     */
    public void doExport(String[] paths, boolean nameSpaces, boolean nodeTypes) {

        Session jcrSession = null;

        try {
            // get a JCR session:
            Repository repository = new TransientRepository(repoConfig, repoHome);
            jcrSession = repository.login(new SimpleCredentials(USER, PASSWORD.toCharArray()));

            File result = new File(fileName);
            ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(result));

            createMappingEntry(zipout, paths);
            if (nameSpaces) {
                createNamespaceEntry(jcrSession, zipout);
            }
            if (nodeTypes) {
                createNodeTypeEntry(jcrSession, zipout);
            }
            createContentEntries(jcrSession, zipout, paths);

            zipout.close();

        } catch (Exception e) {
            LOG.error("export failed:", e);
        } finally {
            if (jcrSession != null) {
                jcrSession.logout();
            }
        }
    }

    /**
     * Creates the mapping zip entry.
     *
     * @param zipout zipout the OutputStream to write to
     * @throws IOException on IO error
     */
    private void createMappingEntry(ZipOutputStream zipout, String paths[]) throws IOException {

        ZipEntry ze = new ZipEntry(MAPPING);
        zipout.putNextEntry(ze);
        Writer out = new OutputStreamWriter(zipout, ENCODING);
        for (int i=0; i<paths.length; i++) {
            out.write(i+".xml\t"+paths[i]+"\n");
        }
        out.flush();
        zipout.closeEntry();
    }

    /**
     * Creates the namespaces zip entry.
     *
     * @param jcrSession a JCR session
     * @param zipout the OutputStream to write to
     * @throws IOException on IO error
     * @throws RepositoryException on JCR error
     */
    private void createNamespaceEntry(Session jcrSession, ZipOutputStream zipout) throws IOException, RepositoryException {

        ZipEntry ze = new ZipEntry(NAMESPACES);
        Writer out = new OutputStreamWriter(zipout, ENCODING);
        NamespaceRegistry namespaceReg = jcrSession.getWorkspace().getNamespaceRegistry();

        zipout.putNextEntry(ze);
        String[] uris = namespaceReg.getURIs();
        for (int i=0; i<uris.length; i++) {
            String prefix = namespaceReg.getPrefix(uris[i]);
            out.write(prefix+"\t"+uris[i]+"\n");
            LOG.info("exported namespace: "+prefix+" -> "+uris[i]);
        }
        out.flush();
        zipout.closeEntry();
    }

    /**
     * Creates the nodetype zip entry.
     *
     * @param jcrSession a JCR session
     * @param zipout the OutputStream to write to
     * @throws IOException on IO error
     * @throws RepositoryException on JCR error
     */
    private void createNodeTypeEntry(Session jcrSession, ZipOutputStream zipout) throws IOException, RepositoryException {

        NamespaceRegistry namespaceReg = jcrSession.getWorkspace().getNamespaceRegistry();
        ZipEntry ze = new ZipEntry(NODETYPES);

        zipout.putNextEntry(ze);
        NodeTypeManagerImpl ntm = (NodeTypeManagerImpl) jcrSession.getWorkspace().getNodeTypeManager();
        NodeTypeRegistry ntr = ntm.getNodeTypeRegistry();
        Set custom = new HashSet();
        QName[] names = ntr.getRegisteredNodeTypes();
        for (int i=0; i<names.length; i++) {
            if (!ntr.isBuiltIn(names[i])) {
                custom.add(ntr.getNodeTypeDef(names[i]));
                LOG.info("exported nodetype \""+names[i]+"\"");
            }
        }
        NodeTypeDef[] defs = (NodeTypeDef[]) custom.toArray(new NodeTypeDef[custom.size()]);
        NodeTypeWriter.write(zipout, defs, namespaceReg);
        zipout.closeEntry();
    }

    /**
     * Creates the content entries.
     *
     * @param jcrSession a JCR session
     * @param zipout the OutputStream to write to
     * @throws IOException on IO error
     * @throws RepositoryException on JCR error
     */
    private void createContentEntries(Session jcrSession, ZipOutputStream zipout, String paths[]) throws IOException, RepositoryException {

        // Export the specified nodes:
        for (int i=0; i<paths.length; i++) {
            ZipEntry ze = new ZipEntry(i+".xml");
            zipout.putNextEntry(ze);
            LOG.info("exporting content at path: \""+paths[i]+"\"");
            jcrSession.exportSystemView(paths[i], zipout, false, false);
            zipout.closeEntry();
        }
    }

    /**
     * Starts the JCR Import/Export tool.
     *
     * @param args the set of arguments to the Jcr Import tool
     */
    public static void main(String[] args) {

        // these are constructed from the argument list:
        boolean export = false;

        boolean nodeTypes = false;
        boolean nameSpaces = false;

        String repoconfig = null;
        boolean configSet = false;

        String repodir = null;
        boolean dirSet = false;

        String filename = null;
        boolean fileSet = false;

        String[] paths = {"/"};

        // try to parse the argument list:
        for (int i=0; i<args.length; i++) {
            if ("-c".equals(args[i]) && i+1 < args.length) {
                repoconfig = args[i+1];
                i++;
                configSet = true;
            } else if ("-d".equals(args[i]) && i+1 < args.length) {
                repodir = args[i+1];
                i++;
                dirSet = true;
            } else if ("-nt".equals(args[i])) {
                nodeTypes = true;
            } else if ("-ns".equals(args[i])) {
                nameSpaces = true;
            } else if ("-i".equals(args[i]) && i+1 < args.length) {
                filename = args[i+1];
                i++;
                export = false;
                fileSet = true;

            } else if ("-e".equals(args[i]) && i+1 < args.length) {
                export = true;
                fileSet = true;
                filename = args[i+1];
                if (i+2 < args.length) {
                    paths = new String[args.length-(i+2)];
                    for (int j=i+2; j<args.length; j++) {
                        paths[j-(i+2)] = args[j];
                    }
                }
            }
        }

        if (!configSet || !dirSet || !fileSet) {
            LOG.error("Invalid arguments.\nUsage:\n"
                    + "  java -jar JcrImportExportTool-<version>.jar -c <path to repository.xml> \\\n"
                    + "\t\t-d <path to repository home directory> [-nt] [-ns] {-i import_filename | -e export_filename [list of paths]}");
        } else {

            JcrImportExportTool jcrImport = new JcrImportExportTool(repoconfig, repodir, filename);
            if (export) {
                jcrImport.doExport(paths, nameSpaces, nodeTypes);
            } else {
                jcrImport.doImport(nameSpaces, nodeTypes);
            }
        }
    }

    /**
     * Wrapper for the ZipInputStream because the Session.importXML() method
     * of the JCR closes its argument InputStream before it returns.
     *
     */
    static class UncloseableInputStream extends InputStream {
        private final ZipInputStream zipInputStream;

        UncloseableInputStream(ZipInputStream in) {
            zipInputStream = in;
        }

        public int available() throws IOException {
            return zipInputStream.available();
        }

        private void myClose() throws IOException {
            zipInputStream.close();
        }

        public void close() throws IOException {
            // We don't close it!
            //zipInputStream.close();
        }

        public void closeEntry() throws IOException {
            zipInputStream.closeEntry();
        }

        public boolean equals(Object obj) {
            return zipInputStream.equals(obj);
        }

        public ZipEntry getNextEntry() throws IOException {
            return zipInputStream.getNextEntry();
        }

        public int hashCode() {
            return zipInputStream.hashCode();
        }

        public void mark(int readlimit) {
            zipInputStream.mark(readlimit);
        }

        public boolean markSupported() {
            return zipInputStream.markSupported();
        }

        public int read() throws IOException {
            return zipInputStream.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return zipInputStream.read(b, off, len);
        }

        public int read(byte[] b) throws IOException {
            return zipInputStream.read(b);
        }

        public void reset() throws IOException {
            zipInputStream.reset();
        }

        public long skip(long n) throws IOException {
            return zipInputStream.skip(n);
        }

        public String toString() {
            return zipInputStream.toString();
        }
    }
}
