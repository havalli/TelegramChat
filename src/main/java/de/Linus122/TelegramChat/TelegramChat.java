package de.Linus122.TelegramChat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;

import de.Linus122.Handlers.BanHandler;
import de.Linus122.Handlers.CommandHandler;
import de.Linus122.Metrics.Metrics;
import de.Linus122.Telegram.Telegram;
import de.Linus122.Telegram.Utils;
import de.Linus122.TelegramComponents.Chat;
import de.Linus122.TelegramComponents.ChatMessageToMc;
import de.Linus122.TelegramComponents.ChatMessageToTelegram;

public class TelegramChat extends JavaPlugin implements Listener {
	private static File datad = new File("plugins/TelegramChat/data.json");
	private static FileConfiguration cfg;

	private static Data data = new Data();
	private static Date lastTimeUserQuit = null;
	private static Player playerJustPlacedWitherSkull = null;
	public static Telegram telegramHook;

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		cfg = this.getConfig();
		Utils.cfg = cfg;

		Bukkit.getPluginCommand("telegram").setExecutor(new TelegramCmd());
		Bukkit.getPluginCommand("linktelegram").setExecutor(new LinkTelegramCmd());
		Bukkit.getPluginManager().registerEvents(this, this);

		File dir = new File("plugins/TelegramChat/");
		dir.mkdir();
		data = new Data();
		if (datad.exists()) {
			Gson gson = new Gson();
			try {
				FileReader fileReader = new FileReader(datad);
				StringBuilder sb = new StringBuilder();
				int c;
			    while((c = fileReader.read()) !=-1) {
			    	sb.append((char) c);
			    }

				data = (Data) gson.fromJson(sb.toString(), Data.class);
				
				fileReader.close();
			} catch (Exception e) {
				// old method for loading the data.yml file
				try {
					FileInputStream fin = new FileInputStream(datad);
					ObjectInputStream ois = new ObjectInputStream(fin);
					
					data = (Data) gson.fromJson((String) ois.readObject(), Data.class);
					ois.close();
					fin.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				this.getLogger().log(Level.INFO, "Converted old data.yml");
				save();
			}
		}

		telegramHook = new Telegram();
		telegramHook.auth(data.getToken());
		
		// Ban Handler (Prevents banned players from chatting)
		telegramHook.addListener(new BanHandler());
		
		// Console sender handler, allows players to send console commands (telegram.console permission)
		telegramHook.addListener(new CommandHandler(telegramHook, this));

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			boolean connectionLost = false;
			if (connectionLost) {
				boolean success = telegramHook.reconnect();
				if (success)
					connectionLost = false;
			}
			if (telegramHook.connected) {
				connectionLost = !telegramHook.getUpdate();
			}
		}, 10L, 10L);

		//new Metrics(this);
	}

	@Override
	public void onDisable() {
		save();
	}

	public static void save() {
		Gson gson = new Gson();

		try {
			FileWriter fileWriter = new FileWriter(datad);
			fileWriter.write(gson.toJson(data));
			
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Data getBackend() {
		return data;
	}

	public static void initBackend() {
		data = new Data();
	}

	public static void sendToMC(ChatMessageToMc chatMsg) {
		sendToMC(chatMsg.getUuid_sender(), chatMsg.getContent(), chatMsg.getChatID_sender());
	}

	private static void sendToMC(UUID uuid, String msg, int sender_chat) {
		OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
		List<Integer> recievers = new ArrayList<Integer>();
		recievers.addAll(TelegramChat.data.chat_ids);
		recievers.remove((Object) sender_chat);
		String msgF = Utils.formatMSG("general-message-to-mc", op.getName(), msg)[0];
		for (int id : recievers) {
			telegramHook.sendMsg(id, msgF.replaceAll("§.", ""));
		}
		Bukkit.broadcastMessage(msgF.replace("&", "§"));

	}

	public static void link(UUID player, int userID) {
		TelegramChat.data.addChatPlayerLink(userID, player);
		OfflinePlayer p = Bukkit.getOfflinePlayer(player);
		telegramHook.sendMsg(userID, "Success! Linked " + p.getName());
	}
	
	public boolean isChatLinked(Chat chat) {
		if(TelegramChat.getBackend().getLinkedChats().containsKey(chat.getId())) {
			return true;
		}
		
		return false;
	}

	public static String generateLinkToken() {

		Random rnd = new Random();
		int i = rnd.nextInt(9999999);
		String s = i + "";
		String finals = "";
		for (char m : s.toCharArray()) {
			int m2 = Integer.parseInt(m + "");
			int rndi = rnd.nextInt(2);
			if (rndi == 0) {
				m2 += 97;
				char c = (char) m2;
				finals = finals + c;
			} else {
				finals = finals + m;
			}
		}
		return finals;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (!this.getConfig().getBoolean("enable-joinquitmessages"))
			return;
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils.formatMSG("join-message", e.getPlayer().getName())[0];
			telegramHook.sendAll(chat);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (!this.getConfig().getBoolean("enable-deathmessages"))
			return;
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils.formatMSG("death-message", e.getDeathMessage())[0];
			telegramHook.sendAll(chat);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		lastTimeUserQuit = new Date();
		if (!this.getConfig().getBoolean("enable-joinquitmessages"))
			return;
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils.formatMSG("quit-message", e.getPlayer().getName())[0];
			telegramHook.sendAll(chat);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event){
		Block block = event.getBlockPlaced();
		if(block.getType() == Material.WITHER_SKELETON_SKULL){
			playerJustPlacedWitherSkull = event.getPlayer();
			Bukkit.getLogger().log(Level.INFO, "Wither skull placed by " + playerJustPlacedWitherSkull.getName());
		}
	}

	@EventHandler
	public void onSpawn(CreatureSpawnEvent event) {
		if(event.getEntityType().equals(EntityType.WITHER)){
			Bukkit.getLogger().log(Level.INFO, "Wither spawn.");
			if (playerJustPlacedWitherSkull != null) {
				Bukkit.getLogger().log(Level.INFO, "Wither spawned by " + playerJustPlacedWitherSkull.getName());
				if (telegramHook.connected) {
					ChatMessageToTelegram chat = new ChatMessageToTelegram();
					//chat.parse_mode = "Markdown";
					int x = event.getLocation().getBlockX();
					int y = event.getLocation().getBlockY();
					int z = event.getLocation().getBlockZ();
					chat.text = playerJustPlacedWitherSkull.getName() + " hat Bock auf beef mit nem Wither. Sei kein Lauch, hilf deinem Bro. Stepp rüber nach X="+x+",Y="+y+",Z="+z+" und wams dem Teil ein par rüber.";
					telegramHook.sendAll(chat);
				}
			}
		}
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if (!this.getConfig().getBoolean("enable-chatmessages"))
			return;
		if (e.isCancelled())
			return;
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			chat.parse_mode = "Markdown";
			chat.text = Utils
					.escape(Utils.formatMSG("general-message-to-telegram", e.getPlayer().getName(), e.getMessage())[0])
					.replaceAll("§.", "");
			telegramHook.sendAll(chat);
		}
	}

	@EventHandler
	public void onTimeSkip(TimeSkipEvent e) {
		if (!this.getConfig().getBoolean("enable-chatmessages"))
			return;
		if (e.isCancelled())
			return;
		if(e.getSkipReason() != TimeSkipEvent.SkipReason.NIGHT_SKIP){
			return;
		}
		if(lastTimeUserQuit == null){
			return;
		}
		long diff = new Date().getTime() - lastTimeUserQuit.getTime();
		long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
		if(seconds > 120){
			return;
		}
		if (telegramHook.connected) {
			ChatMessageToTelegram chat = new ChatMessageToTelegram();
			//chat.parse_mode = "Markdown";
			chat.text = "Nacht ist vorbei!";
			telegramHook.sendAll(chat);
		}
	}

}
