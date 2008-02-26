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
package org.apache.jackrabbit.test.config;

import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.config.JournalConfig;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;

public class ClusterConf {

    private String id;

    private long syncDelay;

    private JournalConf jc;

	public ClusterConf(String id, long syncDelay, JournalConf jc) {
		this.id = id;
		this.syncDelay = syncDelay;
		this.jc = jc;
	}

	public ClusterConfig createClusterConfig(Variables variables) throws ConfException {
		JournalConfig jc = null;
		if (getJournalConf() != null) {
			jc = getJournalConf().createJournalConfig(variables);
		}
		return new ClusterConfig(getId(), getSyncDelay(), jc);
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSyncDelay() {
        return syncDelay;
    }

    public void setSyncDelay(long syncDelay) {
        this.syncDelay = syncDelay;
    }

    public JournalConf getJournalConf() {
        return jc;
    }
    
    public void setJournalConf(JournalConf jc) {
        this.jc = jc;
    }

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[Cluster");
		
		pp.increaseIndent();
		
		pp.printlnIndent("id=" + id);
		pp.printlnIndent("syncDelay=" + syncDelay);
		
		if (jc != null) jc.print(pp);

		pp.decreaseIndent();
		
		pp.printlnIndent("]");
	}
}
