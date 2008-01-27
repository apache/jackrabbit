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
package org.apache.jackrabbit.ocm.testmodel.uuid;

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ReferenceBeanConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.BeanReferenceCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;


/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * @version $Id: Exp $
 */
@Node
public class B2
{
	@Field(path=true) private String path;

    // This attribute is mapped to a reference jcr property
	@Bean(converter=ReferenceBeanConverterImpl.class) private A a;

    //  a collection of bean mapped into a list of jcr properties (reference type)
    @Collection (collectionConverter=BeanReferenceCollectionConverterImpl.class)
    private java.util.Collection multiReferences;

    public String getPath()
    {
		return path;
	}
	
    public void setPath(String path)
	{
		this.path = path;
	}

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}
	public java.util.Collection getMultiReferences() {
		return multiReferences;
	}

	public void setMultiReferences(java.util.Collection multiReferences) {
		this.multiReferences = multiReferences;
	}

}
