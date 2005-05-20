package org.apache.jackrabbit.taglib;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;

/**
 * Create test nodes    
 */
public class InitServlet extends HttpServlet
{
    private static Log log = LogFactory.getLog(InitServlet.class);

    private Repository repo;

    public void destroy()
    {
    }

    public void init() throws ServletException
    {
        this.createTree() ;
    }
    
    private void createTree(){
        try {
            Repository repo = this.getRepository() ;
            Session s = repo.login(new SimpleCredentials("admin", "".toCharArray())) ;
            Node root = s.getRootNode() ;
            if (!root.hasNode("TestA")) {
                // Tree
                Node ta = root.addNode("TestA", "nt:unstructured") ;
                ta.setProperty("prop1", "prop1 value V0");
                Node tb= root.addNode("TestB") ;
                
                // Versionable
                ta.addMixin("mix:versionable");
                s.save() ;
                ta.checkin() ;
                ta.checkout() ;
                ta.setProperty("prop1", "prop1 value V1");
                ta.save() ;
                ta.checkin() ;
                
                // Level 2
                ta.checkout() ;
                Node ta2= ta.addNode("A-L2") ;
                ta2.setProperty("msg", "test message");
                tb.addNode("B-L2(1)");
                tb.addNode("B-L2(2)");
                // Level 3
                Node ta3 = ta2.addNode("A-L3") ;
                Node ta4_1 = ta3.addNode("A-L4_1") ;
                Node ta4_2 = ta3.addNode("A-L4_3") ;
                s.save() ;
                
                Query q = s.getWorkspace().getQueryManager().createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/A/%'", Query.SQL) ;
                QueryResult qr = q.execute() ;
                if (IteratorUtils.toList(qr.getNodes()).size()==0) {
                    log.error("Index is not working");
                }
            }
            s.logout() ;
        } catch (Exception e) {
            log.error("Unable to init repo",e);
        }
        
    }
    
    private Repository getRepository() throws ServletException {
        try {
            InitialContext ctx = new InitialContext() ;
            Context env = (Context) ctx.lookup("java:comp/env");
            Repository repo = (Repository) env.lookup(JCRTagConstants.JNDI_DEFAULT_REPOSITORY);
            return repo ;
        }catch (Exception e) {
            throw new ServletException(e.toString(), e);
        }
    }

}