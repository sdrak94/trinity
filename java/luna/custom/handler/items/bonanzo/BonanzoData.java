package luna.custom.handler.items.bonanzo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowHtml;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.util.Rnd;

public final class BonanzoData
{
	public Map<Integer, List<RewardData>>	_box_data	= new HashMap<>();
	private String							_file_name	= "data/xml/items/BonanzoBoxes.xml";
	public String							_title		= "";
	
	private BonanzoData()
	{
		loadXml();
	}
	
	protected void loadXml()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(_file_name);
		try
		{
			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("box"))
						{
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							//
							if (!_box_data.containsKey(id))
							{
								_box_data.put(id, new ArrayList<>());
							}
							//
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								String name = d.getAttributes().getNamedItem("name").getNodeValue();
								String title = d.getAttributes().getNamedItem("title").getNodeValue();
								if ("reward".equalsIgnoreCase(att.getNodeName()))
								{
									int itemId = Integer.parseInt(att.getAttributes().getNamedItem("itemId").getNodeValue());
									int itemAmmount = Integer.parseInt(att.getAttributes().getNamedItem("ammount").getNodeValue());
									int itemChance = Integer.parseInt(att.getAttributes().getNamedItem("chance").getNodeValue());
									int displayChance = -1;
									String aug = "";
									if ("aug".equalsIgnoreCase(att.getNodeName()))
									{
										aug = att.getAttributes().getNamedItem("aug").getNodeValue();
									}
									Node enchant = att.getAttributes().getNamedItem("enchant");
									String enc = "";
									if (enchant != null)
									{
										enc = enchant.getNodeValue();
									}
									if(att.getAttributes().getNamedItem("chanceDisp") != null)
									{
										displayChance = Integer.parseInt(att.getAttributes().getNamedItem("chanceDisp").getNodeValue());
									}
									//
									//System.out.println(name + " " + id + " " + itemId + " " + itemAmmount + " " + enc + " " + itemChance);
									_box_data.get(id).add(new RewardData(itemId, itemAmmount, itemChance, enc, aug, title, displayChance));
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String getTitle2()
	{
		return _title;
	}
	
	public void ReloadBonanzo()
	{
		_box_data.clear();
		loadXml();
		System.out.println("Bonanzo Boxes Reloaded.");
	}
	
	public List<RewardData> getBox(int id)
	{
		return _box_data.get(id);
	}
	
	public boolean containsBox(int itemId)
	{
		return _box_data.containsKey(itemId);
	}
	
	public RewardData getRandomReward(int id)
	{
		List<RewardData> dat = _box_data.get(id);
		boolean found = false;
		RewardData rnd = dat.get(Rnd.get(dat.size()));
		while (!found)
		{
			//System.out.println(rnd.getItemId());
			int chance = rnd.getChance();
			rnd.setChanceToDisplay(chance);
			rnd.setTitleToDisplay(rnd.getParentName());
			if (chance > Rnd.get(10000))
			{
				found = true;
			}
			else
			{
				rnd = dat.get(Rnd.get(dat.size()));
			}
		}
		return rnd;
	}
	
	public class RewardData
	{
		private final int	_itemId;
		private final int	_amount;
		private final int	_chance;
		private final int	_enc_min;
		private final int	_enc_max;
		private int			chancetodisplay;
		private String		titleToDisplay;
		private String		_aug;
		private String _parentName;
		private final int _chanceDisp;
		
		public RewardData(int itemId, int amount, int chance, String enc, String aug, String parentName, int chanceDisp)
		{
			_itemId = itemId;
			_amount = amount;
			_chance = chance;
			_aug = aug;
			_parentName = parentName;
			_chanceDisp = chanceDisp;
			
			if (!enc.isEmpty())
			{
				_enc_min = Integer.parseInt(enc.split("-")[0]);
				_enc_max = Integer.parseInt(enc.split("-")[1]);
			}
			else
			{
				_enc_min = 0;
				_enc_max = 0;
			}
		}
		public String getParentName()
		{
			return _parentName;
		}
		public void setChanceToDisplay(int val)
		{
			chancetodisplay = val;
		}
		
		public int getChanceToDisplay()
		{
			return chancetodisplay;
		}
		
		public void setTitleToDisplay(String val)
		{
			titleToDisplay = val;
		}
		
		public String getTitleToDisplay()
		{
			return titleToDisplay;
		}
		
		public String getEnc()
		{
			return String.valueOf(_enc_min) + "-" + String.valueOf(_enc_max);
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getAmount()
		{
			return _amount;
		}
		
		public int getChance()
		{
			return _chance;
		}
		
		public int getChanceDisp()
		{
			return _chanceDisp;
		}
		public int getRndEnc()
		{
			if (_enc_max == 0)
			{
				return 0;
			}
			else
			{
				return Rnd.get(_enc_min, _enc_max);
			}
		}
		
		public boolean getAug()
		{
			if (_aug.equalsIgnoreCase("true"))
			{
				return true;
			}
			else
				return false;
		}
	}
	
	public void itemResult(L2Playable playable, int ItemId, int ench, int ammount)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		final L2PcInstance activeChar = (L2PcInstance) playable;
		StringBuilder rewardStr = StringUtil.startAppend(1000, "");
		{
			L2Item reward = ItemTable.getInstance().getTemplate(ItemId);
			String icon = reward.getIcon();
			String name = reward.getName();
			String ammountStr = "";
			String enchantStr = "";
			if (ench > 0)
			{
				enchantStr = "+" + String.valueOf(ench);
			}
			if (ammount > 1)
			{
				ammountStr = "(x" + ammount + ")";
			}
			else
			{
				ammountStr = enchantStr;
			}
			StringUtil.append(rewardStr, "<table width=400 >" + "               <tr>" + "                    <td>" + "							<font name=\"hs15\" color=\"LEVEL\">CONGRATULATIONS YOU HAVE EARNED:</font>" + "</td>" + "</tr>" + "<tr>" + "        <td width=44 height=136 align=center>" + "            <table cellpadding=6 cellspacing=-5>" + "                <tr>" + "                    <td>" + "                        <button width=32 height=32 back=" + icon + " fore=" + icon + ">" + "                    </td>" + "                </tr>" + "            </table>" + "        </td>" + "        <td width=220 align=left><font color=>" + ammountStr + "  " + name + "</font> " + "            <br>" + "		</td>" + "    </tr></table>");
		}
		final String filename = "data/html/custom/Bonanzo/bonanzo_result.htm";
		final String content = HtmCache.getInstance().getHtm(filename);
		// NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		// itemReply.setFile(filename);
		// itemReply.replace("%rewards%", rewardStr.toString());
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			String fp = content;
			activeChar.sendPacket(new TutorialShowHtml(fp));
		}
		else
		{
			L2Item reward = ItemTable.getInstance().getTemplate(ItemId);
			String icon = reward.getIcon();
			String name = reward.getName();
			String ammountStr = "";
			if (ammount > 1)
			{
				ammountStr = "(x" + ammount + ")";
			}
			else
			{
				ammountStr = "+" + ench;
			}
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			String rewards = rewardStr.toString();
			// String itemId = String.valueOf(item.getItemId());
			itemReply.replace("%title%", "" + ItemTable.getInstance().getTemplate(ItemId).getName());
			itemReply.replace("%icon%", reward.getIcon());
			// itemReply.replace("%id%", "" + item.getItemId());
			itemReply.replace("%ammountStr%", ammountStr);
			itemReply.replace("%name%", name);
			String qsb = itemReply.getText();
			activeChar.sendPacket(new TutorialShowHtml(qsb));
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public static BonanzoData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BonanzoData INSTANCE = new BonanzoData();
	}
}
