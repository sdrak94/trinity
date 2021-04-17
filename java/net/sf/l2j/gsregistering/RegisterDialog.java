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
package net.sf.l2j.gsregistering;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileFilter;

import net.sf.l2j.loginserver.GameServerTable;

/**
 *
 * @author  KenM
 */
@SuppressWarnings("serial")
public class RegisterDialog extends JDialog implements ActionListener
{
	
	private ResourceBundle _bundle;
	@SuppressWarnings("rawtypes")
	private final JComboBox _combo;
	private final GUserInterface _owner;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public RegisterDialog(final GUserInterface owner)
	{
		super(owner.getFrame(), true);
		_owner = owner;
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		_bundle = owner.getBundle();
		this.setTitle(_bundle.getString("registerGS"));
		this.setResizable(false);
		this.setLayout(new GridBagLayout());
		GridBagConstraints cons = new GridBagConstraints();
		cons.weightx = 0.5;
		cons.weighty = 0.5;
		cons.gridx = 0;
		cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;
		
		final JLabel label = new JLabel(_bundle.getString("serverName"));
		this.add(label, cons);
		
		_combo = new JComboBox();
		_combo.setEditable(false);
		for (Map.Entry<Integer, String> entry : GameServerTable.getInstance().getServerNames().entrySet())
		{
			if (!GameServerTable.getInstance().hasRegisteredGameServerOnId(entry.getKey()))
			{
				_combo.addItem(new ComboServer(entry.getKey(), entry.getValue()));
			}
		}
		cons.gridx = 1;
		cons.gridy = 0;
		this.add(_combo, cons);
		
		cons.gridx = 0;
		cons.gridy = 1;
		cons.gridwidth = 2;
		JTextPane textPane = new JTextPane();
		textPane.setText(_bundle.getString("saveHexId"));
		textPane.setEditable(false);
		textPane.setBackground(label.getBackground());
		this.add(textPane, cons);
		cons.gridwidth = 1;
		
		JButton btnSave = new JButton(_bundle.getString("save"));
		btnSave.setActionCommand("save");
		btnSave.addActionListener(this);
		cons.gridx = 0;
		cons.gridy = 2;
		this.add(btnSave, cons);
		
		JButton btnCancel = new JButton(_bundle.getString("cancel"));
		btnCancel.setActionCommand("cancel");
		btnCancel.addActionListener(this);
		cons.gridx = 1;
		cons.gridy = 2;
		this.add(btnCancel, cons);
		
		final double leftSize = Math.max(label.getPreferredSize().getWidth(), btnSave.getPreferredSize().getWidth());
		final double rightSize = Math.max(_combo.getPreferredSize().getWidth(), btnCancel.getPreferredSize().getWidth());
		
		final double height = _combo.getPreferredSize().getHeight()+ 4 * textPane.getPreferredSize().getHeight()+btnSave.getPreferredSize().getHeight();
		this.setSize((int) (leftSize + rightSize + 30), (int) (height + 20));
		
		this.setLocationRelativeTo(owner.getFrame());
	}
	
	class ComboServer
	{
		private final int _id;
		private final String _name;

		public ComboServer(int id, String name)
		{
			_id = id;
			_name = name;
		}

		/**
         * @return Returns the id.
         */
        public int getId()
        {
	        return _id;
        }

		/**
         * @return Returns the name.
         */
        public String getName()
        {
	        return _name;
        }

		@Override
        public String toString()
        {
	        return this.getName();
        }
	}

	/**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
	    String cmd = e.getActionCommand();
	    
	    if (cmd.equals("save"))
	    {
	    	ComboServer server = (ComboServer) _combo.getSelectedItem();
	    	int gsId = server.getId();
	    	
	    	JFileChooser fc = new JFileChooser();
	    	//fc.setS
	    	fc.setDialogTitle(_bundle.getString("hexidDest"));
	        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    	fc.setFileFilter
	    	(
	    		new FileFilter()
	    		{

					@Override
                    public boolean accept(File f)
                    {
	                    return f.isDirectory();
                    }

					@Override
                    public String getDescription()
                    {
	                    return null;
                    }
	    			
	    		}
	    	);
	    	fc.showOpenDialog(this);
	    	
	    	try
            {
	            GUserInterface.registerGameServer(gsId, fc.getSelectedFile().getAbsolutePath());
	            _owner.refreshAsync();
	            this.setVisible(false);
            }
            catch (IOException e1)
            {
	            _owner.showError(_bundle.getString("ioErrorRegister"), e1);
            }
	    }
	    else if (cmd.equals("cancel"))
	    {
	    	this.setVisible(false);
	    }
    }
}
