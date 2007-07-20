package org.apache.jackrabbit.core.journal;

import org.apache.jackrabbit.util.Text;



/**
 * It has the following property in addition to those of the DatabaseJournal:
 * <ul>
 * <li><code>tableSpace</code>: the Oracle tablespace to use</li>
 * </ul>
 */
public class OracleDatabaseJournal extends DatabaseJournal {

    /** the variable for the Oracle table space */
    public static final String TABLE_SPACE_VARIABLE =
        "${tableSpace}";

    /** the Oracle table space to use */
    protected String tableSpace;

    /**
     * Returns the configured Oracle table space.
     * @return the configured Oracle table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the Oracle table space.
     * @param tableSpace the Oracle table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null) {
            this.tableSpace = tableSpace.trim();
        } else {
            this.tableSpace = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected String createSchemaSQL(String sql) {
        // replace the schemaObjectPrefix
        sql = super.createSchemaSQL(sql);
        // set the tablespace if it is defined
        String tspace;
        if (tableSpace == null || "".equals(tableSpace)) {
            tspace = "";
        } else {
            tspace = "tablespace " + tableSpace;
        }
        return Text.replace(sql, TABLE_SPACE_VARIABLE, tspace).trim();
    }



}
