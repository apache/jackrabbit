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

import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.annotation.Node;

/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * 
 */

@Node
public class B
{
     private String b1;
     private String b2;   
     
    @Field
    public String getB1()
    {
        return b1;
    }

    public void setB1(String b1)
    {
        this.b1 = b1;
    }

    @Field
    public String getB2()
    {
        return b2;
    }

    public void setB2(String b2)
    {
        this.b2 = b2;
    }
    
    
}
