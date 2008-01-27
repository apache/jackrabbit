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

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;


/**
 * Simple object used to test default value assignement
 */
@Node(jcrType="ocm:DefTestPrimary", discriminator=false)
public class Default
{
	@Field(path=true) private String path;

	@Field(jcrName="ocm:p1") private String p1;

	@Field(jcrName="ocm:p2") private String p2;

	@Field(jcrName="ocm:p3", jcrDefaultValue="p3DescriptorDefaultValue")
    private String p3;

	@Field(jcrName="ocm:p4") private String p4;

	@Field(jcrName="ocm:p5") private String p5;


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the p1
     */
    public String getP1() {
        return p1;
    }

    /**
     * @param p1 the p1 to set
     */
    public void setP1(String p1) {
        this.p1 = p1;
    }

    /**
     * @return the p2
     */
    public String getP2() {
        return p2;
    }

    /**
     * @param p2 the p2 to set
     */
    public void setP2(String p2) {
        this.p2 = p2;
    }

    /**
     * @return the p3
     */
    public String getP3() {
        return p3;
    }

    /**
     * @param p3 the p3 to set
     */
    public void setP3(String p3) {
        this.p3 = p3;
    }

    /**
     * @return the p4
     */
    public String getP4() {
        return p4;
    }

    /**
     * @param p4 the p4 to set
     */
    public void setP4(String p4) {
        this.p4 = p4;
    }

    /**
     * @return the p5
     */
    public String getP5() {
        return p5;
    }

    /**
     * @param p5 the p5 to set
     */
    public void setP5(String p5) {
        this.p5 = p5;
    }

}
