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
package org.apache.portals.graffito.jcr.testmodel;


/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * 
 */
public class PropertyTest
{
	private String path;
	private String requiredProp;
	private String requiredWithConstraintsProp;
	private String autoCreatedProp;
	private String autoCreatedWithConstraintsProp;
	private String mandatoryProp;
	private String mandatoryWithConstaintsProp; 
	private String protectedWithDefaultValueProp;
	
	
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getAutoCreatedProp() {
		return autoCreatedProp;
	}
	public void setAutoCreatedProp(String autoCreadetProp) {
		this.autoCreatedProp = autoCreadetProp;
	}
	public String getAutoCreatedWithConstraintsProp() {
		return autoCreatedWithConstraintsProp;
	}
	public void setAutoCreatedWithConstraintsProp(
			String autoCreatedWithConstraintsProp) {
		this.autoCreatedWithConstraintsProp = autoCreatedWithConstraintsProp;
	}
	public String getMandatoryProp() {
		return mandatoryProp;
	}
	public void setMandatoryProp(String mandatoryProp) {
		this.mandatoryProp = mandatoryProp;
	}
	public String getMandatoryWithConstaintsProp() {
		return mandatoryWithConstaintsProp;
	}
	public void setMandatoryWithConstaintsProp(String mandatoryWithConstaintsProp) {
		this.mandatoryWithConstaintsProp = mandatoryWithConstaintsProp;
	}
	public String getProtectedWithDefaultValueProp() {
		return protectedWithDefaultValueProp;
	}
	public void setProtectedWithDefaultValueProp(
			String protectedWithDefaultValueProp) {
		this.protectedWithDefaultValueProp = protectedWithDefaultValueProp;
	}
	public String getRequiredProp() {
		return requiredProp;
	}
	public void setRequiredProp(String requiredProp) {
		this.requiredProp = requiredProp;
	}
	public String getRequiredWithConstraintsProp() {
		return requiredWithConstraintsProp;
	}
	public void setRequiredWithConstraintsProp(String requiredWithConstraintsProp) {
		this.requiredWithConstraintsProp = requiredWithConstraintsProp;
	}
	
	
	
}
