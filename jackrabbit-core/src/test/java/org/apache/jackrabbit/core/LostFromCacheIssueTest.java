package org.apache.jackrabbit.core;

import java.io.File;
import java.io.InputStream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import junit.framework.TestCase;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class LostFromCacheIssueTest extends AbstractJCRTest {
    //public RepositoryImpl repository = null;
    //public Session session = null;
    public Property mixinTypes;

//    static private void delete(File path) {
//        if(path.exists()) {
//            if(path.isDirectory()) {
//                File[] files = path.listFiles();
//                for(int i=0; i<files.length; i++)
//                    delete(files[i]);
//            }
//            path.delete();
//        }
//    }
//
//    static private void clear() {
//        String[] files = new String[] { ".lock", "repository", "version", "workspaces" };
//        for(int i=0; i<files.length; i++) {
//            File file = new File(files[i]);
//            delete(file);
//        }
//    }

    public void setUp() throws Exception {
//        org.apache.jackrabbit.core.config.RepositoryConfig repoConfig = null;
//        InputStream config = getClass().getResourceAsStream("jackrabbit.xml");
//        String path = ".";
//
//        clear();

        super.setUp();

        //repoConfig = org.apache.jackrabbit.core.config.RepositoryConfig.create(config, path);
        //repository = org.apache.jackrabbit.core.RepositoryImpl.create(repoConfig);
        //Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Session session = superuser;

        System.err.println("Registering namespace and node types...");
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        NodeTypeManager ntmgr = workspace.getNodeTypeManager();
        NodeTypeRegistry nodetypeRegistry = ((NodeTypeManagerImpl)ntmgr).getNodeTypeRegistry();
        namespaceRegistry.registerNamespace("myprefix", "http://www.onehippo.org/test/1.0");
        QNodeTypeDefinition nodeTypeDefinition = new QNodeTypeDefinitionImpl(
                ((SessionImpl)session).getQName("test:mixin"),
                Name.EMPTY_ARRAY,
                Name.EMPTY_ARRAY,
                true,
                false,
                true,
                false,
                null,
                QPropertyDefinition.EMPTY_ARRAY,
                QNodeDefinition.EMPTY_ARRAY
                );
        nodetypeRegistry.registerNodeType(nodeTypeDefinition);
        nodeTypeDefinition = new QNodeTypeDefinitionImpl(
                ((SessionImpl)session).getQName("test:mxn"),
                Name.EMPTY_ARRAY,
                Name.EMPTY_ARRAY,
                true,
                false,
                true,
                false,
                null,
                QPropertyDefinition.EMPTY_ARRAY,
                QNodeDefinition.EMPTY_ARRAY
                );
        nodetypeRegistry.registerNodeType(nodeTypeDefinition);

        /*System.err.println("Initializing tree...");
        Node node = session.getRootNode();
        node = node.addNode("test", "nt:unstructured");
        buildTree(node, 2, 2, 10, 96, 0);
        session.save();*/
        session.getRootNode().addNode("test").addNode("node");
        session.save();

//        session.logout();
//        this.session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

//    public void tearDown() throws Exception {
//        if (session != null) {
//            session.logout();
//        }
//        if (repository != null) {
//            repository.shutdown();
//        }
//    }

    public void testIssue() throws Exception {
        String path = "/test/node";
        Session session = superuser;
        Node node = session.getRootNode().getNode(path.substring(1));
        node.addMixin("test:mxn");
        mixinTypes = node.getProperty("jcr:mixinTypes");
        session.save();
        node.addMixin("test:mixin");
        session.save();
        node.removeMixin("test:mxn");
        node.removeMixin("test:mixin");
        session.save();
        node.addMixin("test:mixin");
        session.save();
    }
}
