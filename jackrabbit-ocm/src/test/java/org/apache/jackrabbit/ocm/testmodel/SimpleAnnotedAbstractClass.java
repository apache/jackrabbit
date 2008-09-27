package org.apache.jackrabbit.ocm.testmodel;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;


@Node
public abstract class SimpleAnnotedAbstractClass {

	@Field(path=true) String path; 
	@Field String test;

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	} 
		
		
}
