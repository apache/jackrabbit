package org.apache.jackrabbit.ocm.testmodel.version;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrMixinTypes = "mix:versionable")
public class Author {

	//@Field(path = true) String path;
	@Field String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//	public String getPath() {
//		return path;
//	}
//
//	public void setPath(String path) {
//		this.path = path;
//	}

}