package cz.nxs.debug;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import cz.nxs.interf.NexusEvents;

/**
 * @author hNoke
 *
 */
public class CommandListener implements ActionListener
{
    private final JTextField textField;
    
    public CommandListener(DebugConsole servergui, JTextField jtextfield)
    {
        textField = jtextfield;
    }

    @Override
    public void actionPerformed(ActionEvent actionevent)
    {
        String s = textField.getText().trim();
        if(s.length() > 0)
        {
        	NexusEvents.consoleCommand(s);
        	DebugConsole.userCmd("[COMMAND] " + s + "\n");
        }
        textField.setText("");
    }
}
