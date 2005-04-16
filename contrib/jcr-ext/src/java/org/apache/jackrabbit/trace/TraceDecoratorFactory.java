/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.trace;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.decorator.DecoratorFactory;
import org.apache.jackrabbit.decorator.SimpleDecoratorFactory;

/**
 * TODO
 */
public class TraceDecoratorFactory extends SimpleDecoratorFactory implements
        DecoratorFactory {

    private LogFactory logFactory;
    
    private TraceFingerprint fingerprint;
    
    private Map loggers;
    
    public TraceDecoratorFactory(LogFactory logFactory) {
        this.logFactory = logFactory;
        this.fingerprint = new TraceFingerprint();
        this.loggers = new HashMap();
    }
    
    private synchronized TraceLogger getLogger(Class klass) {
        TraceLogger logger = (TraceLogger) loggers.get(klass);
        if (logger == null) {
            Log log = logFactory.getInstance(klass); 
            logger = new TraceLogger(klass.getName(), fingerprint, log);
            loggers.put(klass, logger);
        }
        return logger;
    }
    
    public Repository getRepositoryDecorator(Repository repository) {
        TraceLogger logger = getLogger(Repository.class);
        return new TraceRepositoryDecorator(this, repository, logger);
    }
    
    public Session getSessionDecorator(Repository repository, Session session) {
        // TODO Auto-generated method stub
        return super.getSessionDecorator(repository, session);
    }
    
    public Workspace getWorkspaceDecorator(
            Session session, Workspace workspace) {
        TraceLogger logger = getLogger(Workspace.class);
        return new TraceWorkspaceDecorator(this, session, workspace, logger);
    }

    public Item getItemDecorator(Session session, Item item) {
        // TODO Auto-generated method stub
        return super.getItemDecorator(session, item);
    }
    public Node getNodeDecorator(Session session, Node node) {
        // TODO Auto-generated method stub
        return super.getNodeDecorator(session, node);
    }
    public Property getPropertyDecorator(Session session, Property property) {
        // TODO Auto-generated method stub
        return super.getPropertyDecorator(session, property);
    }
}
