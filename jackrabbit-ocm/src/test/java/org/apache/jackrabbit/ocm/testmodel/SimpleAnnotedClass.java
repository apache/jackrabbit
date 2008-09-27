package org.apache.jackrabbit.ocm.testmodel;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;



@Node
public class SimpleAnnotedClass 
       extends SimpleAnnotedAbstractClass // The ancestor is also a mapped class
       implements 
       			  // The following interface is also mapped
       			  SimpleInterface, 
       			  // The following interface is not mapped.So, the ObjectContentManager will not manage it 
                  UnmappedInterface { 

	private int testInt;

	public int getTestInt() {
		return testInt;
	}

	public void setTestInt(int testInt) {
		this.testInt = testInt;
	} 
	
	
}
