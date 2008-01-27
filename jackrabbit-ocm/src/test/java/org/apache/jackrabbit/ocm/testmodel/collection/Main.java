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
package org.apache.jackrabbit.ocm.testmodel.collection;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;


/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * @version $Id: Exp $
 */
@Node
public class Main
{
	@Field(path=true) private String path;
    @Field private String text;
    @Collection (elementClassName=Element.class, collectionClassName=HashMapElement.class)
    private HashMapElement hashMap;

    @Collection (elementClassName=Element.class, collectionClassName=ArrayListElement.class)
    private ArrayListElement list;

    public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	/**
     * @return Returns the elements.
     */
    public HashMapElement getHashMap()
    {
        return hashMap;
    }
    /**
     * @param elements The elements to set.
     */
    public void setHashMap(HashMapElement hashMap)
    {
        this.hashMap = hashMap;
    }

    public ArrayListElement getList()
    {
		return list;
	}
	
    public void setList(ArrayListElement list)
    {
		this.list = list;
	}
	/**
     * @return Returns the text.
     */
    public String getText()
    {
        return text;
    }
    /**
     * @param text The text to set.
     */
    public void setText(String text)
    {
        this.text = text;
    }


}
