package org.apache.jackrabbit.ocm.annotation.model.unstructured;

import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.annotation.Node;

/**
 * This class is mapped into a unstructured jcr node type
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a> 
 */

@Node
public class UnstructuredAddress {

    private String path; 
    
	private String type;

    private String street;

    private String city;

    private String state;
    
    
    @Field(path = true)
    public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Field
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Field
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    @Field(id=true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
