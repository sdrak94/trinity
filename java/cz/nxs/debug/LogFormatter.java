package cz.nxs.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author hNoke & Minecraft server console
 *
 */
public class LogFormatter extends Formatter
{
    public final OutputHandler outputHandler;
    
    public LogFormatter(OutputHandler guilogoutputhandler)
    {
        outputHandler = guilogoutputhandler;
    }

    public String format(LogRecord logrecord)
    {
        StringBuilder stringbuilder = new StringBuilder();
        Level level = logrecord.getLevel();
        stringbuilder.append((new StringBuilder()).append("[").append(level.getLocalizedName().toUpperCase()).append("] ").toString());
        
        stringbuilder.append(logrecord.getMessage());
        stringbuilder.append('\n');
        Throwable throwable = logrecord.getThrown();
        if(throwable != null)
        {
            StringWriter stringwriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringwriter));
            stringbuilder.append(stringwriter.toString());
        }
        return stringbuilder.toString();
    }
}
