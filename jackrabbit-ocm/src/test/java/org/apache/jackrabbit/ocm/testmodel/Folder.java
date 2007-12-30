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
package org.apache.jackrabbit.ocm.testmodel;

import java.util.ArrayList;


import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
/**
 * Java class used to map the jcr node type nt:folder
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
@Node(jcrType="nt:folder", extend=HierarchyNode.class )
public class Folder extends HierarchyNode
{
    @Collection(autoUpdate=false, elementClassName=HierarchyNode.class , collectionConverter=NTCollectionConverterImpl.class)
    private java.util.Collection children; 
   

	public java.util.Collection getChildren() 
	{
		return children;
	}

	public void setChildren(java.util.Collection children) 
	{
		this.children = children;
	}

	public void addChild(HierarchyNode node)
	{
		if (children == null)
		{
			children = new ArrayList();			
		}
		children.add(node);
	}
    
}
