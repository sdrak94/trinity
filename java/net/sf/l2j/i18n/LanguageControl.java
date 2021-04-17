/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.i18n;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 *
 * @author  KenM
 */
public class LanguageControl extends Control
{
	public static final String LANGUAGES_DIRECTORY = "../languages/";
	
	public static final LanguageControl INSTANCE = new LanguageControl();
	
	/**
	 * prevent instancing, allows sub-classing
	 */
	protected LanguageControl()
	{
		
	}
	
	@Override
	public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException
	{
		if (baseName == null || locale == null || format == null || loader == null)
		{
			throw new NullPointerException();
		}
		ResourceBundle bundle = null;
		if (format.equals("java.properties"))
		{
			format = "properties";
			String bundleName = toBundleName(baseName, locale);
			String resourceName = LANGUAGES_DIRECTORY + toResourceName(bundleName, format);
			InputStream stream = new FileInputStream(resourceName);
			if (stream != null)
			{
				BufferedInputStream bis = new BufferedInputStream(stream);
				try
				{
					bundle = new PropertyResourceBundle(bis);
				}
				finally
				{
					bis.close();
				}
			}
		}
		return bundle;
	}
	
}
