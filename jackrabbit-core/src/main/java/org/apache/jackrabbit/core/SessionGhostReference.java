package org.apache.jackrabbit.core;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;

/**
 * Phantom reference for session objects that provides access to referent object.
 * Used to cleanup unclosed, orphan sessions.
 * @see <a href="http://www.javaspecialists.eu/archive/Issue098.html">Ghost references</a>
 * 
 * @author Roland Gruber
 *
 */
public class SessionGhostReference extends PhantomReference<SessionImpl> {

	/**
	 * {@inheritDoc}
	 */
	public SessionGhostReference(SessionImpl session, ReferenceQueue<? super SessionImpl> queue) {
		super(session, queue);
	}
	
	/**
	 * Closes the linked session if needed.
	 */
	public void cleanUp() {
		SessionImpl session = getReferent();
		if (session != null) {
			session.cleanup();
		}
		clear();
	}
	
	/**
	 * Returns the referenced object.
	 * 
	 * @return session
	 */
	public SessionImpl getReferent() {
		try {
			Field reqField = Reference.class.getDeclaredField("referent");
			reqField.setAccessible(true);
			return (SessionImpl) reqField.get(this);
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to get referenced session object", e);
		}
	}

}
