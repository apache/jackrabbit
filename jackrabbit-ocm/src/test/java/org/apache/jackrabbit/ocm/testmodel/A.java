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


import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @version $Id: Exp $
 */
@Node(jcrMixinTypes="mix:lockable" )
public class A
{
	@Field(path=true) private String path; 
    @Field private String a1;
    @Field private String a2;
    @Bean private B b;
    
    private B emptyB;

    @Collection(elementClassName=C.class, jcrType="ocm:C") private java.util.Collection collection;	
    @Collection(elementClassName=C.class) private java.util.Collection emptyCollection; 
    
    public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	/**
     * @return Returns the a1.
     */
    public String getA1()
    {
        return a1;
    }
    /**
     * @param a1 The a1 to set.
     */
    public void setA1(String a1)
    {
        this.a1 = a1;
    }
    /**
     * @return Returns the a2.
     */
    public String getA2()
    {
        return a2;
    }
    /**
     * @param a2 The a2 to set.
     */
    public void setA2(String a2)
    {
        this.a2 = a2;
    }
    /**
     * @return Returns the b.
     */
    public B getB()
    {
        return b;
    }
    /**
     * @param b The b to set.
     */
    public void setB(B b)
    {
        this.b = b;
    }
    
        
    /**
     * @return Returns the collection.
     */
    public java.util.Collection getCollection()
    {
        return collection;
    }
    
    /**
     * @param collection The collection to set.
     */
    public void setCollection(java.util.Collection collection)
    {
        this.collection = collection;
    }
    
    public void addC(C c)
    {
       if (collection == null )
       {
           collection = new ArrayList();
       }
       
       collection.add(c);   
    }
    public java.util.Collection getEmptyCollection()
    {
        return emptyCollection;
    }
    public void setEmptyCollection(java.util.Collection emptyCollection)
    {
        this.emptyCollection = emptyCollection;
    }
    public B getEmptyB()
    {
        return emptyB;
    }
    public void setEmptyB(B emptyB)
    {
        this.emptyB = emptyB;
    }
    
    
}
