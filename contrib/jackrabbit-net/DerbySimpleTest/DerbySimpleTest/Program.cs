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
using java.lang;
using java.sql;
using Exception = System.Exception;
using String = System.String;


using org.apache.derby.jdbc;

namespace DerbySimpleTest
{
	class Program
	{
		static void Main(string[] args)
		{

			// The line below is needed if we want to use 
			// Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
			// We need the now unused 
			// using org.apache.derby.jdbc;
			// if we want to use 
			// EmbeddedDriver dummy = new EmbeddedDriver();
			AppDomain.CurrentDomain.Load("derby");

			new SimpleApp().go(args);
		}
	}


	public class SimpleApp
	{
		/* the default framework is embedded*/
		public String framework = "embedded";
		public String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		public String protocol = "jdbc:derby:";

		public static void main(String[] args)
		{
			new SimpleApp().go(args);
		}

		public void go(String[] args)
		{
			/* parse the arguments to determine which framework is desired*/
			parseArguments(args);

			Console.WriteLine("SimpleApp starting in " + framework + " mode.");

			try
			{

				/*
				   The driver is installed by loading its class.
				   In an embedded environment, this will start up Derby, since it is not already running.
				*/
				Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
				//EmbeddedDriver dummy = new EmbeddedDriver();

				Console.WriteLine("Loaded the appropriate driver.");

				Connection conn = null;
				java.util.Properties props = new java.util.Properties();
				props.put("user", "user1");
				props.put("password", "user1");

				/*
				   The connection specifies create=true to cause
				   the database to be created. To remove the database,
				   remove the directory derbyDB and its contents.
				   The directory derbyDB will be created under
				   the directory that the system property
				   derby.system.home points to, or the current
				   directory if derby.system.home is not set.
				 */
				conn = DriverManager.getConnection(protocol +
						"derbyDB;create=true", props);

				Console.WriteLine("Connected to and created database derbyDB");

				conn.setAutoCommit(false);

				/*
				   Creating a statement lets us issue commands against
				   the connection.
				 */
				Statement s = conn.createStatement();

				/*
				   We create a table, add a few rows, and update one.
				 */
				s.execute("create table derbyDB(num int, addr varchar(40))");
				Console.WriteLine("Created table derbyDB");
				s.execute("insert into derbyDB values (1956,'Webster St.')");
				Console.WriteLine("Inserted 1956 Webster");
				s.execute("insert into derbyDB values (1910,'Union St.')");
				Console.WriteLine("Inserted 1910 Union");
				s.execute(
					"update derbyDB set num=180, addr='Grand Ave.' where num=1956");
				Console.WriteLine("Updated 1956 Webster to 180 Grand");

				s.execute(
					"update derbyDB set num=300, addr='Lakeshore Ave.' where num=180");
				Console.WriteLine("Updated 180 Grand to 300 Lakeshore");

				/*
				   We select the rows and verify the results.
				 */
				ResultSet rs = s.executeQuery(
						"SELECT num, addr FROM derbyDB ORDER BY num");

				if (!rs.next())
				{
					throw new Exception("Wrong number of rows");
				}

				if (rs.getInt(1) != 300)
				{
					throw new Exception("Wrong row returned");
				}

				if (!rs.next())
				{
					throw new Exception("Wrong number of rows");
				}

				if (rs.getInt(1) != 1910)
				{
					throw new Exception("Wrong row returned");
				}

				if (rs.next())
				{
					throw new Exception("Wrong number of rows");
				}

				Console.WriteLine("Verified the rows");

				s.execute("drop table derbyDB");
				Console.WriteLine("Dropped table derbyDB");

				/*
				   We release the result and statement resources.
				 */
				rs.close();
				s.close();
				Console.WriteLine("Closed result set and statement");

				/*
				   We end the transaction and the connection.
				 */
				conn.commit();
				conn.close();
				Console.WriteLine("Committed transaction and closed connection");

				/*
				   In embedded mode, an application should shut down Derby.
				   If the application fails to shut down Derby explicitly,
				   the Derby does not perform a checkpoint when the JVM shuts down, which means
				   that the next connection will be slower.
				   Explicitly shutting down Derby with the URL is preferred.
				   This style of shutdown will always throw an "exception".
				 */
				bool gotSQLExc = false;

				if (framework.Equals("embedded"))
				{
					try
					{
						DriverManager.getConnection("jdbc:derby:;shutdown=true");
					}
					catch (SQLException se)
					{
						gotSQLExc = true;
					}

					if (!gotSQLExc)
					{
						Console.WriteLine("Database did not shut down normally");
					}
					else
					{
						Console.WriteLine("Database shut down normally");
					}
				}
			}
			catch (Throwable e)
			{
				Console.WriteLine("exception thrown:");
				e.printStackTrace();
			}

			Console.WriteLine("SimpleApp finished");
		}

		static void printSQLError(SQLException e)
		{
			while (e != null)
			{
				Console.WriteLine(e.toString());
				e = e.getNextException();
			}
		}

		private void parseArguments(String[] args)
		{
			int length = args.Length;

			for (int index = 0; index < length; index++)
			{
				if (args[index].Equals("jccjdbcclient"))
				{
					framework = "jccjdbc";
					driver = "com.ibm.db2.jcc.DB2Driver";
					protocol = "jdbc:derby:net://localhost:1527/";
				}
				if (args[index].Equals("derbyclient"))
				{
					framework = "derbyclient";
					driver = "org.apache.derby.jdbc.ClientDriver";
					protocol = "jdbc:derby://localhost:1527/";
				}
			}
		}
	}
	
}
