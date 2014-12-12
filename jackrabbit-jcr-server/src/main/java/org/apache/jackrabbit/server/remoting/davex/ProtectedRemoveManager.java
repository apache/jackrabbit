package org.apache.jackrabbit.server.remoting.davex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

public class ProtectedRemoveManager {

    private static List<ProtectedItemRemoveHandler> handlers = new ArrayList<ProtectedItemRemoveHandler>();
    
    public static void remove(String itemPath) throws RepositoryException {         
        for (ProtectedItemRemoveHandler handler : handlers) {
            if (handler.canHandle(itemPath)) {
                handler.remove(itemPath);
                return;
            }
        }
        throw new UnsupportedRepositoryOperationException("Cannot remove protected item -> " +
        		"There is no proper configured handler that can remove the item!");
    }

    /**
     * Load the ProtectedItemRemoveHandler configuration into this manager.
     * @param inStream
     * @throws RepositoryException      if an IOException occurs.
     */
    public static void load(Session session, InputStream inStream) throws RepositoryException {
        Properties props = new Properties();
        try {
            props.load(inStream);
            fillList(session, props);
        } catch (IOException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    /**
     *  Iterate through the loaded configuration and populate the list with concrete handlers.
     * @param props
     * @throws RepositoryException
     */
    private static void fillList(Session session, Properties props) throws RepositoryException {
        for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
            String key = en.nextElement().toString();
            String className = props.getProperty(key);
            if (!className.isEmpty()) {
                ProtectedItemRemoveHandler irHandler = createHandler(className);
                if (irHandler != null) {
                    irHandler.init(session);
                    handlers.add(irHandler);
                }
            }
        }        
    }

    /**
     * Instantiates and returns a concrete ProtectedItemRemoveHandler implementation.
     * @param className
     * @return
     * @throws RepositoryException
     */
    private static ProtectedItemRemoveHandler createHandler(String className) throws RepositoryException {
        try {
            Class<?> irHandlerClass = Class.forName(className);
            if (ProtectedItemRemoveHandler.class.isAssignableFrom(irHandlerClass)) {
                ProtectedItemRemoveHandler irHandler = (ProtectedItemRemoveHandler) irHandlerClass.newInstance();
                return irHandler;
            }
            return null;
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage());
        }
    }
}
