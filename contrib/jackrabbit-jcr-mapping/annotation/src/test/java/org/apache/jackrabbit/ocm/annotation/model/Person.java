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
package org.apache.jackrabbit.ocm.annotation.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.ocm.annotation.Collection;
import org.apache.jackrabbit.ocm.annotation.Node;
import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;

/**
 * A simple model to test the annotation mapping
 * 
 * @author Philip Dodds
 */
@Node
public class Person {


    private String path;

    private List<Address> addresses = new ArrayList<Address>();

    @Collection(type = Address.class, converter= NTCollectionConverterImpl.class)
    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    @Field(path = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void addAddress(Address address) {
        addresses.add(address);
    }

}
