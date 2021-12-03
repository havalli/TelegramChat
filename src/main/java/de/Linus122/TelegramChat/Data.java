package de.Linus122.TelegramChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Data {
	private String token = "";
	
	// User ID : Player ID
	private HashMap<Integer, UUID> linkedChats = new HashMap<Integer, UUID>();
	
	// Token : Player ID
	private HashMap<String, UUID> linkCodes = new HashMap<String, UUID>();
	
	public ArrayList<Integer> chat_ids = new ArrayList<Integer>();
	
	private boolean firstUse = true;
	

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	// chats 
	public HashMap<Integer, UUID> getLinkedChats() {
		return linkedChats;
	}

	public void setLinkedChats(HashMap<Integer, UUID> linkedChats) {
		this.linkedChats = linkedChats;
	}

	public HashMap<String, UUID> getLinkCodes() {
		return linkCodes;
	}

	public void setLinkCodes(HashMap<String, UUID> linkCodes) {
		this.linkCodes = linkCodes;
	}

	public List<Integer> getIds() {
		return chat_ids;
	}

	public boolean isFirstUse() {
		return firstUse;
	}

	public void setFirstUse(boolean firstUse) {
		this.firstUse = firstUse;
	}

	public void addChatPlayerLink(int chatID, UUID player) {
		linkedChats.put(chatID, player);
	}

	public void addLinkCode(String code, UUID player) {
		linkCodes.put(code, player);
	}

	public UUID getUUIDFromLinkCode(String code) {
		return linkCodes.get(code);
	}

	public void removeLinkCode(String code) {
		linkCodes.remove(code);
	}

	public UUID getUUIDFromUserID(int userID) {
		return linkedChats.get(userID);
	}

}
