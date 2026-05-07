package com.rakku212.sendmodlist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public final class ModListPayloadBuilder {

    public static JsonObject build(String clientType, String loaderVersion, List<ModInfo> mods) {
        JsonArray modsJson = new JsonArray();
        for (ModInfo mod : mods) {
            JsonObject modJson = new JsonObject();
            modJson.addProperty("id", mod.id());
            modJson.addProperty("name", mod.name());
            modJson.addProperty("version", mod.version());
            modJson.addProperty("environment", mod.environment());
            modsJson.add(modJson);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("client", clientType);
        payload.addProperty("loaderVersion", loaderVersion);
        payload.add("mods", modsJson);
        return payload;
    }

    public static final class ModInfo {
        private final String id;
        private final String name;
        private final String version;
        private final String environment;

        public ModInfo(String id, String name, String version, String environment) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.environment = environment;
        }

        public String id() { return id; }
        public String name() { return name; }
        public String version() { return version; }
        public String environment() { return environment; }
    }
}
