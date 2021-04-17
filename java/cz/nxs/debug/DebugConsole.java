package cz.nxs.debug;



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import cz.nxs.events.NexusLoader;

/**
 * @author hNoke
 *
 */
public class DebugConsole extends JComponent
{
	private static final long serialVersionUID = 1L;
	
	public static Logger logger = Logger.getLogger("NexusDebug");
	
    public static void initGui()
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception exception) 
        {
        }
        
        DebugConsole console = new DebugConsole();
        JFrame jframe = new JFrame("Nexus Events " + NexusLoader.version);
        jframe.add(console);
        jframe.pack();
		jframe.setLocationRelativeTo((Component) null);
       //jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);
    }

    public DebugConsole()
    {
        setPreferredSize(new Dimension(520, 648));
        setLayout(new BorderLayout());
        try
        {
            add(getLogComponent(), "Center");
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
    }

    private JComponent getLogComponent()
    {
        JPanel jpanel = new JPanel(new BorderLayout());
        
        jtextarea = new JTextArea();
        logger.addHandler(new OutputHandler(jtextarea));
        JScrollPane jscrollpane = new JScrollPane(jtextarea, 22, 30);
        jtextarea.setEditable(false);
        
        JTextField jtextfield = new JTextField();
        jtextfield.addActionListener(new CommandListener(this, jtextfield));
        
        //jtextarea.addFocusListener(new DebugConsoleFocusAdapter(this));
        
        jpanel.add(jscrollpane, "Center");
        jpanel.add(jtextfield, "South");
        jpanel.setBorder(new TitledBorder(new EtchedBorder(), "Logs"));
        
        return jpanel;
    }
    
    public static JTextArea jtextarea;
    
    public static void userCmd(String s)
    {
    	jtextarea.append(s);
    }
    
    public static void log(Level level, String s)
    {
    	logger.log(level, s);
    }
    
    public static void info(String s)
    {
    	logger.info(s);
    }
    
    public static void warning(String s)
    {
    	logger.warning(s);
    }
    
    public static void severe(String s)
    {
    	logger.severe(s);
    }
}
