package org.apache.jackrabbit.core;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class LostFromCacheIssueTest extends AbstractJCRTest {

    public Property mixinTypes;


    public void setUp() throws Exception {

        super.setUp();
        Session session = superuser;

        System.err.println("Registering namespace and node types...");
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        NodeTypeManager ntmgr = workspace.getNodeTypeManager();
        NodeTypeRegistry nodetypeRegistry = ((NodeTypeManagerImpl)ntmgr).getNodeTypeRegistry();
        try {
            namespaceRegistry.registerNamespace("lfcit", "data:lfcit");
        } catch (NamespaceException ignore) { /* mapping may already be present */  }
        QNodeTypeDefinition nodeTypeDefinition = new QNodeTypeDefinitionImpl(
                ((SessionImpl)session).getQName("lfcit:mixin"),
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
                ((SessionImpl)session).getQName("lfcit:mxn"),
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

        session.getRootNode().addNode("test").addNode("node");
        session.save();
    }

    public void testIssue() throws Exception {
        String path = "/test/node";
        Session session = superuser;
        Node node = session.getRootNode().getNode(path.substring(1));
        node.addMixin("lfcit:mxn");
        mixinTypes = node.getProperty("jcr:mixinTypes");
        session.save();
        node.addMixin("lfcit:mixin");
        session.save();
        node.removeMixin("lfcit:mxn");
        node.removeMixin("lfcit:mixin");
        session.save();
        node.addMixin("lfcit:mixin");
        session.save();
    }
}
