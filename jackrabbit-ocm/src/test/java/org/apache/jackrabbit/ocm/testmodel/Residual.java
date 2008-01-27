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

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManagedHashMap;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ResidualNodesCollectionConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ResidualPropertiesCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 *
 * @author <a href="mailto:fmeschbe[at]apache[dot]com">Felix Meschberger</a>
 *
 * Note :
 * If the annotation are defined on the field declaration, it is mandatory to register the ancestor classes.
 * Otherwise, the annotation mapper will not map the fields defined in the ancester classes.
 *
 */
@Node
public class Residual
{
	@Field(path=true) private String path;

    private ManagedHashMap elements;

	/**
     * @return Returns the elements.
     */
    public ManagedHashMap getElements()
    {
        return elements;
    }
    /**
     * @param elements The elements to set.
     */
    public void setElements(ManagedHashMap elements)
    {
        this.elements = elements;
    }


    @Node(extend=Residual.class) public static class ResidualProperties extends Residual
    {
        @Collection( jcrName="value*",elementClassName=String.class,collectionConverter=ResidualPropertiesCollectionConverterImpl.class,
                collectionClassName=ManagedHashMap.class)
        private ManagedHashMap elements;


    }

    @Node(extend=Residual.class) public static class ResidualNodes extends Residual
    {
    	
        @Collection( jcrName="value*",elementClassName=Object.class,collectionConverter=ResidualNodesCollectionConverterImpl.class,
                collectionClassName=ManagedHashMap.class)
        private ManagedHashMap elements;

    	
    }

    protected Residual() {}


    public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
}
