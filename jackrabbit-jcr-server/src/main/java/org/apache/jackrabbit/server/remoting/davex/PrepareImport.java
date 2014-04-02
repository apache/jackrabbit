package org.apache.jackrabbit.server.remoting.davex;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.TextValue;
import org.apache.jackrabbit.server.remoting.davex.JsonDiffHandler.ImportItem;
import org.apache.jackrabbit.server.remoting.davex.JsonDiffHandler.ImportMvProp;
import org.apache.jackrabbit.server.remoting.davex.JsonDiffHandler.ImportNode;
import org.apache.jackrabbit.server.remoting.davex.JsonDiffHandler.ImportProp;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.value.ValueHelper;

class PrepareImport {

	private NameResolver resolver;
	private Session session;
	private JsopImporter importer;
	private ValueFactory vf;
	
	// variables for holding information about the current property being
	// prepared for import.

	// Store serialized Value objects.
	private List<StringValue> currentPropertyValues = new ArrayList<StringValue>();
	private List<Name> mixinNames = new ArrayList<Name>();
	private ImportItem item;
	// private Map<NodeInfo,List<PropInfo>> map = new
	// HashMap<NodeInfo,List<PropInfo>>();

	private Stack<ImportState> states = new Stack<ImportState>();

	public PrepareImport(Session session, Node parent, ValueFactory valueFactory) {
		this.session = session;
		vf = valueFactory;
		resolver = new DefaultNamePathResolver(this.session);
		importer = new ImporterFactory().createImporter(parent, session);
	}

	void prepareNode(ImportItem item) throws IOException {
		// 1. get node and its properties.
		// 2. prepare a NodeInfo and propInfo object and push onto stack.
		// 3. recursively do the same if there are any child nodes.

		Name nodeName = null;
		Name ntName = null;
		ImportState state = null;
		if (item instanceof ImportNode) {
			nodeName = nameFromString(item.getName(), "Node Name");
			ntName = nameFromString(((ImportNode) item).getNodeTypeName(),
					"Node type name");
		}

		state = new ImportState(nodeName);
		state.setNodeType(ntName);

		// prepare properties
		for (ImportItem prop : ((ImportNode) item).getChildProps()) {
			if (prop instanceof ImportMvProp) {
				if (((ImportMvProp) prop).getName().equals(
						JcrConstants.JCR_MIXINTYPES)) {
					addMixins(state, (ImportMvProp) prop);
				} else {
					PropInfo propInfo = createPropInfo((ImportMvProp) prop);
					state.addPropInfo(propInfo);
				}
			} else {
				PropInfo propInfo = createPropInfo((ImportProp) prop);
				state.addPropInfo(propInfo);
			}
		}
		processNode(state,true, false);
		state.setFlag(true);
		states.push(state);
		
		// Recursively prepare child nodes
		for (ImportNode in : ((ImportNode)item).getChildNodes()){
			prepareNode(in);
		}
		
		// End all nodes for which the import has been started.
		if(!states.empty()){
			ImportState current = states.peek();
			if(current.started()){
				processNode(current,false,true);
			}
		}
	}

	// Import the current node
	private void processNode(ImportState state, boolean start, boolean end)
			throws IOException {
		if (!start && !end) {
			return;
		}
		Name[] mixinNames = null;
		if (state.getMixinNames() != null) {
			mixinNames = state.getMixinNames().toArray(
					new Name[state.getMixinNames().size()]);
		}

		NodeId id = null;
		if (state.getUUID() != null) {
			id = NodeId.valueOf(state.uuid);
		}
		if (state.getPropInfos() == null) {
			state.props = new ArrayList<PropInfo>();
		}
		
		NodeInfo nodeInfo = new NodeInfo(state.nodeName, state.ntName,
				mixinNames, id);
		
		// call Importer
		try {
			if (start) {
				importer.startNode(nodeInfo, state.getPropInfos());
				// dispose temporary property values
				for (PropInfo pi : state.props) {
					pi.dispose();
				}

			}
			if (end) {
				importer.endNode(nodeInfo);
			}
		} catch (RepositoryException reCause) {
			throw new DiffException(
					"Repository exception while importing nodeName {} with node type {} "
							+ nodeInfo.getName()
							+ nodeInfo.getNodeTypeName(), reCause);
		}
	}


	private PropInfo createPropInfo(ImportMvProp prop) throws IOException{
		List<StringValue> values = serialize(prop.getValues());
		Name propName = nameFromString(prop.getName(),"property value");
		return new PropInfo(propName,PropertyType.UNDEFINED,values.toArray(new TextValue[values.size()]));
	}

	private PropInfo createPropInfo(ImportProp prop) throws IOException {
		Name propName = nameFromString(prop.getName(), "property name");
		StringValue value[] = new StringValue[1];
		value[0] = serialize(prop.getValue());
		return new PropInfo(propName,PropertyType.UNDEFINED,value);
	}

	private void addMixins(ImportState state, ImportMvProp prop)
			throws IOException {
		Name mixin = null;
		for (Value value : prop.getValues()) {
			mixin = nameFromValue(value);
			state.addMixin(mixin);
		}
	}

	private Name nameFromString(String string, String exceptionMsg) throws IOException {
		Name name = null;
		try {
			name = resolver.getQName(string);
		} catch (IllegalNameException e) {
			throw new DiffException("Illegal " + exceptionMsg,
					e);
		} catch (NamespaceException e) {
			throw new DiffException("Illegal " + exceptionMsg,
					e);
		}
		return name;
	}

	private Name nameFromValue(Value value) throws IOException {
		Name name = null;
		try {
			name = resolver.getQName(value.getString());
		} catch (IllegalNameException e) {
			throw new DiffException("Invalide mixin name");
		} catch (NamespaceException e) {
			throw new DiffException("Invalide mixin name");
		} catch (ValueFormatException e) {
			throw new DiffException("Invalide mixin name");
		} catch (IllegalStateException e) {
			throw new DiffException("Invalide mixin name");
		} catch (RepositoryException e) {
			throw new DiffException("Invalide mixin name");
		}
		return name;
	}

	/*
	 * Serialize the given Value to a StringValue.
	 */
	private StringValue serialize(Value value) throws IOException {
		String serializedValue = null;

		try {
			serializedValue = ValueHelper.serialize(value, false);
			return new StringValue(serializedValue);
		} catch (IllegalStateException e) {
			throw new DiffException("TODO");
		} catch (RepositoryException e) {
			throw new DiffException("TODO");
		}

	}

	private List<StringValue> serialize(List<Value> values) throws IOException {
		StringValue vl = null;
		List<StringValue> vls = new ArrayList<StringValue>();
		for (Value v : values) {
			vl = serialize(v);
			vls.add(vl);
		}
		return vls;
	}

	// ----------------------------------------------------- < StringValue >
	// ---------------------------------------
	// -----
	private class StringValue implements TextValue {

		private String value;

		public StringValue(String value) {
			this.value = value;
		}

		@Override
		public Value getValue(int type, NamePathResolver resolver)
				throws ValueFormatException, RepositoryException {
			// We do this for all property types including NAME and PATH types...
			return ValueHelper.deserialize(value, type, false, vf);
		}

		@Override
		public InternalValue getInternalValue(int type)
				throws ValueFormatException, RepositoryException {
			return null;
		}

		@Override
		public void dispose() {
			// do nothing
		}

	}

	// ----------------------------------------------------- < ImportState >
	// ---------------------------------------
	// -----

	final class ImportState {
		private Name nodeName;
		private Name ntName;
		private String uuid;
		private List<Name> mixinNames;
		private List<PropInfo> props;
		private boolean started = false;

		ImportState(Name key) {
			this.nodeName = key;
		}

		public void setNodeType(Name ntName) {
			this.ntName = ntName;
		}

		public void setUUID(String uuid) {
			this.uuid = uuid;
		}

		public Name getName() {
			return nodeName;
		}

		public Name getNodeTypeName() {
			return ntName;
		}

		public String getUUID() {
			return uuid;
		}

		public List<Name> getMixinNames() {
			return mixinNames;
		}

		public void addMixin(Name mixinName) {
			if (mixinNames == null) {
				mixinNames = new ArrayList<Name>();
			}
			mixinNames.add(mixinName);
		}

		public void addPropInfo(PropInfo prop) {
			if (props == null) {
				props = new ArrayList<PropInfo>();
			}
			props.add(prop);
		}

		public List<PropInfo> getPropInfos() {
			return props;
		}

		public boolean started() {
			return started;
		}

		public void setFlag(boolean started) {
			this.started = started;
		}
	}

}
