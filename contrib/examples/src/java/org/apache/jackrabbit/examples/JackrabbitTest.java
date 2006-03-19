/*
   Copyright 2004-2005 The Apache Software Foundation or its licensors,
                       as applicable.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.apache.jackrabbit.examples;

import java.io.*;

import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;

import javax.jcr.*;

public class JackrabbitTest
    extends HttpServlet
{
    Repository ownedRepository = null;
    Repository sharedRepository = null;
    
    public void doGet( HttpServletRequest request, HttpServletResponse response )
        throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Jackrabbit Deployment Testing</title>");
        out.println("</head>");
        out.println("<body>");
        
        getRepositoriesFromJNDI( out );
        
        if( ownedRepository != null )
        {
            out.println( "<hr>" );
            out.println( "<h3>Testing the embedded repository</h3>" );
            
            try
            {
                testRepository( ownedRepository, out );
            }
            catch( IllegalStateException e )
            {
                out.println(
                    "<p><b>IllegalStateException thrown logging into ownedRepository</b>."
                    + "This is likely due to the repository having been shut down. You will "
                    + "need to restart Tomcat before the repository will be accessible again."
                    + "</p>" );
            }
        }
        
        if( sharedRepository != null )
        {
            out.println( "<hr>" );
            out.println( "<h3>Testing the shared repository</h3>" );
            testRepository( sharedRepository, out );
        }
        
        out.println("</body>");
        out.println("</html>");
    }
    
    private void getRepositoriesFromJNDI( PrintWriter out )
    {
        out.println("<h3>Getting the Jackrabbit reference via JNDI</h3>");
            
        try
        {
        // Get the initial context
            Context initialContext = new InitialContext();
            
            out.println( "<p>Got initial context</p>" );
            
            listContextEntries( out, initialContext, "java:comp/env" );
            listContextEntries( out, initialContext, "java:comp/env/jcr" );
            
        // Get the environment context
            Context environmentContext = (Context) initialContext.lookup( "java:comp/env" );
            
            out.println( "<p>Got environment context</p>" );
            
            listContextEntries( out, environmentContext, "/" );
            listContextEntries( out, environmentContext, "/jcr" );
            
        // get values out of the context.
            
            ownedRepository = (Repository) getObjectFromContext(
                out, environmentContext, "jcr/model1Repository" );
            out.println( "<p>ownedRepository = " + ownedRepository + "</p>" );
            
            try
            {
                sharedRepository = (Repository) getObjectFromContext(
                    out, environmentContext, "sharedJCRRepository" );
                out.println( "<p>sharedJCRRepository = " + sharedRepository + "</p>" );
            }
            catch( ClassCastException e )
            {
                out.println(
                    "<p><b>ClassCastException thrown setting testObj to sharedRepository</b>."
                    + "This is likely a class loader issue. Since the repository is shared "
                    + "between all webapps, you need to have jcr-1.0.jar in "
                    + "<code>$TOMCAT_HOME/common/lib</code>. If you also include jcr-1.0.jar "
                    + "in your webapp's <code>WEB-INF/lib</code>, that instance of the "
                    + "Repository.class will take precedence within the webapp. But Tomcat's "
                    + "instance was created with the Repository.class in the global jar file "
                    + "leading to the ClassCastException.</p>" );
                
                sharedRepository = null;
            }
        }
        catch( Exception e )
        {
            out.println( "<p>Exception thrown: <pre>" );
            e.printStackTrace( out );
            out.println( "</pre></p>" );
        }
    }
    
    private Object getObjectFromContext( PrintWriter out, Context context, String path )
    {
        Object o = null;
        
        try
        {
            o = context.lookup( path );
        }
        catch( NameNotFoundException e )
        {
            out.println( "<p>Could not load " + path + " from the context.<pre>" );
            e.printStackTrace( out );
            out.println( "</pre></p>" );
        }
        catch( NamingException e )
        {
            out.println( "<p>Naming exception:<pre>" );
            e.printStackTrace( out );
            out.println( "</pre></p>" );
        }
        
        return( o );
    }
    
    private void listContextEntries( PrintWriter out, Context context, String path )
    {
        if( ( out == null ) || ( context == null ) || ( path == null ) )
        {
            throw new IllegalArgumentException( "parameters must not be null: out = "
                + out + ", context = " + context + ", path = " + path );
        }
        
        try
        {
            NamingEnumeration contextEntries = context.list( path );
            
            out.println( "<p>Entries at <code>" + path + "</code>:" );
            
            while( contextEntries.hasMore() )
            {
                NameClassPair entry = (NameClassPair) contextEntries.next();
                out.println( "<li><code>" + entry + "</code></li>" );
            }
        
            out.println( "</p>" );
        }
        catch( NamingException e )
        {
            out.println(
                "<p>Exception thrown printing contents of <code>" + path + "</code>: <pre>" );
            e.printStackTrace( out );
            out.println( "</pre></p>" );
        }
    }
    
    private void testRepository( Repository repository, PrintWriter out )
    {
        Session session = null;
        
        try
        {
            session = repository.login(
                new SimpleCredentials( "username", "password".toCharArray() ) );
        }
        catch( LoginException e )
        {
            out.println( "<p>LoginException thrown: <pre>" );
            e.printStackTrace( out );
            out.println( "</pre></p>" );
        }
        catch( RepositoryException e )
        {
            out.println( "<p>Exception logging into repository: <pre>" );
            e.printStackTrace( out );
            out.println( "</pre></p>" );
        }
        
        if( session != null )
        {
            try
            {
                out.println( "<p>Login complete, session = " + session + "</p>" );
                
                Node root = session.getRootNode();
                out.println( "<p>Root node is type: "
                    + root.getPrimaryNodeType().getName() + "</p>" );
                
                Node node = null;
                
                if( !root.hasNode( "testnode" ) )
                {
                    out.println( "<p>Creating \"testnode\".</p>" );
                    
                    node = root.addNode( "testnode", "nt:unstructured" );
                    
                    out.println( "<p>Adding \"testprop\" to new node.</p>" );
                    
                    node.setProperty( "testprop", "Hello, World!" );
                    
                    out.println( "<p>Saving changes.</p>" );
                    
                    session.save();
                }
                else
                {
                    out.println( "<p>You've done this before. \"testnode\" already exists.</p>" );
                    
                    out.println( "<p>Getting previously created \"testnode\".</p>" );
                    node = (Node) root.getNode( "testnode" );
                }
                
                out.println( "<p>Getting \"testprop\" via the root node.</p>" );
                Property propViaRoot = root.getProperty( "testnode/testprop" );
                out.println( "<p>Property = " + propViaRoot.getString() + "</p>" );
                
                out.println( "<p>Getting \"testprop\" via the test node.</p>" );
                Property propViaNode = node.getProperty( "testprop" );
                out.println( "<p>Property = " + propViaNode.getString() + "</p>" );
                
                session.logout();
            }
            catch( Exception e )
            {
                out.println( "<p>Exception while using repository: <pre>" );
                e.printStackTrace( out );
                out.println( "</pre></p>" );
            }
        }
    }
}
