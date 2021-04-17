package luna.custom.season;

public class SeasonRewardData
{
		private final String _rank;
		private final int _itemId;
		private final int _ammount;
		
		public SeasonRewardData (String rank, int itemId, int ammount)
		{
			_rank = rank;
			_itemId = itemId;
			_ammount = ammount;
			
		}

		public String get_rank()
		{
			return _rank;
		}

		public int get_itemId()
		{
			return _itemId;
		}

		public int get_ammount()
		{
			return _ammount;
		}

}
