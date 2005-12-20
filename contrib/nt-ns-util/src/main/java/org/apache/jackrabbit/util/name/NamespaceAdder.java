package org.apache.jackrabbit.util.name;

import javax.jcr.NamespaceRegistry;
import javax.jcr.NamespaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Iterator;

public class NamespaceAdder {
    NamespaceRegistry registry;

    public NamespaceAdder(NamespaceRegistry nsr) {
        registry = nsr;
    }

    public void addNamespaces(NamespaceMapping nsm)
            throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        Map m = nsm.getPrefixToURIMapping();
        for(Iterator i = m.values().iterator(); i.hasNext();){
            Map.Entry e = (Map.Entry)i.next();
            String prefix = (String)e.getKey();
            String uri =(String)e.getKey();
            registry.registerNamespace(prefix, uri);
        }
    }

    public void addNamespace(String prefix, String uri)
        throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        registry.registerNamespace(prefix, uri);
    }
}
