package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.InputStream;

/**
 * <code>WorkspaceImport</code>...
 */
public class WorkspaceImport extends AbstractOperation {

    private final NodeState nodeState;
    private final InputStream xmlStream;
    private final int uuidBehaviour;

    private WorkspaceImport(NodeState nodeState, InputStream xmlStream, int uuidBehaviour) {
        if (nodeState == null || xmlStream == null) {
            throw new IllegalArgumentException();
        }
        this.nodeState = nodeState;
        this.xmlStream = xmlStream;
        this.uuidBehaviour = uuidBehaviour;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that has been updated and all
     * its decendants.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        NodeEntry entry;
        if (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING ||
                uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            // invalidate the complete tree
            entry = nodeState.getNodeEntry();
            while (entry.getParent() != null) {
                entry = entry.getParent();
            }
            entry.invalidate(true);
        } else {
            // import only added new items below the import target. therefore
            // recursive invalidation is not required. // TODO correct?
            nodeState.getNodeEntry().invalidate(false);
        }
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() {
        return nodeState.getNodeId();
    }

    public InputStream getXmlStream() {
        return xmlStream;
    }

    public int getUuidBehaviour() {
        return uuidBehaviour;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param xmlStream
     * @return
     */
    public static Operation create(NodeState nodeState, InputStream xmlStream, int uuidBehaviour) {
        return new WorkspaceImport(nodeState, xmlStream, uuidBehaviour);
    }
}
