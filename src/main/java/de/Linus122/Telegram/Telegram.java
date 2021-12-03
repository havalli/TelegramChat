package de.Linus122.Telegram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.Linus122.TelegramComponents.*;
import de.Linus122.TelegramChat.TelegramChat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

public class Telegram {
	public JsonObject authJson;
	public boolean connected = false;

	static int lastUpdate = -2;
	public String token;

	private List<TelegramActionListener> listeners = new ArrayList<TelegramActionListener>();

	private final String API_URL_GETME = "https://api.telegram.org/bot%s/getMe";
	private final String API_URL_GETUPDATES = "https://api.telegram.org/bot%s/getUpdates?offset=%d";
	private final String API_URL_GENERAL = "https://api.telegram.org/bot%s/%s";

	private Gson gson = new Gson();

	public void addListener(TelegramActionListener actionListener) {
		listeners.add(actionListener);
	}

	public boolean auth(String token) {
		this.token = token;
		return reconnect();
	}

	public boolean reconnect() {
		try {
			JsonObject obj = sendGet(String.format(API_URL_GETME, token));
			authJson = obj;
			System.out.print("[Telegram] Established a connection with the telegram servers.");
			connected = true;
			return true;
		} catch (Exception e) {
			connected = false;
			System.out.print("[Telegram] Sorry, but could not connect to Telegram servers. The token could be wrong.");
			return false;
		}
	}

	public boolean getUpdate() {
		JsonObject up = null;
		try {
			up = sendGet(String.format(API_URL_GETUPDATES, TelegramChat.getBackend().getToken(), lastUpdate + 1));
		} catch (IOException e) {
			return false;
		}
		if (up == null) {
			return false;
		}
		if (up.has("result")) {
			for (JsonElement ob : up.getAsJsonArray("result")) {
				if (ob.isJsonObject()) {
					Update update = gson.fromJson(ob, Update.class);
		
					if(lastUpdate == update.getUpdate_id())
						return true;
					lastUpdate = update.getUpdate_id();

					if (update.getMessage() != null) {
						Chat chat = update.getMessage().getChat();

						if (chat.isPrivate()) {
							// private chat
							if (!TelegramChat.getBackend().chat_ids.contains(chat.getId()))
								TelegramChat.getBackend().chat_ids.add(chat.getId());

							if (update.getMessage().getText() != null) {
								String text = update.getMessage().getText();
								if (text.length() == 0)
									return true;
								if (text.equals("/start")) {
									if (TelegramChat.getBackend().isFirstUse()) {
										TelegramChat.getBackend().setFirstUse(false);
										ChatMessageToTelegram chat2 = new ChatMessageToTelegram();
										chat2.chat_id = chat.getId();
										chat2.parse_mode = "Markdown";
										chat2.text = Utils.formatMSG("setup-msg")[0];
										this.sendMsg(chat2);
									}
									this.sendMsg(chat.getId(), Utils.formatMSG("can-see-but-not-chat")[0]);
								} else {
									handleUserMessage(text, update);
								}
							}

						} else if (!chat.isPrivate()) {
							// group chat
							int id = chat.getId();
							if (!TelegramChat.getBackend().chat_ids.contains(id))
								TelegramChat.getBackend().chat_ids.add(id);
							
							if (update.getMessage().getText() != null) {
								String text = update.getMessage().getText();
								handleUserMessage(text, update);
							}
						}
					}

				}
			}
		}
		return true;
	}
	
	public void handleUserMessage(String telegramChatText, Update update) {
		Chat telegramChat = update.getMessage().getChat();
		User user = update.getMessage().getFrom();
		int telegram_user_id = user.getId();
		if (TelegramChat.getBackend().getLinkCodes().containsKey(telegramChatText)) {
			// LINK
			TelegramChat.link(TelegramChat.getBackend().getUUIDFromLinkCode(telegramChatText), telegram_user_id);
			TelegramChat.getBackend().removeLinkCode(telegramChatText);
			TelegramChat.save();
		} else if (TelegramChat.getBackend().getLinkedChats().containsKey(telegram_user_id)) {

			if(telegramChatText.startsWith("/werda")){
				sendWerdaSatistics(telegramChat);
			}
			else if(telegramChatText.startsWith("/berghoch")){
				sendBerchHochSatistics(telegramChat);
			}
			else if(telegramChatText.startsWith("/suchti")){
				sendSuchtiSatistics(telegramChat);
			}
			else if(telegramChatText.startsWith("/hebamme")){
				sendHebameSatistics(telegramChat);
			}
			else {
				ChatMessageToMc chatMsg = new ChatMessageToMc(
						TelegramChat.getBackend().getUUIDFromUserID(telegram_user_id), telegramChatText, telegramChat.getId());

				for (TelegramActionListener actionListener : listeners) {
					actionListener.onSendToMinecraft(chatMsg);
				}

				if(!chatMsg.isCancelled()){
					TelegramChat.sendToMC(chatMsg);
				}
			}
		} else {
			Bukkit.getLogger().log(Level.INFO, "Player '" + user.getUsername() + "' with id '" + user.getId() + "' not linked");
			this.sendMsg(telegramChat.getId(), Utils.formatMSG("need-to-link")[0]);
		}
	}

	public void sendHebameSatistics(Chat telegramChat){

		OfflinePlayer[] players = Bukkit.getOfflinePlayers();
		Map<Integer, String> map = new HashMap<>();


		for (OfflinePlayer player: players) {
			int statistic = player.getStatistic(Statistic.ANIMALS_BRED);
			map.put(statistic, player.getName());

		}

		Map<Integer, String> treeMap = new TreeMap<>(Collections.reverseOrder());
		treeMap.putAll(map);
		Set<Map.Entry<Integer, String>> entries = treeMap.entrySet();

		StringBuilder report = new StringBuilder();
		entries.forEach( entry -> {
			report
					.append(entry.getValue())
					.append(", ")
					.append(entry.getKey())
					.append(" Tiere Zur Welt gebracht")
					.append("\r\n");
		});
		this.sendMsg(telegramChat.getId(), report.toString());
	}

	public void sendWerdaSatistics(Chat telegramChat){

		StringBuilder report = new StringBuilder();
		OfflinePlayer[] players = Bukkit.getOfflinePlayers();
		boolean atLeastOnePlayerOnline = false;
		for (OfflinePlayer player: players) {
			if(player.isOnline()) {
				atLeastOnePlayerOnline = true;
				//Date lastLogin = new Date(player.getLastPlayed());
				//Instant onlineSince = Instant.now().minusMillis(player.getLastPlayed());
				//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
				report
						.append(player.getName())
						//.append(" online seid: ")
						//.append(formatter.format(onlineSince))
						.append("\r\n");
			}
		}
		if(atLeastOnePlayerOnline){
			this.sendMsg(telegramChat.getId(), report.toString());
		}
		else {
			this.sendMsg(telegramChat.getId(), "Keiner da");
		}
	}

	public void sendBerchHochSatistics(Chat telegramChat){

		OfflinePlayer[] players = Bukkit.getOfflinePlayers();
		Map<Integer, String> map = new HashMap<>();

		for (OfflinePlayer player: players) {
			int statistic = player.getStatistic(Statistic.DEATHS);
			map.put(statistic, player.getName());

		}

		Map<Integer, String> treeMap = new TreeMap<>(Collections.reverseOrder());
		treeMap.putAll(map);
		Set<Map.Entry<Integer, String>> entries = treeMap.entrySet();

		StringBuilder report = new StringBuilder();
		entries.forEach( entry -> {
			report
					.append(entry.getValue())
					.append(", ")
					.append(entry.getKey())
					.append(" mal gestorben")
					.append("\r\n");
		});
		this.sendMsg(telegramChat.getId(), report.toString());
	}

	public void sendSuchtiSatistics(Chat telegramChat){

		OfflinePlayer[] players = Bukkit.getOfflinePlayers();
		Map<Integer, String> map = new HashMap<>();

		for (OfflinePlayer player: players) {
			int statistic = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
			map.put(statistic / 20 / 60 / 60, player.getName());

		}

		Map<Integer, String> treeMap = new TreeMap<>(Collections.reverseOrder());
		treeMap.putAll(map);
		Set<Map.Entry<Integer, String>> entries = treeMap.entrySet();

		StringBuilder report = new StringBuilder();
		entries.forEach( entry -> {
			report
					.append(entry.getValue())
					.append(", ")
					.append(entry.getKey())
					.append("h gezockt")
					.append("\r\n");
		});
		this.sendMsg(telegramChat.getId(), report.toString());
	}

	public void sendMsg(int id, String msg) {
		ChatMessageToTelegram chat = new ChatMessageToTelegram();
		chat.chat_id = id;
		chat.text = msg;
		sendMsg(chat);
	}

	public void sendMsg(ChatMessageToTelegram chat) {
		for (TelegramActionListener actionListener : listeners) {
			actionListener.onSendToTelegram(chat);
		}
		Gson gson = new Gson();
		if(!chat.isCancelled()){
			post("sendMessage", chat);
		}
	}

	public void sendAll(final ChatMessageToTelegram chat) {
		new Thread(new Runnable() {
			public void run() {
				for (int id : TelegramChat.getBackend().chat_ids) {
					chat.chat_id = id;
					// post("sendMessage", gson.toJson(chat, Chat.class));
					sendMsg(chat);
				}
			}
		}).start();
	}

	public void post(String method, ChatMessageToTelegram chat) {
		try {
			String json = gson.toJson(chat, ChatMessageToTelegram.class);
			URL url = new URL(String.format(API_URL_GENERAL, TelegramChat.getBackend().getToken(), method));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/json; ; Charset=UTF-8");
			connection.setRequestProperty("Content-Length", String.valueOf(json.length()));

			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
			writer.write(json);
			writer.close();
			wr.close();

			if(connection.getResponseCode() == 403){
				Bukkit.getLogger().log(Level.WARNING,"Failed to send Telegram message to this chat: " + chat.chat_id);
				Object chatId = chat.chat_id;
				Integer chatIdIndex = TelegramChat.getBackend().chat_ids.indexOf(chatId);
				TelegramChat.getBackend().chat_ids.remove(chatIdIndex);
				TelegramChat.save();
				return;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			reader.close();
		} catch (Exception e) {
			reconnect();
			Bukkit.getLogger().log(Level.WARNING,"Failed to send Telegram message", e);
			System.out.print("[Telegram] Disconnected from Telegram, reconnect...");
		}

	}

	public JsonObject sendGet(String url) throws IOException {
		String a = url;
		URL url2 = new URL(a);
		URLConnection conn = url2.openConnection();

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

		String all = "";
		String inputLine;
		while ((inputLine = br.readLine()) != null) {
			all += inputLine;
		}

		br.close();
		JsonParser parser = new JsonParser();
		return parser.parse(all).getAsJsonObject();

	}

}
