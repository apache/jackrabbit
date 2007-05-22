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
package org.apache.jackrabbit.jcrlog.player;

import org.apache.jackrabbit.jcrlog.StringUtils;

/**
 * A function call argument used by the statement.
 *
 * @author Thomas Mueller
 *
 */
class Arg {
    private Player player;
    private Class clazz;
    private Object obj;
    private Statement stat;

    Arg(Player player, Class clazz, Object obj) {
        this.player = player;
        this.clazz = clazz;
        this.obj = obj;
    }

    Arg(Statement stat) {
        this.stat = stat;
    }

    public String toString() {
        if (stat != null) {
            return stat.toString();
        } else {
            return StringUtils.quote(clazz, getValue());
        }
    }

    void execute() throws Exception {
        if (stat != null) {
            stat.execute();
            clazz = stat.getReturnClass();
            obj = stat.getReturnObject();
            stat = null;
        }
    }

    Class getValueClass() {
        return clazz;
    }

    Object getValue() {
        if (obj == null) {
            return null;
        } else if (clazz == String.class) {
            return player.getUUID((String)obj);
        } else if (clazz == String[].class) {
            String[] now = (String[])obj;
            String[] result = new String[now.length];
            for (int i = 0; i < now.length; i++) {
                result[i] = player.getUUID(now[i]);
            }
            return result;
        } else {
            return obj;
        }
    }
}