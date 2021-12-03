package de.Linus122.Handlers;

import de.Linus122.MessageInterceptingCommandRunner;
import de.Linus122.Telegram.Telegram;
import de.Linus122.Telegram.TelegramActionListener;
import de.Linus122.TelegramComponents.ChatMessageToMc;
import de.Linus122.TelegramComponents.ChatMessageToTelegram;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class CommandHandler extends ConsoleHandler implements TelegramActionListener {

    private Permission permissionsAdapter;
    //private int lastChatId = -1;
    //private long lastCommandTyped;

    private Telegram telegram;
    private Plugin plugin;

    private class TelegramCallback implements MessageInterceptingCommandRunner.ITelegramCallback {

        private int chatId;

        public TelegramCallback(int chatId) {

            this.chatId = chatId;
        }

        @Override
        public void Send(String message) {
            while (message.length() > 3000) {
                String subMessage = message.substring(0, 3000);
                Bukkit.getLogger().log(Level.INFO, "sending response with Textlength: " + subMessage.length());
                telegram.sendMsg(chatId, subMessage);
                message = message.substring(3000);
            }
            Bukkit.getLogger().log(Level.INFO, "sending response with Textlength: " + message.length());
            telegram.sendMsg(chatId, message);
        }
    }

    public CommandHandler(Telegram telegram, Plugin plugin) {
        Bukkit.getLogger().addHandler(this);

        setupVault();

        this.telegram = telegram;
        this.plugin = plugin;
    }

    private void setupVault() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        permissionsAdapter = rsp.getProvider();
    }

    @Override
    public void onSendToTelegram(ChatMessageToTelegram chat) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSendToMinecraft(ChatMessageToMc chatMsg) {

        if (permissionsAdapter == null) {
            // setting up vault permissions
            this.setupVault();
        }

        if (chatMsg.getContent().startsWith("/")) {
            chatMsg.setCancelled(true);
            String command = chatMsg.getContent().substring(1);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(chatMsg.getUuid_sender());


            int lastChatId = chatMsg.getChatID_sender();
            BukkitScheduler scheduler = Bukkit.getScheduler();
            if (command.compareToIgnoreCase("restart") == 0) {
                scheduler.runTaskLater(
                        this.plugin,
                        () -> {
                            final MessageInterceptingCommandRunner cmdRunner = new MessageInterceptingCommandRunner(
                                    Bukkit.getConsoleSender(),
                                    new TelegramCallback(lastChatId),
                                    offlinePlayer);
                            Bukkit.getLogger().log(Level.INFO, "restarting Server delayed: " + command);
                            Bukkit.dispatchCommand(cmdRunner, command);
                        },
                        60);
            } else {
                scheduler.runTask(
                        this.plugin,
                        () -> {
                            final MessageInterceptingCommandRunner cmdRunner = new MessageInterceptingCommandRunner(
                                    Bukkit.getConsoleSender(),
                                    new TelegramCallback(lastChatId),
                                    offlinePlayer);
                            Bukkit.getLogger().log(Level.INFO, "performing command: " + command);
                            Bukkit.dispatchCommand(cmdRunner, command);
                        });
            }
        }
    }

    @Override
    public void close() throws SecurityException {
        // TODO Auto-generated method stub

    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLevel() == Level.SEVERE) {
            String s = this.getFormatter().format(record);
            ChatMessageToTelegram chat = new ChatMessageToTelegram();
            chat.text = s;
            telegram.sendAll(chat);
        }
    }
}
