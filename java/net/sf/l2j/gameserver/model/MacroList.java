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
package net.sf.l2j.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Macro.L2MacroCmd;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SendMacroList;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.2 $ $Date: 2005/03/02 15:38:41 $
 */
public class MacroList
{
    private static Logger _log = Logger.getLogger(MacroList.class.getName());

    private L2PcInstance _owner;
	private int _revision;
	private int _macroId;
    private Map<Integer, L2Macro> _macroses = new FastMap<Integer, L2Macro>();

    public MacroList(L2PcInstance owner)
    {
        _owner = owner;
		_revision = 1;
		_macroId = 1000;
    }

	public int getRevision() {
		return _revision;
	}

    public L2Macro[] getAllMacroses()
    {
		return _macroses.values().toArray(new L2Macro[_macroses.size()]);
    }

    public L2Macro getMacro(int id)
    {
        return _macroses.get(id-1);
    }

    public void registerMacro(L2Macro macro)
    {
		if (macro.id == 0) {
			macro.id = _macroId++;
			while (_macroses.get(macro.id) != null)
				macro.id = _macroId++;
			_macroses.put(macro.id, macro);
			registerMacroInDb(macro);
		} else {
			L2Macro old = _macroses.put(macro.id, macro);
			if (old != null)
				deleteMacroFromDb(old);
			registerMacroInDb(macro);
		}
		sendUpdate();
    }

    public void deleteMacro(int id)
    {
        L2Macro toRemove = _macroses.get(id);
        if(toRemove != null)
        {
            deleteMacroFromDb(toRemove);
        }
		_macroses.remove(id);

		L2ShortCut[] allShortCuts = _owner.getAllShortCuts();
		for (L2ShortCut sc : allShortCuts) {
			if (sc.getId() == id && sc.getType() == L2ShortCut.TYPE_MACRO)
				_owner.deleteShortCut(sc.getSlot(), sc.getPage());
		}

		sendUpdate();
    }

	public void sendUpdate() {
		_revision++;
		L2Macro[] all = getAllMacroses();
		if (all.length == 0) {
			_owner.sendPacket(new SendMacroList(_revision, all.length, null));
		} else {
			for (L2Macro m : all) {
				_owner.sendPacket(new SendMacroList(_revision, all.length, m));
			}
		}
	}

    private void registerMacroInDb(L2Macro macro)
    {
        Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();

            PreparedStatement statement = con.prepareStatement("INSERT INTO character_macroses (charId,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)");
			statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, macro.id);
            statement.setInt(3, macro.icon);
            statement.setString(4, macro.name);
            statement.setString(5, macro.descr);
            statement.setString(6, macro.acronym);
            final StringBuilder sb = new StringBuilder(300);
            for (L2MacroCmd cmd : macro.commands) {
                StringUtil.append(sb,
                        String.valueOf(cmd.type),
                        ",",
                        String.valueOf(cmd.d1),
                        ",",
                        String.valueOf(cmd.d2)
                        );

                if (cmd.cmd != null && cmd.cmd.length() > 0) {
                    StringUtil.append(sb, ",", cmd.cmd);
                }

                sb.append(';');
            }

            if (sb.length() > 255) {
                sb.setLength(255);
            }
            
            statement.setString(7, sb.toString());
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not store macro:", e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }

    /**
     * @param shortcut
     */
    private void deleteMacroFromDb(L2Macro macro)
    {
        Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();

            PreparedStatement statement = con.prepareStatement("DELETE FROM character_macroses WHERE charId=? AND id=?");
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, macro.id);
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not delete macro:", e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }

    public void restore()
    {
		_macroses.clear();
        Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT charId, id, icon, name, descr, acronym, commands FROM character_macroses WHERE charId=?");
            statement.setInt(1, _owner.getObjectId());
            ResultSet rset = statement.executeQuery();
            while (rset.next())
            {
				int id = rset.getInt("id");
                int icon = rset.getInt("icon");
                String name = rset.getString("name");
                String descr = rset.getString("descr");
                String acronym = rset.getString("acronym");
				List<L2MacroCmd> commands = new FastList<L2MacroCmd>();
				StringTokenizer st1 = new StringTokenizer(rset.getString("commands"),";");
				while (st1.hasMoreTokens()) {
					StringTokenizer st = new StringTokenizer(st1.nextToken(),",");
					if(st.countTokens() < 3)
						continue;
					int type = Integer.parseInt(st.nextToken());
					int d1 = Integer.parseInt(st.nextToken());
					int d2 = Integer.parseInt(st.nextToken());
					String cmd = "";
					if (st.hasMoreTokens())
						cmd = st.nextToken();
					L2MacroCmd mcmd = new L2MacroCmd(commands.size(), type, d1, d2, cmd);
					commands.add(mcmd);
				}

				L2Macro m = new L2Macro(id, icon, name, descr, acronym, commands.toArray(new L2MacroCmd[commands.size()]));
				_macroses.put(m.id, m);
            }
            rset.close();
            statement.close();
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not store shortcuts:", e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }
}
