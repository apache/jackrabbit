package org.apache.jackrabbit.server.remoting.davex;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.Text;

/**
 * ProtectedItemRemoveHandlerImpl... TODO
 */
public class ProtectedItemRemoveHandlerImpl implements ProtectedItemRemoveHandler {

    private Session session;
    private AccessControlManager acMgr;
    private NamePathResolver npResolver;
    
    private static final String NT_REP_ACL = "rep:ACL";
    
    @Override
    public void init(Session session) throws RepositoryException {
        this.session = session;
        acMgr = session.getAccessControlManager();
        npResolver = new DefaultNamePathResolver(session);
    }

    @Override
    public boolean canHandle(String itemPath) throws RepositoryException {
        Item aclItem = session.getItem(itemPath);
        if (aclItem.isNode() && isAbsolute(itemPath)) {
            if (isJackrabbitAclNodeType((Node) aclItem)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void remove(String itemPath) throws RepositoryException {
        String controlledPath = Text.getRelativeParent(itemPath, 1);
        AccessControlPolicy[] policies = acMgr.getPolicies(controlledPath);
        for (AccessControlPolicy policy : policies) {
            acMgr.removePolicy(controlledPath, policy);
        }        
    }

    // ----------------------------------------< private >---
    private boolean isJackrabbitAclNodeType(Node aclNode) throws RepositoryException {
        String ntName = aclNode.getPrimaryNodeType().getName();
        return ntName.equals(NT_REP_ACL);
    }
    
    private boolean isAbsolute(String itemPath) throws RepositoryException {
        Path qPath = npResolver.getQPath(itemPath);
        return qPath.isAbsolute();
    }    
}
