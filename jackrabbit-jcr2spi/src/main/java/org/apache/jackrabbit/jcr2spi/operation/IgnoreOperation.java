package org.apache.jackrabbit.jcr2spi.operation;

/**
 * Marker interface that represent operations which must be ignored
 * by the <code>SessionItemStateManager</code> for building the final ChangeLog. Instances
 * of <code>IgnoreOperation</code> never appear in the ChangeLog.
 */
public interface IgnoreOperation {
}
