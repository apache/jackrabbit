package org.apache.jackrabbit.browser;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.InitialContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.jackrabbit.command.CommandHelper;

public class JcrSessionListener implements HttpSessionListener {

	private static Repository repository;

	public void sessionCreated(HttpSessionEvent evt) {

	}

	public void sessionDestroyed(HttpSessionEvent evt) {
		Session s = (Session) evt.getSession().getAttribute(
				CommandHelper.SESSION_KEY);
		if (s!=null) {
			s.logout();
		}
	}

	public static Repository getRepository() {
		if (repository == null) {
			try {
				InitialContext ctx = new InitialContext();
				String jndiAddress = (String) ctx
						.lookup("java:comp/env/jcr/jndi/address");
				String jndiProperties = (String) ctx
						.lookup("java:comp/env/jcr/jndi/properties");
				Properties properties = new Properties();
				properties.load(new ByteArrayInputStream(jndiProperties
						.getBytes()));
				InitialContext repoCtx = new InitialContext(properties);
				repository = (Repository) repoCtx.lookup(jndiAddress);
			} catch (Exception e) {
				throw new IllegalStateException(
						"unable to retrieve repository. ", e);
			}
		}
		return repository;
	}

}
