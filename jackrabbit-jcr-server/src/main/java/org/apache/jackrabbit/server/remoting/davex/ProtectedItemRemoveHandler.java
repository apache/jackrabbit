package org.apache.jackrabbit.server.remoting.davex;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * ProtectedItemRemoveHandler... TODO
 */
public interface ProtectedItemRemoveHandler {

    public void init(Session session) throws RepositoryException;
    
    public boolean canHandle(String itemPath) throws RepositoryException;
    
    public void remove(String itemPath) throws RepositoryException;
}
