package com.example.aimassistant.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EntityCategoryConfig {
    private static Map<String, List<String>> categories = new HashMap<>();
    private static boolean loaded = false;

    public static void loadConfig(Path path) {
        if (!Files.exists(path)) {
            loaded = false;
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            categories = new Gson().fromJson(reader, type);
            loaded = true;
        } catch (Exception e) {
            categories.clear();
            loaded = false;
        }
    }

    public static boolean isLoaded() { return loaded; }

    public static boolean isHostile(String entityId) {
        return loaded && categories.getOrDefault("hostile", Collections.emptyList()).contains(entityId);
    }

    public static boolean isFriendly(String entityId) {
        return loaded && categories.getOrDefault("friendly", Collections.emptyList()).contains(entityId);
    }

    public static boolean isNeutral(String entityId) {
        return loaded && categories.getOrDefault("neutral", Collections.emptyList()).contains(entityId);
    }
}
