package com.github.sanctum.myessentials.util;

import com.github.sanctum.myessentials.Essentials;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Objects;
import java.util.Properties;

public enum ConfiguredMessage {
    MUST_BE_PLAYER("must-be-player"),
    TRY_IN_SURVIVAL("try-in-survival"),
    // Fly messages
    FLIGHT_OFF("flight-off"),
    FLIGHT_ON("flight-on");

    private final String key;

    private static Properties properties;

    ConfiguredMessage(String s) {
        this.key = s;
    }

    @Nullable
    public String get() {
        return properties.getProperty(key);
    }

    @Override
    public String toString() {
        final String s = get();
        return (s != null) ? ChatColor.translateAlternateColorCodes('&', s) : "null";
    }

    public String replace(Object... objects) {
        String replacement = toString();
        int i = 0;
        for (Object obj : objects) {
            replacement = StringUtils.replace(replacement, "{" + i++ + "}", (obj instanceof String) ? (String) obj : obj.toString());
        }
        return replacement;
    }

    public TextComponent asComponent() {
        final TextComponent textComponent = new TextComponent();
        for (BaseComponent bc : TextComponent.fromLegacyText(toString())) {
            textComponent.addExtra(bc);
        }
        return textComponent;
    }

    public static void loadProperties(Essentials essentials) {
        final Properties defaults = new Properties();
        final InputStream resource = essentials.getResource("messages.properties");
        try {
            defaults.load(new InputStreamReader(Objects.requireNonNull(resource)));
        } catch (IOException e) {
            throw new IllegalStateException("Messages missing from the .jar!", e);
        }
        final File file = new File(essentials.getDataFolder(), "messages.properties");
        if (file.exists()) {
            properties = new Properties(defaults);
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                essentials.getLogger().severe("Unable to load external copy of messages.properties");
                e.printStackTrace();
            }
            return;
        }
        properties = defaults;
    }
}