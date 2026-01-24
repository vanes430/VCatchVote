package github.vanes430.vcatchvote.utils;

import github.vanes430.vcatchvote.VCatchVote;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final VCatchVote plugin;

    public DiscordWebhook(VCatchVote plugin) {
        this.plugin = plugin;
    }

    public void send(String content) {
        String webhookUrl = plugin.getConfig().getString("discord-webhook.url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL")) {
            return;
        }

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "VCatchVote-Webhook");
                conn.setDoOutput(true);

                String json = buildJson(content);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    plugin.getLogger().warning("Failed to send webhook. Response Code: " + code);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error sending webhook: " + e.getMessage());
            }
        });
    }

    private String buildJson(String content) {
        // Simple JSON builder to avoid dependencies. 
        // Escapes double quotes in content to prevent broken JSON.
        String escapedContent = content.replace("\"", "\\\"").replace("\n", "\\n");
        String username = plugin.getConfig().getString("discord-webhook.username", "VCatchVote");
        String avatar = plugin.getConfig().getString("discord-webhook.avatar-url", "");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"content\": \"").append(escapedContent).append("\",");
        json.append("\"username\": \"").append(username).append("\"");
        
        if (avatar != null && !avatar.isEmpty()) {
            json.append(", \"avatar_url\": \"").append(avatar).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
}
