package net.sf.l2j.gsregistering;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.sf.l2j.images.ImagesTable;
import net.sf.l2j.loginserver.GameServerTable;

/**
 * @author KenM
 *
 */
public class GUserInterface extends BaseGameServerRegister implements ActionListener
{
	
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	private final JFrame _frame;

	
	JTableModel _dtm;
	JProgressBar _progressBar;

	public JTable _gsTable;
	
	public GUserInterface(ResourceBundle bundle)
	{
		super(bundle);

		_frame = new JFrame();
		getFrame().setTitle(getBundle().getString("toolName"));
		getFrame().setSize(600, 400);
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getFrame().setLayout(new GridBagLayout());
		GridBagConstraints cons = new GridBagConstraints();
		
		JMenuBar menubar = new JMenuBar();
		getFrame().setJMenuBar(menubar);
		
		JMenu fileMenu = new JMenu(getBundle().getString("fileMenu"));
		
		JMenuItem exitItem = new JMenuItem(getBundle().getString("exitItem"));
		exitItem.addActionListener(this);
		exitItem.setActionCommand("exit");
		
		fileMenu.add(exitItem);
		
		JMenu helpMenu = new JMenu(getBundle().getString("helpMenu"));
		
		JMenuItem aboutItem = new JMenuItem(getBundle().getString("aboutItem"));
		aboutItem.addActionListener(this);
		aboutItem.setActionCommand("about");
		
		helpMenu.add(aboutItem);
		
		menubar.add(fileMenu);
		menubar.add(helpMenu);
		
		JButton btnRegister = new JButton(getBundle().getString("btnRegister"), ImagesTable.getImage("add.png"));
		btnRegister.addActionListener(this);
		btnRegister.setActionCommand("register");
		getFrame().add(btnRegister, cons);
		
		cons.gridx = 1;
		cons.anchor = GridBagConstraints.LINE_END;
		JButton btnRemoveAll = new JButton(getBundle().getString("btnRemoveAll"), ImagesTable.getImage("cross.png"));
		btnRemoveAll.addActionListener(this);
		btnRemoveAll.setActionCommand("removeAll");
		getFrame().add(btnRemoveAll, cons);
		
		String name = getBundle().getString("gsName");
		String action = getBundle().getString("gsAction");

		_dtm = new JTableModel(new Object[] { "ID", name, action });
		_gsTable = new JTable(_dtm);
		_gsTable.addMouseListener(new JTableButtonMouseListener(_gsTable));
		
		_gsTable.getColumnModel().getColumn(0).setMaxWidth(30);
		
		TableColumn actionCollumn = _gsTable.getColumnModel().getColumn(2);
		actionCollumn.setCellRenderer(new ButtonCellRenderer());
		
		cons.fill = GridBagConstraints.BOTH;
		cons.gridx = 0;
		cons.gridy = 1;
		cons.weighty = 1.0;
		cons.weightx = 1.0;
		cons.gridwidth = 2;
		JLayeredPane layer = new JLayeredPane();
		layer.setLayout(new BoxLayout(layer, BoxLayout.PAGE_AXIS));
		layer.add(new JScrollPane(_gsTable), 0);
		_progressBar = new JProgressBar();
		_progressBar.setIndeterminate(true);
		_progressBar.setVisible(false);
		layer.add(_progressBar, BorderLayout.CENTER, 1);
		//layer.setV
		getFrame().add(layer, cons);
		
		
		// maximize, doesn't seem really needed 
		//getFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
		/*
		// Work-around JVM maximize issue on linux
		String osName = System.getProperty("os.name");
		if (osName.equals("Linux"))
		{
		   Toolkit toolkit = Toolkit.getDefaultToolkit();
		   Dimension screenSize = toolkit.getScreenSize();
		   getFrame().setSize(screenSize);
		}
		*/
		this.refreshAsync();
	}
	
	public void refreshAsync()
	{
		Runnable r = new Runnable()
		{
			@Override
            public void run()
            {
				GUserInterface.this.refreshServers();
            }
		};
		Thread t = new Thread(r, "LoaderThread");
		t.start();
	}
	
	@Override
    public void load()
    {
		
		SwingUtilities.invokeLater
		(
				new Runnable()
				{
					@Override
                    public void run()
                    {
						_progressBar.setVisible(true);
                    }
				}
		);
	    
	    super.load();
	    
	    SwingUtilities.invokeLater
		(
				new Runnable()
				{
					@Override
                    public void run()
                    {
						_progressBar.setVisible(false);
                    }
				}
		);
    }


	/**
     * @see net.sf.l2j.gsregistering.BaseGameServerRegister#showError(java.lang.String, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void showError(String msg, Throwable t)
    {
    	String title;
    	if (this.getBundle() != null)
    	{
    		title = this.getBundle().getString("error");
    		msg += '\n'+this.getBundle().getString("reason")+' '+t.getLocalizedMessage();
    	}
    	else
    	{
    		title = "Error";
    		msg += "\nCause: "+t.getLocalizedMessage();
    	}
	    JOptionPane.showMessageDialog(this.getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
    }

	int i = 0;
	protected void refreshServers()
	{
		if (!this.isLoaded())
		{
			this.load();
		}
		
		// load succeeded?
		if (this.isLoaded())
		{
			SwingUtilities.invokeLater
			(
					new Runnable()
					{
						@Override
						public void run()
						{
							int size = GameServerTable.getInstance().getServerNames().size();
							if (size == 0)
							{
						    	String title = getBundle().getString("error");
								String msg = getBundle().getString("noServerNames"); 
								JOptionPane.showMessageDialog(getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
								System.exit(1);
							}
							// reset
							_dtm.setRowCount(0);
							
							for (final int id : GameServerTable.getInstance().getRegisteredGameServers().keySet())
							{
								String name = GameServerTable.getInstance().getServerNameById(id);
								JButton button = new JButton(getBundle().getString("btnRemove"), ImagesTable.getImage("cross.png"));
								button.addActionListener
								(
										new ActionListener()
										{
											@Override
											public void actionPerformed(ActionEvent e)
											{
												String sid = String.valueOf(id);
												String sname = GameServerTable.getInstance().getServerNameById(id);
												
												int choice = JOptionPane.showConfirmDialog(getFrame(), getBundle().getString("confirmRemoveText").replace("%d", sid).replace("%s",sname), getBundle().getString("confirmRemoveTitle"), JOptionPane.YES_NO_OPTION);
												if (choice == JOptionPane.YES_OPTION)
												{
													try
													{
														BaseGameServerRegister.unregisterGameServer(id);
														GUserInterface.this.refreshAsync();
													}
													catch (SQLException e1)
													{
														GUserInterface.this.showError(getBundle().getString("errorUnregister"), e1);
													}
												}
											}
										}
								);
								_dtm.addRow(new Object[] { id, name, button });
							}
						}
					}
			);
		}
	}

	

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		
		if (cmd.equals("register"))
		{
			RegisterDialog rd = new RegisterDialog(this);
			rd.setVisible(true);
		}
		else if (cmd.equals("exit"))
		{
			System.exit(0);
		}
		else if (cmd.equals("about"))
		{
			JOptionPane.showMessageDialog(getFrame(), getBundle().getString("credits") + "\nhttp://www.l2jserver.com\n\n"+getBundle().getString("icons")+"\n\n"+getBundle().getString("language")+'\n'+getBundle().getString("translation"), getBundle().getString("aboutItem"), JOptionPane.INFORMATION_MESSAGE, ImagesTable.getImage("l2jserverlogo.png"));
		}
		else if (cmd.equals("removeAll"))
		{
			int choice = JOptionPane.showConfirmDialog(getFrame(), getBundle().getString("confirmRemoveAllText"), getBundle().getString("confirmRemoveTitle"), JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION)
			{
				try
                {
	                BaseGameServerRegister.unregisterAllGameServers();
	                this.refreshAsync();
                }
                catch (SQLException e1)
                {
                	GUserInterface.this.showError(getBundle().getString("errorUnregister"), e1);
                }
			}
		}
	}

	/**
     * @return Returns the frame.
     */
    public JFrame getFrame()
    {
	    return _frame;
    }

	class ButtonCellRenderer implements TableCellRenderer
	{

		/* (non-Javadoc)
		 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			return (Component) value;
		}
		
	}
	
	/**
	 * Forward mouse-events from table to buttons inside.
	 * Buttons animate properly.
	 *
	 * @author  KenM
	 */
	class JTableButtonMouseListener implements MouseListener
	{
		private final JTable _table;
		
		public JTableButtonMouseListener(JTable table)
		{
			_table = table;
		}
		
		private void forwardEvent(MouseEvent e)
	    {
	        TableColumnModel columnModel = _table.getColumnModel();
	        int column = columnModel.getColumnIndexAtX(e.getX());
	        int row    = e.getY() / _table.getRowHeight();
	        Object value;

	        if (row >= _table.getRowCount() || row < 0 || column >= _table.getColumnCount() || column < 0)
	        {
	            return;
	        }

	        value = _table.getValueAt(row, column);
	        
	        if (value instanceof JButton)
	        {
	            final JButton b = (JButton) value;
	            if (e.getID() == MouseEvent.MOUSE_PRESSED)
	            {
	            	b.getModel().setPressed(true);
					b.getModel().setArmed(true);
					_table.repaint();
	            }
	            else if (e.getID() == MouseEvent.MOUSE_RELEASED)
	            {
	            	b.doClick();
	            }
	        }
	    }

	    public void mouseEntered(MouseEvent e)
	    {
	        forwardEvent(e);
	    }

	    public void mouseExited(MouseEvent e)
	    {
	        forwardEvent(e);
	    }

	    public void mousePressed(MouseEvent e)
	    {
	        forwardEvent(e);
	    }

	    public void mouseClicked(MouseEvent e)
	    {
	    	forwardEvent(e);
	    }
	    
	    public void mouseReleased(MouseEvent e)
	    {
	    	forwardEvent(e);
	    }
	}
	
	@SuppressWarnings("serial")
	class JTableModel extends DefaultTableModel
	{
		public JTableModel(Object[] columnNames)
		{
			super(columnNames, 0);
		}
		
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
		
		@Override
		public Class<?> getColumnClass(int column)
		{
			return getValueAt(0, column).getClass();
		}
	}
}
