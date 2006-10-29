/*
   Copyright 2001, 2006 The Apache Software Foundation or its licensors, as applicable.

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
 

using System;
using java.io;
using javax.jcr;
using org.apache.jackrabbit.core;

namespace JackRabbitHops
{
	class Program
	{
		static void Main(string[] args)
		{

			AppDomain.CurrentDomain.Load("xercesImpl");
			AppDomain.CurrentDomain.Load("xalan");
			AppDomain.CurrentDomain.Load("derby");

			java.lang.System.setProperty(
					"javax.xml.parsers.DocumentBuilderFactory",
					"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

			java.lang.System.setProperty(
				"javax.xml.transform.TransformerFactory",
				"org.apache.xalan.processor.TransformerFactoryImpl");

			FirstHop();
			
			// If you run into trouble, comment out two lines below,
			// and make sure FirstHop is working ok.
			SecondHop();
			ThirdHop();		
		}

		public static void FirstHop()
		{
			Session session = null;
			try
			{
				Repository repository = new TransientRepository();
				session = repository.login();

				String user = session.getUserID();
				String name = repository.getDescriptor(Repository.__Fields.REP_NAME_DESC);
				Console.WriteLine("Logged in as " + user + " to " + name + " repository.");
			}
			catch (Exception e)
			{
				Console.WriteLine("Excetion: " + e.InnerException);
			}
			finally
			{
				if (session != null) 
					session.logout();
			}

		}





		public static void SecondHop()
		{
			Repository repository = new TransientRepository();

			// Login to the default workspace as a dummy user
			Session session = repository.login(
				new SimpleCredentials("username", "password".ToCharArray()));
			try
			{
				// Use the root node as a starting point
				Node root = session.getRootNode();

				// Create a test node unless it already exists
				if (!root.hasNode("testnode"))
				{
					Console.WriteLine("Creating testnode... ");
					// Create an unstructured node called "testnode"
					Node node = root.addNode("testnode", "nt:unstructured");
					// Add a string property called "testprop"
					node.setProperty("testprop", "Hello, World!");
					// Save the changes to the repository
					session.save();
					Console.WriteLine("done.");
				}

				// Use the property path to get and print the added property
				Property property = root.getProperty("testnode/testprop");
				Console.WriteLine(property.getString());
			}
			finally
			{
				session.logout();
			}

		}


		public static void ThirdHop()
		{
			// Set up a Jackrabbit repository with the specified
			// configuration file and repository directory
			Repository repository = new TransientRepository();

			// Login to the default workspace as a dummy user
			Session session = repository.login(
				new SimpleCredentials("username", "password".ToCharArray()));
			try
			{
				// Use the root node as a starting point
				Node root = session.getRootNode();

				// Import the XML file unless already imported
				if (!root.hasNode("importxml"))
				{
					Console.WriteLine("Importing xml... ");
					// Create an unstructured node under which to import the XML
					Node node = root.addNode("importxml", "nt:unstructured");
					// Import the file "test.xml" under the created node

					String s = System.IO.Directory.GetCurrentDirectory();

					FileInputStream xml = new FileInputStream("test.xml");
					session.importXML(
						"/importxml", xml, ImportUUIDBehavior.__Fields.IMPORT_UUID_CREATE_NEW);
					xml.close();
					// Save the changes to the repository
					session.save();
					Console.WriteLine("done.");
				}

				dump(root);
			}
			finally
			{
				session.logout();
			}
		}
		/** Recursively outputs the contents of the given node. */
		private static void dump(Node node)
		{
			// First output the node path
			Console.WriteLine(node.getPath());
			// Skip the virtual (and large!) jcr:system subtree
			if (node.getName().Equals("jcr:system"))
			{
				return;
			}

			// Then output the properties
			PropertyIterator properties = node.getProperties();
			while (properties.hasNext())
			{
				Property property = properties.nextProperty();
				if (property.getDefinition().isMultiple())
				{
					// A multi-valued property, print all values
					Value[] values = property.getValues();
					for (int i = 0; i < values.Length; i++)
					{
						Console.WriteLine(
							property.getPath() + " = " + values[i].getString());
					}
				}
				else
				{
					// A single-valued property
					Console.WriteLine(
						property.getPath() + " = " + property.getString());
				}
			}

			// Finally output all the child nodes recursively
			NodeIterator nodes = node.getNodes();
			while (nodes.hasNext())
			{
				dump(nodes.nextNode());
			}
		}

	}
}
