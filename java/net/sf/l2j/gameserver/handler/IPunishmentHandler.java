/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.l2j.gameserver.model.base.punishment.PunishmentTask;
import net.sf.l2j.gameserver.model.base.punishment.PunishmentType;

/**
 * @author UnAfraid
 */
public interface IPunishmentHandler
{
	static final Logger _log = LoggerFactory.getLogger(IPunishmentHandler.class);
	
	public void onStart(PunishmentTask task);
	
	public void onEnd(PunishmentTask task);
	
	public PunishmentType getType();
}
