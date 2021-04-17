package luna.custom.handler.items.lootBox;

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

import net.sf.l2j.util.Rnd;

public final class LootData
{
	public Map<Integer, List<GainData>>	_loot_data	= new HashMap<>();
	private String						_file_name	= "data/xml/items/LootBoxes.xml";
	
	private LootData()
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
							String enc = "";
							boolean proEnch = false;
							int proEnchRate = 33;
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							//
							if (!_loot_data.containsKey(id))
							{
								_loot_data.put(id, new ArrayList<>());
							}
							//
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								if ("reward".equalsIgnoreCase(att.getNodeName()))
								{
									int itemId = Integer.parseInt(att.getAttributes().getNamedItem("itemId").getNodeValue());
									int itemAmmount = Integer.parseInt(att.getAttributes().getNamedItem("ammount").getNodeValue());
									int itemChance = Integer.parseInt(att.getAttributes().getNamedItem("chance").getNodeValue());
									Node enchant = att.getAttributes().getNamedItem("enchant");
									if (enchant != null)
									{
										enc = enchant.getNodeValue();
									}
									if (att.getNodeName().equalsIgnoreCase("proEnchant"))
									{
										String proEnchStr = att.getAttributes().getNamedItem("proEnchant").getNodeValue();
										if (proEnchStr.equalsIgnoreCase("true"))
										{
											proEnch = true;
										}
										else
										{
											proEnch = false;
										}
									}
									if(att.getNodeName().equalsIgnoreCase("proEnchRate"))
									{
										proEnchRate = Integer.parseInt(att.getAttributes().getNamedItem("proEnchRate").getNodeValue());
									}
									_loot_data.get(id).add(new GainData(itemId, itemAmmount, itemChance, enc, proEnch, proEnchRate));
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
	
	public void ReloadLootBoxes()
	{
		_loot_data.clear();
		loadXml();
		System.out.println("Loot Boxes Reloaded.");
	}
	
	public List<GainData> getBox(int id)
	{
		return _loot_data.get(id);
	}
	
	public boolean containsBox(int itemId)
	{
		return _loot_data.containsKey(itemId);
	}
	
	public GainData getRandomReward(int id)
	{
		List<GainData> dat = _loot_data.get(id);
		boolean found = false;
		GainData rnd = dat.get(Rnd.get(dat.size()));
		while (!found)
		{
			System.out.println(rnd.getItemId());
			int chance = rnd.getChance();
			rnd.setChanceToDisplay(chance);
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
	
	public class GainData
	{
		private final int		_itemId;
		private final int		_amount;
		private final int		_chance;
		private final int		_enc_min;
		private final int		_enc_max;
		private final boolean	_proEnch;
		private final int		_proEnchRate;
		private int				chancetodisplay;
		private String			titleToDisplay;
		
		public GainData(int itemId, int amount, int chance, String enc, boolean proEnch, int proEnchRate)
		{
			_itemId = itemId;
			_amount = amount;
			_chance = chance;
			_proEnch = proEnch;
			_proEnchRate = proEnchRate;
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
		
		public void setChanceToDisplay(int val)
		{
			this.chancetodisplay = val;
		}
		
		public int getChanceToDisplay()
		{
			return chancetodisplay;
		}
		
		public void setTitleToDisplay(String val)
		{
			this.titleToDisplay = val;
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
		
		public boolean getProEnch()
		{
			return _proEnch;
		}

		public int getProEnchRate()
		{
			return _proEnchRate;
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
		
		public int getProRndEnc()
		{
			int i = _enc_min;
			for (; i <= _enc_max; i++)
			{
				if (Rnd.get(100) >= _chance)
				{
					break;
				}
			}
			return i;
		}
	}
	
	public static LootData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LootData INSTANCE = new LootData();
	}
}
