package org.apache.jackrabbit.core;


/**
 * Cleans up non-closed sessions that are no longer in use.
 * 
 * @author Roland Gruber
 */
public class RepositorySessionCleaner implements Runnable {

	private RepositoryImpl repo;

	public RepositorySessionCleaner(RepositoryImpl repo) {
		this.repo = repo;
	}
	
	@Override
	public void run() {
		repo.cleanupPhantomSessions();
	}

}
