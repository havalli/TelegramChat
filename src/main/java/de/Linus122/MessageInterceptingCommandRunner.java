package de.Linus122;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class MessageInterceptingCommandRunner implements ConsoleCommandSender {
    private final ConsoleCommandSender wrappedSender;
    private ITelegramCallback telegramCallback;
    private OfflinePlayer player;
    private final Spigot spigotWrapper;
    private net.milkbowl.vault.permission.Permission permissionsAdapter;
    //private final StringBuilder msgLog = new StringBuilder();

    public interface ITelegramCallback {
        void Send(String message);
    }

    private class Spigot extends CommandSender.Spigot {
        /**
         * Sends this sender a chat component.
         *
         * @param component the components to send
         */
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent component) {
            Bukkit.getLogger().log(Level.INFO, "Spigot.sendMessage()");
            wrappedSender.spigot().sendMessage(component);
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         */
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent... components) {
            Bukkit.getLogger().log(Level.INFO, "Spigot.sendMessage(md5)");
            wrappedSender.spigot().sendMessage(components);
            sendLogToTelegram();
        }
    }

    /*
    public String getMessageLog() {
        return msgLog.toString();
    }

    public String getMessageLogStripColor() {
        return ChatColor.stripColor(msgLog.toString());
    }

    public void clearMessageLog() {
        msgLog.setLength(0);
    }*/

    private void sendLogToTelegram(String... messages){
        StringBuilder msgLog = new StringBuilder();
        for (String message : messages) {
            msgLog.append(message).append('\n');
        }
        String message = ChatColor.stripColor(msgLog.toString());
        telegramCallback.Send(message);
    }

    private void setupVault() {
        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        permissionsAdapter = rsp.getProvider();
    }

    public MessageInterceptingCommandRunner(ConsoleCommandSender wrappedSender, ITelegramCallback telegramCallback, OfflinePlayer player) {
        this.wrappedSender = wrappedSender;
        this.telegramCallback = telegramCallback;
        this.player = player;
        setupVault();
        spigotWrapper = new Spigot();
    }

    @Override
    public void sendMessage(String message) {
        Bukkit.getLogger().log(Level.INFO, "sendMessage(string)");
        wrappedSender.sendMessage(message);
        //msgLog.append(message).append('\n');
        sendLogToTelegram(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        Bukkit.getLogger().log(Level.INFO, "Spigot.sendMessage(string[])");
        wrappedSender.sendMessage(messages);
        sendLogToTelegram(messages);
    }

    @Override
    public void sendMessage(UUID uuid, String s) {

        Bukkit.getLogger().log(Level.INFO, "Spigot.sendMessage(UUID, String)");
        wrappedSender.sendMessage(uuid, s);
        sendLogToTelegram(s);
    }

    @Override
    public void sendMessage(UUID uuid, String... strings) {

        Bukkit.getLogger().log(Level.INFO, "Spigot.sendMessage(UUID, String...)");
        wrappedSender.sendMessage(uuid, strings);
        sendLogToTelegram(strings);
    }

    @Override
    public Server getServer() {

        Bukkit.getLogger().log(Level.INFO, "getServer");
        return wrappedSender.getServer();
    }

    @Override
    public String getName() {

        Bukkit.getLogger().log(Level.INFO, "GetName");
        return "OrderFulfiller";
    }

    @Override
    public CommandSender.Spigot spigot() {
        Bukkit.getLogger().log(Level.INFO, "spigot");
        return spigotWrapper;
    }

    @Override
    public boolean isConversing() {
        Bukkit.getLogger().log(Level.INFO, "isConversing");
        return wrappedSender.isConversing();
    }

    @Override
    public void acceptConversationInput(String input) {
        Bukkit.getLogger().log(Level.INFO, "acceptConversationInput(String input)");
        wrappedSender.acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        Bukkit.getLogger().log(Level.INFO, "beginConversation(Conversation conversation)");
        return wrappedSender.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        Bukkit.getLogger().log(Level.INFO, "abandonConversation(Conversation conversation)");
        wrappedSender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        Bukkit.getLogger().log(Level.INFO, "abandonConversation(Conversation conversation, ConversationAbandonedEvent details)");
        wrappedSender.abandonConversation(conversation, details);
    }

    @Override
    public void sendRawMessage(String message) {
        Bukkit.getLogger().log(Level.INFO, "sendRawMessage(String message)");
        wrappedSender.sendRawMessage(message);
        sendLogToTelegram(message);
    }

    @Override
    public void sendRawMessage(UUID uuid, String s) {
        Bukkit.getLogger().log(Level.INFO, "sendRawMessage(UUID uuid, String s)");
        wrappedSender.sendRawMessage(uuid, s);
        sendLogToTelegram(s);
    }

    @Override
    public boolean isPermissionSet(String name) {
        Bukkit.getLogger().log(Level.INFO, "isPermissionSet(String name)");
        return wrappedSender.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        Bukkit.getLogger().log(Level.INFO, "isPermissionSet(Permission perm)");
        return wrappedSender.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        Bukkit.getLogger().log(Level.INFO, "hasPermission(String name)");
        return permissionsAdapter.playerHas(null, player, name);
        //return wrappedSender.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        Bukkit.getLogger().log(Level.INFO, "hasPermission(Permission perm)");
        return permissionsAdapter.playerHas(null, player, perm.getName());
        //return wrappedSender.hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        Bukkit.getLogger().log(Level.INFO, "addAttachment(Plugin plugin, String name, boolean value)");
        return wrappedSender.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        Bukkit.getLogger().log(Level.INFO, "addAttachment(Plugin plugin)");
        return wrappedSender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        Bukkit.getLogger().log(Level.INFO, "addAttachment(Plugin plugin, String name, boolean value, int ticks)");
        return wrappedSender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        Bukkit.getLogger().log(Level.INFO, "addAttachment(Plugin plugin, int ticks)");
        return wrappedSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        Bukkit.getLogger().log(Level.INFO, "removeAttachment(PermissionAttachment attachment)");
        wrappedSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        Bukkit.getLogger().log(Level.INFO, "recalculatePermissions()");
        wrappedSender.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Bukkit.getLogger().log(Level.INFO, "getEffectivePermissions()");
        return wrappedSender.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        Bukkit.getLogger().log(Level.INFO, "isOp()");
        return wrappedSender.isOp();
    }

    @Override
    public void setOp(boolean value) {
        Bukkit.getLogger().log(Level.INFO, "setOp(boolean value)");
        wrappedSender.setOp(value);
    }
}
