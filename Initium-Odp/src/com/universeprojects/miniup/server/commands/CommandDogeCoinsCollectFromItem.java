package com.universeprojects.miniup.server.commands;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.universeprojects.cacheddatastore.CachedDatastoreService;
import com.universeprojects.cacheddatastore.CachedEntity;
import com.universeprojects.miniup.server.GameUtils;
import com.universeprojects.miniup.server.ODPDBAccess;
import com.universeprojects.miniup.server.commands.framework.Command;
import com.universeprojects.miniup.server.commands.framework.UserErrorMessage;
import com.universeprojects.miniup.server.services.MainPageUpdateService;

/**
 * Collects all gold from the specified container object/item ID.
 * 
 * Parameters:
 * 		itemId - Item ID of the container object collecting from 
 * 
 * @author SPFiredrake
 *
 */
public class CommandDogeCoinsCollectFromItem extends Command {

	public CommandDogeCoinsCollectFromItem(ODPDBAccess db, HttpServletRequest request, HttpServletResponse response) {
		super(db, request, response);
	}
	
	@Override
	public void run(Map<String, String> parameters) throws UserErrorMessage {
		ODPDBAccess db = getDB();
		CachedDatastoreService ds = getDS();
		CachedEntity character = db.getCurrentCharacter();
		
		long itemId = tryParseId(parameters, "itemId");
		CachedEntity item = db.getEntity("Item", itemId);
		if(item == null)
			throw new UserErrorMessage("Item does not exist");
		
		// If there are no coins in this container, then we can exit at this point.
		if(item.getProperty("dogecoins").equals(0L))
			return;
		
		CachedEntity itemContainer = db.getEntity((Key)item.getProperty("containerKey"));
		if(itemContainer == null)
			throw new RuntimeException("itemId " + itemId + " does not have a valid container");

		if(db.checkContainerAccessAllowed(character, itemContainer)==false)
			throw new UserErrorMessage("Character does not have access to this container");

		Long characterCoins = (Long)character.getProperty("dogecoins");
		Long containerCoins = (Long)item.getProperty("dogecoins");
		character.setProperty("dogecoins", characterCoins + containerCoins);
		item.setProperty("dogecoins", 0L);
		
		// Order matters! Always subtract coins first!
		ds.put(item);
		ds.put(character);
		
		MainPageUpdateService service = new MainPageUpdateService(db, this);
		service.updateMoney(character);
	}

}
