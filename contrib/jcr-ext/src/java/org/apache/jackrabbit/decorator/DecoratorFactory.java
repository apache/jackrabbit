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
package org.apache.jackrabbit.decorator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * Factory interface for creating decorator instances.
 * 
 * @author Jukka Zitting
 */
public interface DecoratorFactory {

    /**
     * Creates a {@link Repository Repository} decorator.
     *  
     * @param repository the underlying repository instance
     * @return decorator for the given repository
     */
    public Repository getRepositoryDecorator(Repository repository);
    
    /**
     * Creates a {@link Session Session} decorator. 
     *  
     * @param repository the repository decorator 
     * @param session the underlying session instance
     * @return decorator for the given session
     */
    public Session getSessionDecorator(Repository repository, Session session);
    
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace);
    
    public Node getNodeDecorator(Session session, Node node);
    
    public Property getPropertyDecorator(Session session, Property property);
    
    public Item getItemDecorator(Session session, Item item);
    
}
