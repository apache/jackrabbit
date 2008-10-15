package org.apache.jackrabbit.ocm.testmodel.enumeration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.EnumCollectionConverterImpl;
import org.apache.jackrabbit.ocm.manager.enumconverter.EnumTypeConverter;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node
public class Odyssey {
	@Field(path=true)
	private String path = null;
	@Field(converter=EnumTypeConverter.class)
	private Planet startingFrom;
	@Field(converter=EnumTypeConverter.class)
	private Planet goingTo;
	public Odyssey(){
		startingFrom = Planet.EARTH;
		goingTo = Planet.PLUTO;
		stops = new ArrayList<Planet>();
	}
	@org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection(collectionConverter=EnumCollectionConverterImpl.class)
	private java.util.List<Planet> stops;

	public void add(int index, Planet element) {
		stops.add(index, element);
	}

	public boolean add(Planet e) {
		return stops.add(e);
	}

	public boolean addAll(Collection<? extends Planet> c) {
		return stops.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends Planet> c) {
		return stops.addAll(index, c);
	}

	public void clear() {
		stops.clear();
	}

	public boolean contains(Object o) {
		return stops.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return stops.containsAll(c);
	}

	public boolean equals(Object o) {
		return stops.equals(o);
	}

	public Planet get(int index) {
		return stops.get(index);
	}

	public int hashCode() {
		return stops.hashCode();
	}

	public int indexOf(Object o) {
		return stops.indexOf(o);
	}

	public boolean isEmpty() {
		return stops.isEmpty();
	}

	public Iterator<Planet> iterator() {
		return stops.iterator();
	}

	public int lastIndexOf(Object o) {
		return stops.lastIndexOf(o);
	}

	public ListIterator<Planet> listIterator() {
		return stops.listIterator();
	}

	public ListIterator<Planet> listIterator(int index) {
		return stops.listIterator(index);
	}

	public Planet remove(int index) {
		return stops.remove(index);
	}

	public boolean remove(Object o) {
		return stops.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return stops.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return stops.retainAll(c);
	}

	public Planet set(int index, Planet element) {
		return stops.set(index, element);
	}

	public int size() {
		return stops.size();
	}

	public List<Planet> subList(int fromIndex, int toIndex) {
		return stops.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return stops.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return stops.toArray(a);
	}

	public java.util.List<Planet> getStops() {
		return stops;
	}

	public void setStops(java.util.List<Planet> stops) {
		this.stops = stops;
	}

	public Planet getStartingFrom() {
		return startingFrom;
	}

	public void setStartingFrom(Planet startingFrom) {
		this.startingFrom = startingFrom;
	}

	public Planet getGoingTo() {
		return goingTo;
	}

	public void setGoingTo(Planet goingTo) {
		this.goingTo = goingTo;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
