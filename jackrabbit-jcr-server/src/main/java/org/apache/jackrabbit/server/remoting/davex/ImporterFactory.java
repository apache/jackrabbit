package org.apache.jackrabbit.server.remoting.davex;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.core.config.ImportConfig;
import org.apache.jackrabbit.core.xml.AccessControlImporter;
import org.apache.jackrabbit.core.xml.ProtectedNodeImporter;

/*
 * Factory for creating an importer and plugging-in a custom pseudo configuration
 * containing an "access control import handler".
 *
 */
class ImporterFactory {
	
	public JsopImporter createImporter(Node parent, Session session){
		return new JsopImporter(parent, session, new PseudoConfig());
	}
	
	// --------------------------------- ImportConfig
	private final class PseudoConfig extends ImportConfig {

		private final ProtectedNodeImporter aci;

		private PseudoConfig() {
			this.aci = new AccessControlImporter();
		}

		@Override
		public List<? extends ProtectedNodeImporter> getProtectedItemImporters() {
			return Collections.singletonList(aci);
		}
	}

}
