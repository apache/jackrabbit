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
package org.apache.jackrabbit.standalone.cli;

/**
 * Command line argument
 */
public class Argument extends Option {
    /** position of the argument */
    private int position;

    /**
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * sets the argument position
     * @param position
     *        the position
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * {@inheritDoc}
     * @return a clone
     */
    public Object clone() {
        Argument arg = new Argument();
        arg.position = this.position;
        this.clone(arg);
        return arg;
    }
}
