/**
 * 
 */
package cz.nxs.interf.callback.api;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.interf.callback.api.descriptions.CTFDescription;
import cz.nxs.interf.callback.api.descriptions.ChestsDescription;
import cz.nxs.interf.callback.api.descriptions.DMDescription;
import cz.nxs.interf.callback.api.descriptions.DominationDescription;
import cz.nxs.interf.callback.api.descriptions.KoreanDescription;
import cz.nxs.interf.callback.api.descriptions.LMSDescription;
import cz.nxs.interf.callback.api.descriptions.MassDominationDescription;
import cz.nxs.interf.callback.api.descriptions.MiniTvTDescription;
import cz.nxs.interf.callback.api.descriptions.PartyFightsDescription;
import cz.nxs.interf.callback.api.descriptions.SinglePlayersFightsDescription;
import cz.nxs.interf.callback.api.descriptions.TvTAdvancedDescription;
import cz.nxs.interf.callback.api.descriptions.TvTDescription;

/**
 * @author hNoke
 *
 */
public class DescriptionLoader
{
	public static void load()
	{
		EventDescriptionSystem.getInstance().addDescription(EventType.TvT, new TvTDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.TvTAdv, new TvTAdvancedDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.CTF, new CTFDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.Domination, new DominationDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.MassDomination, new MassDominationDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.DM, new DMDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.LastMan, new LMSDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.LuckyChests, new ChestsDescription());
		
		EventDescriptionSystem.getInstance().addDescription(EventType.Classic_1v1, new SinglePlayersFightsDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.PartyvsParty, new PartyFightsDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.Korean, new KoreanDescription());
		EventDescriptionSystem.getInstance().addDescription(EventType.MiniTvT, new MiniTvTDescription());
	}
}
