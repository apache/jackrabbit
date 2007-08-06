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
import java.util.List;

import org.apache.jackrabbit.ocm.annotation.Bean;
import org.apache.jackrabbit.ocm.annotation.Collection;
import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.annotation.Node;


/**
 * 
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 */
@Node
public class UnstructuredPerson {


    private String path;

    private List<UnstructuredAddress> addresses = new ArrayList<UnstructuredAddress>();
    
    private UnstructuredAddress anotherAdress; // Add here to test the Bean mapping

    @Collection(type = UnstructuredAddress.class)
    public List<UnstructuredAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<UnstructuredAddress> addresses) {
        this.addresses = addresses;
    }

    @Field(path = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void addAddress(UnstructuredAddress address) {
        addresses.add(address);
    }

    @Bean
	public UnstructuredAddress getAnotherAdress() {
		return anotherAdress;
	}

	public void setAnotherAdress(UnstructuredAddress anotherAdress) {
		this.anotherAdress = anotherAdress;
	}
    
    

}
