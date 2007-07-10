package org.apache.jackrabbit.ocm.annotation.model;

import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.annotation.Node;

/**
 * A simple model to test the annotation mapping
 * 
 * @author Philip Dodds
 */
@Node
public class Address {

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

	@Field(jcrName="ocm:street")
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Field(jcrName="ocm:city")
    public String getCity() {
        return city;
    }

    @Field(jcrName="ocm:type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Field(jcrName="ocm:state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
