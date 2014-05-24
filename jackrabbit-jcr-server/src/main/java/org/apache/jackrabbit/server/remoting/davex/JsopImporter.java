package org.apache.jackrabbit.server.remoting.davex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.config.ImportConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.xml.DefaultProtectedItemImporter;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.ProtectedItemImporter;
import org.apache.jackrabbit.core.xml.ProtectedNodeImporter;
import org.apache.jackrabbit.core.xml.ProtectedPropertyImporter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The import task is responsible for the import. The actual importing of the content is delegated to
// configured (protected item import) handlers
public class JsopImporter implements Importer {

	private static Logger log = LoggerFactory.getLogger(JsopImporter.class);

	private SessionImpl session;
	private Stack<NodeImpl> parents;
	private final static int uuidBehaviour = ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
	private final NodeImpl importTargetNode;
	
	private final List<ProtectedItemImporter> pItemImporters = new ArrayList<ProtectedItemImporter>();

	/**
	 * Currently active importer for protected nodes.
	 */
	private ProtectedNodeImporter pnImporter = null;

	private final ReferenceChangeTracker refTracker;

	public JsopImporter(Node importTargetNode, Session session, ImportConfig config) {

		this.session = (SessionImpl) session;
		this.importTargetNode = (NodeImpl)importTargetNode;
		
		refTracker = new ReferenceChangeTracker();
		parents = new Stack<NodeImpl>();
		parents.push((NodeImpl) importTargetNode);

		if (config != null) {
			List<? extends ProtectedItemImporter> iList = config
					.getProtectedItemImporters();
			for (ProtectedItemImporter importer : iList) {
				if (importer.init((SessionImpl) session, (SessionImpl) session,
						false, uuidBehaviour, refTracker)) {
					pItemImporters.add(importer);
				}
			}
		} else {
			ProtectedItemImporter def = new DefaultProtectedItemImporter();
			if (def.init((SessionImpl) session, (SessionImpl) session, false,
					uuidBehaviour, refTracker)) {
				pItemImporters.add(def);
			}
		}

	}

	@Override
	public void start() throws RepositoryException {
		// nop
	}


	protected void checkPermission(NodeImpl parent, Name nodeName)
			throws RepositoryException {
		if (!session.getAccessManager().isGranted(
				session.getQPath(parent.getPath()), nodeName,
				Permission.NODE_TYPE_MNGMT)) {
			throw new AccessDeniedException("Insufficient permission.");
		}
	}

	@Override
	public void startNode(NodeInfo nodeInfo, List<PropInfo> propInfos)
			throws RepositoryException {
		NodeImpl parent = parents.peek();

		// process node

		NodeImpl node = null;
		NodeId id = nodeInfo.getId();
		
		Name nodeName = nodeInfo.getName();
		Name ntName = nodeInfo.getNodeTypeName();
		Name[] mixins = nodeInfo.getMixinNames();

		if (parent == null) {
			log.debug("Skipping node: " + nodeName);
			// parent node was skipped, skip this child node too
			parents.push(null); // push null onto stack for skipped node
			// notify the p-i-importer
			if (pnImporter != null) {
				pnImporter.startChildInfo(nodeInfo, propInfos);
			}
			return;
		}
		NodeDefinition nodeDef = parent.getDefinition();
		boolean isProtected = nodeDef.isProtected();
		if (isProtected) {
			// skip protected node
			parents.push(null);
			log.debug("Skipping protected node: " + nodeName);

			if (pnImporter != null) {
				// pnImporter was already started (current nodeInfo is a
				// sibling)
				// notify it about this child node.
				pnImporter.startChildInfo(nodeInfo, propInfos);
			} else {
				// no importer defined yet:
				// test if there is a ProtectedNodeImporter among the configured
				// importers that can handle this.
				// if there is one, notify the ProtectedNodeImporter about the
				// start of a item tree that is protected by this parent. If it
				// potentially is able to deal with it, notify it about the
				// child node.
				for (ProtectedItemImporter pni : pItemImporters) {
					if (pni instanceof ProtectedNodeImporter
							&& ((ProtectedNodeImporter) pni).start(parent)) {
						log.debug("Protected node -> delegated to ProtectedNodeImporter");
						pnImporter = (ProtectedNodeImporter) pni;
						pnImporter.startChildInfo(nodeInfo, propInfos);
						break;
					} /*
					 * else: p-i-Importer isn't able to deal with the protected
					 * tree. try next. and if none can handle the passed parent
					 * the tree below will be skipped
					 */
				}
			}
			return;
		}

		if (parent.hasNode(nodeName)) {
			// a node with that name already exists...
			NodeImpl existing = parent.getNode(nodeName);
			NodeDefinition def = existing.getDefinition();
			if (!def.allowsSameNameSiblings()) {
				// existing doesn't allow same-name siblings,
				// check for potential conflicts
				if (def.isProtected() && existing.isNodeType(ntName)) {
					/*
					 * use the existing node as parent for the possible
					 * subsequent import of a protected tree, that the protected
					 * node importer may or may not be able to deal with. ->
					 * upon the next 'startNode' the check for the parent being
					 * protected will notify the protected node importer. -> if
					 * the importer is able to deal with that node it needs to
					 * care of the complete subtree until it is notified during
					 * the 'endNode' call. -> if the import can't deal with that
					 * node or if that node is the a leaf in the tree to be
					 * imported 'end' will not have an effect on the importer,
					 * that was never started.
					 */
					log.debug("Skipping protected node: " + existing);
					parents.push(existing);
					return;
				}
				if (def.isAutoCreated() && existing.isNodeType(ntName)) {
					// this node has already been auto-created, no need to
					// create it
					node = existing;
				} else {
					// edge case: colliding node does have same uuid
					// (see http://issues.apache.org/jira/browse/JCR-1128)
					if (!(existing.getId().equals(id) && (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING || uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING))) {
						throw new ItemExistsException(
								"Node with the same UUID exists:" + existing);
					}
					// fall through
				}
			}
		}

		if (node == null) {
			// create node
			if (id == null) {
				// no potential uuid conflict, always add new node
				checkPermission(parent, nodeName);
				node = createNode(parent, nodeName, ntName, mixins, null);
			} else {
				// potential uuid conflict
				boolean isConflicting;
				try {
					// the following is a fail-fast test whether
					// an item exists (regardless of access control)
					session.getHierarchyManager().getName(id);
					isConflicting = true;
				} catch (ItemNotFoundException infe) {
					isConflicting = false;
				}

				if (isConflicting) {
					// resolve uuid conflict
					node = resolveUUIDConflict(parent, id, nodeInfo);
					if (node == null) {
						// no new node has been created, so skip this node
						parents.push(null); // push null onto stack for skipped
											// node
						log.debug("Skipping existing node "
								+ nodeInfo.getName());
						return;
					}
				} else {
					// create new with given uuid
					checkPermission(parent, nodeName);
					node = createNode(parent, nodeName, ntName, mixins, id);
				}
			}
		}

		// process properties

		for (PropInfo pi : propInfos) {
			// find applicable definition
			QPropertyDefinition def = pi.getApplicablePropertyDef(node
					.getEffectiveNodeType());
			if (def.isProtected()) {
				// skip protected property
				log.debug("Skipping protected property " + pi.getName());

				// notify the ProtectedPropertyImporter.
				for (ProtectedItemImporter ppi : pItemImporters) {
					if (ppi instanceof ProtectedPropertyImporter
							&& ((ProtectedPropertyImporter) ppi)
									.handlePropInfo(node, pi, def)) {
						log.debug("Protected property -> delegated to ProtectedPropertyImporter");
						break;
					} /*
					 * else: p-i-Importer isn't able to deal with this property.
					 * try next pp-importer
					 */

				}
			} else {
				// regular property -> create the property
				createProperty(node, pi, def);
			}
		}

		parents.push(node);

	}

	@Override
	public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        NodeImpl parent = parents.pop();
        if (parent == null) {
            if (pnImporter != null) {
                pnImporter.endChildInfo();
            }
        } else if (parent.getDefinition().isProtected()) {
            if (pnImporter != null) {
                pnImporter.end(parent);
                // and reset the pnImporter field waiting for the next protected
                // parent -> selecting again from available importers
                pnImporter = null;
            }
        }

	}

	@Override
	public void end() throws RepositoryException {
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly generated uuid's on import
         */
        // 1. let protected property/node importers handle protected ref-properties
        //    and (protected) properties underneath a protected parent node.
        for (ProtectedItemImporter ppi : pItemImporters) {
            ppi.processReferences();
        }

        // 2. regular non-protected properties.
        Iterator<Object> iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            Object ref = iter.next();
            if (!(ref instanceof Property)) {
                continue;
            }

            Property prop = (Property) ref;
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE
                    && prop.getType() != PropertyType.WEAKREFERENCE) {
                continue;
            }
            if (prop.isMultiple()) {
                Value[] values = prop.getValues();
                Value[] newVals = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    Value val = values[i];
                    NodeId original = new NodeId(val.getString());
                    NodeId adjusted = refTracker.getMappedId(original);
                    if (adjusted != null) {
                        newVals[i] = session.getValueFactory().createValue(
                                session.getNodeById(adjusted),
                                prop.getType() != PropertyType.REFERENCE);
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = val;
                    }
                }
                prop.setValue(newVals);
            } else {
                Value val = prop.getValue();
                NodeId original = new NodeId(val.getString());
                NodeId adjusted = refTracker.getMappedId(original);
                if (adjusted != null) {
                    prop.setValue(session.getNodeById(adjusted).getUUID());
                }
            }
        }
        refTracker.clear();
    }

	// ------------------------ Helper methods. Copied directly from jackrabbit-core xml#SessionImporter -----------------------
	
	protected NodeImpl resolveUUIDConflict(NodeImpl parent,
			NodeId conflictingId, NodeInfo nodeInfo) throws RepositoryException {
		NodeImpl node;

		NodeImpl conflicting;
		try {
			conflicting = session.getNodeById(conflictingId);
		} catch (ItemNotFoundException infe) {
			// conflicting node can't be read,
			// most likely due to lack of read permission
			conflicting = null;
		}

		if (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
			// create new with new uuid
			checkPermission(parent, nodeInfo.getName());
			node = createNode(parent, nodeInfo.getName(),
					nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
			// remember uuid mapping
			if (node.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
				refTracker.mappedId(nodeInfo.getId(), node.getNodeId());
			}
		} else if (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
			// if conflicting node is shareable, then clone it
			if (conflicting != null
					&& conflicting.isNodeType(NameConstants.MIX_SHAREABLE)) {
				parent.clone(conflicting, nodeInfo.getName());
				return null;
			}
			String msg = "a node with uuid " + nodeInfo.getId()
					+ " already exists!";
			log.debug(msg);
			throw new ItemExistsException(msg);
		} else if (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
			if (conflicting == null) {
				// since the conflicting node can't be read,
				// we can't remove it
				String msg = "node with uuid " + conflictingId
						+ " cannot be removed";
				log.debug(msg);
				throw new RepositoryException(msg);
			}

			// make sure conflicting node is not importTargetNode or an ancestor
			// thereof
			if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
				String msg = "cannot remove ancestor node";
				log.debug(msg);
				throw new ConstraintViolationException(msg);
			}
			// remove conflicting
			conflicting.remove();
			// create new with given uuid
			checkPermission(parent, nodeInfo.getName());
			node = createNode(parent, nodeInfo.getName(),
					nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
					nodeInfo.getId());
		} else if (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
			if (conflicting == null) {
				// since the conflicting node can't be read,
				// we can't replace it
				String msg = "node with uuid " + conflictingId
						+ " cannot be replaced";
				log.debug(msg);
				throw new RepositoryException(msg);
			}

			if (conflicting.getDepth() == 0) {
				String msg = "root node cannot be replaced";
				log.debug(msg);
				throw new RepositoryException(msg);
			}
			// 'replace' current parent with parent of conflicting
			parent = (NodeImpl) conflicting.getParent();

			// replace child node
			checkPermission(parent, nodeInfo.getName());
			node = parent.replaceChildNode(nodeInfo.getId(),
					nodeInfo.getName(), nodeInfo.getNodeTypeName(),
					nodeInfo.getMixinNames());
		} else {
			String msg = "unknown uuidBehavior: " + uuidBehaviour;
			log.debug(msg);
			throw new RepositoryException(msg);
		}
		return node;
	}
	
    protected void createProperty(NodeImpl node, PropInfo pInfo, QPropertyDefinition def) throws RepositoryException {
        // convert serialized values to Value objects
        Value[] va = pInfo.getValues(pInfo.getTargetType(def), session);

        // multi- or single-valued property?
        Name name = pInfo.getName();
        int type = pInfo.getType();
        if (va.length == 1 && !def.isMultiple()) {
            Exception e = null;
            try {
                // set single-value
                node.setProperty(name, va[0]);
            } catch (ValueFormatException vfe) {
                e = vfe;
            } catch (ConstraintViolationException cve) {
                e = cve;
            }
            if (e != null) {
                // setting single-value failed, try setting value array
                // as a last resort (in case there are ambiguous property
                // definitions)
                node.setProperty(name, va, type);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            node.setProperty(name, va, type);
        }
        if (type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE) {
            // store reference for later resolution
            refTracker.processedReference(node.getProperty(name));
        }
    }

    // should i use importNode() in JsonDiffHandler?
	private NodeImpl createNode(NodeImpl parent, Name nodeName,
			Name nodeTypeName, Name[] mixinNames, NodeId id)
			throws RepositoryException {
		NodeImpl node;

		// add node
		node = parent.addNode(nodeName, nodeTypeName, id);
		// add mixins
		if (mixinNames != null) {
			for (Name mixinName : mixinNames) {
				node.addMixin(mixinName);
			}
		}
		return node;
	}


}
