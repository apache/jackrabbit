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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;

/**
 * No very useful class.
 * This is just there to test custom ManageableCollection implementation
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ArrayListElement implements ManageableCollection
{

	private ArrayList<Element> collection = new ArrayList<Element>();
     /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#addObject(java.lang.Object)
     */
    public void addObject(Object object)
    {
    	if (object instanceof Element)
        	collection.add((Element)object);
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#getIterator()
     */
    public Iterator<Element> getIterator()
    {
       return  collection.iterator();
    }

	public int getSize()
	{

		return collection.size();
	}

	public Object getObjects() {

		return collection;
	}


}
