package luna.custom.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LunaLogger
{
	public void log(String filename, String message)
	{
		File file = new File("data/customlogs/" + filename+".txt");
		FileWriter fr = null;
		if (file.exists())
		{
			try
			{
				fr = new FileWriter(file, true);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // if file exists append to file. Works fine.
		}
		else
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try
			{
				fr = new FileWriter(file);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try
		{
			fr.write("");
		}
		catch (IOException e2)
		{
			e2.printStackTrace();
		}
		try
		{
			fr.close();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			fr = new FileWriter(file, true);
			fr.write(message + "\r\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				fr.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	public void logLs(String filename, String message)
	{
		File file = new File("../gs/data/customlogs/" + filename+".txt");
		FileWriter fr = null;
		if (file.exists())
		{
			try
			{
				fr = new FileWriter(file, true);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // if file exists append to file. Works fine.
		}
		else
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try
			{
				fr = new FileWriter(file);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try
		{
			fr.write("");
		}
		catch (IOException e2)
		{
			e2.printStackTrace();
		}
		try
		{
			fr.close();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			fr = new FileWriter(file, true);
			fr.write(message + "\r\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				fr.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static LunaLogger getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LunaLogger _instance = new LunaLogger();
	}
}
