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
package net.sf.l2j.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.4.1 $ $Date: 2005/02/06 16:14:46 $
 */

public class ChatLogFormatter extends Formatter
{
	private static final String CRLF = "\r\n";
	
	private SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM H:mm:ss");
	
	@Override
	public String format(LogRecord record)
	{
		Object[] params = record.getParameters();
		final StringBuilder output = StringUtil.startAppend(30 + record.getMessage().length()
		        + (params != null ? 10 * params.length : 0), "[", dateFmt.format(new Date(record.getMillis())), "] ");
		
		if (params != null)
		{
			for (Object p : params)
			{
				StringUtil.append(output, String.valueOf(p), " ");
			}
		}
		
		StringUtil.append(output, record.getMessage(), CRLF);
		
		return output.toString();
	}
}
