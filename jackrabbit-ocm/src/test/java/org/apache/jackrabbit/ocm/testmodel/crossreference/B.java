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
package org.apache.jackrabbit.ocm.testmodel.crossreference;

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ReferenceBeanConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */

@Node
public class B
{
     @Field private String b1;
     @Field private String b2;
     @Bean(converter=ReferenceBeanConverterImpl.class)private A a;


    /**
     * @return Returns the b1.
     */
    public String getB1()
    {
        return b1;
    }
    /**
     * @param b1 The b1 to set.
     */
    public void setB1(String b1)
    {
        this.b1 = b1;
    }
    /**
     * @return Returns the b2.
     */
    public String getB2()
    {
        return b2;
    }
    /**
     * @param b2 The b2 to set.
     */
    public void setB2(String b2)
    {
        this.b2 = b2;
    }
	public A getA() {
		return a;
	}
	public void setA(A a) {
		this.a = a;
	}


}
