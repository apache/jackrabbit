package org.apache.jackrabbit.ocm.manager.version;


import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;
import org.apache.jackrabbit.ocm.version.Version;
import org.apache.jackrabbit.ocm.version.VersionIterator;

/**
 * Test Query on atomic fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class DigesterBasicVersionningTest extends DigesterTestBase
{
	private final static Log log = LogFactory.getLog(DigesterBasicVersionningTest.class);
	
	
	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public DigesterBasicVersionningTest(String testName) throws Exception
	{
		super(testName);
		
	}

	public static Test suite()
	{
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(
                new TestSuite(DigesterBasicVersionningTest.class));
	}

    public void tearDown() throws Exception
    {
    	cleanUpRepisotory();
    	ocm.save();
       
        super.tearDown();
    }	

	public void testSimpleVersion()
	{
		     ObjectContentManager ocm = getObjectContentManager();
             try
             {
            	 
            	 Page page = new Page();
            	 page.setPath("/page");
            	 page.setTitle("Page Title");            	 
            	 page.addParagraph(new Paragraph("para1"));
            	 page.addParagraph(new Paragraph("para2"));
            	 ocm.insert(page);
            	 ocm.save();
            	 
                 
            	 page.addParagraph(new Paragraph("para3"));
            	 ocm.checkout("/page");
            	 ocm.update(page);
            	 ocm.save();
            	 ocm.checkin("/page");
            	 
            	 page.addParagraph(new Paragraph("para4"));
            	 ocm.checkout("/page");
            	 ocm.update(page);
            	 ocm.save();
            	 ocm.checkin("/page");            	 

            	 VersionIterator versionIterator = ocm.getAllVersions("/page");
            	 assertNotNull("VersionIterator is null", versionIterator);
            	 assertTrue("Invalid number of versions found", versionIterator.getSize() == 3);
            	 
            	 while (versionIterator.hasNext())
            	 {
            		 Version version = (Version) versionIterator.next();
            		 log.info("version found : "+ version.getName() + " - " + version.getPath() + " - " +  version.getCreated().getTime());
            		 
            	 }
            	 
            	 Version baseVersion = ocm.getBaseVersion("/page");
            	 System.out.println("Base version : " + baseVersion.getName());

            	 Version rootVersion = ocm.getRootVersion("/page");
            	 System.out.println("Root version : " + rootVersion.getName());
            	 //this.exportDocument("/home/christophe/export.xml", "/jcr:system/jcr:versionStorage", true, false);
            	             	
                 //Get the latest version 
            	 page = (Page) ocm.getObject( "/page");
            	 assertNotNull("Last version is nulll", page);
            	 assertTrue("Invalid number of paragraph found in the last  version", page.getParagraphs().size() == 4);

            	 
            	 //Get the object matching to the first version 
                 Page  page1 = (Page) ocm.getObject( "/page", "1.0");
            	 assertNotNull("version 1.0 object is null", page1);
            	 assertTrue("Invalid number of paragraph found in the root version", page1.getParagraphs().size() == 3);

             }
             catch(Exception e)
             {
            	 e.printStackTrace();
            	 fail(e.getMessage());
            	 
             }
	}

	
	public void testVersionLabels()
	{
		     ObjectContentManager ocm = getObjectContentManager();
             try
             {
            	 
            	 Page page = new Page();
            	 page.setPath("/page");
            	 page.setTitle("Page Title");            	 
            	 page.addParagraph(new Paragraph("para1"));
            	 page.addParagraph(new Paragraph("para2"));
            	 ocm.insert(page);
            	 ocm.save();
            	 
                 
            	 page.addParagraph(new Paragraph("para3"));
            	 ocm.checkout("/page");
            	 ocm.update(page);
            	 ocm.save();
            	 ocm.checkin("/page", new String[] {"A", "B"});
            	 
            	 page.addParagraph(new Paragraph("para4"));
            	 ocm.checkout("/page");
            	 ocm.update(page);
            	 ocm.save();
            	 ocm.checkin("/page", new String[] {"C", "D"});         	 

            	 String[] allLabels = ocm.getAllVersionLabels("/page");
            	 assertTrue("Incorrect number of labels", allLabels.length == 4);

            	 String[] versionLabels = ocm.getVersionLabels("/page", "1.1");
            	 assertTrue("Incorrect number of labels", versionLabels.length == 2);
            	 assertTrue("Incorrect label", versionLabels[0].equals("C") || versionLabels[0].equals("D"));
            	 assertTrue("Incorrect label", versionLabels[1].equals("C") || versionLabels[0].equals("D"));
            	

             }
             catch(Exception e)
             {
            	 e.printStackTrace();
            	 fail(); 
             }
	}
	
}