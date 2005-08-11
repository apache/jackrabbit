package org.apache.jackrabbit.chain.command.xml;

import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Serializes the node to the given file using the System View Format
 */
public class ExportSysViewToFile extends AbstractExportViewToFile
{

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        boolean skipBinary = CtxHelper.getBooleanAttr(this.skipBinary,
            this.skipBinaryKey, false, ctx);

        boolean noRecurse = CtxHelper.getBooleanAttr(this.noRecurse,
            this.noRecurseKey, false, ctx);

        Session s = CtxHelper.getSession(ctx);
        Node current = CtxHelper.getCurrentNode(ctx);
        Node from = CtxHelper.getNode(ctx, CtxHelper.getAttr(this.from,
            this.fromKey, current.getPath(), ctx));

        OutputStream out = getOutputStream(ctx);
        s.exportSystemView(from.getPath(), out, skipBinary, noRecurse);
        out.close();

        return false;
    }

}
