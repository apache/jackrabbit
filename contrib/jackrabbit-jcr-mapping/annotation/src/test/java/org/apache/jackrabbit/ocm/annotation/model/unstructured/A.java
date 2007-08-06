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
package org.apache.jackrabbit.ocm.annotation.model.unstructured;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.jackrabbit.ocm.annotation.Bean;
import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.annotation.Node;

/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * 
 */
@Node
public class A
{
	
	private String path; 
    private String a1;
    private String a2;
    private B b;
    private B emptyB;
    private Collection collection;
    private Collection emptyCollection; 
    
    
    @Field(path=true)
    public String getPath() {
		return path;
	}
        
	public void setPath(String path) {
		this.path = path;
	}

	@Field
	public String getA1()
    {
        return a1;
    }
	
    
    public void setA1(String a1)
    {
        this.a1 = a1;
    }

    @Field
    public String getA2()
    {
        return a2;
    }

    public void setA2(String a2)
    {
        this.a2 = a2;
    }
    
    @Bean
    public B getB()
    {
        return b;
    }

    public void setB(B b)
    {
        this.b = b;
    }
    
        
    @org.apache.jackrabbit.ocm.annotation.Collection(type=C.class)
    public Collection getCollection()
    {
        return collection;
    }
    
    /**
     * @param collection The collection to set.
     */
    public void setCollection(Collection collection)
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
    public Collection getEmptyCollection()
    {
        return emptyCollection;
    }
    public void setEmptyCollection(Collection emptyCollection)
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
