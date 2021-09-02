package net.sf.l2j.gameserver.script.mobius;
import java.util.logging.Logger;

/**
 * @author HorridoJoho
 */
public class ScriptingClassLoader extends ClassLoader
{
	public static final Logger LOGGER = Logger.getLogger(ScriptingClassLoader.class.getName());
	
	private Iterable<ScriptingOutputFileObject> _compiledClasses;
	
	ScriptingClassLoader(ClassLoader parent, Iterable<ScriptingOutputFileObject> compiledClasses)
	{
		super(parent);
		_compiledClasses = compiledClasses;
	}
	
	void removeCompiledClasses()
	{
		_compiledClasses = null;
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		for (ScriptingOutputFileObject compiledClass : _compiledClasses)
		{
			if (compiledClass.getJavaName().equals(name))
			{
				final byte[] classBytes = compiledClass.getJavaData();
				return defineClass(name, classBytes, 0, classBytes.length);
			}
		}
		return super.findClass(name);
	}
}
