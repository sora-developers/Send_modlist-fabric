package com.rakku212.sendmodlist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=1.20.6 {
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
//?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=1.20.1 && <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?}
//? if >=1.20.1 && <26.1 {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//?}
//? if >=1.20.6 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
*///?}
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
//? if >=1.21.1 && <26.1 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.Minecraft;
//? if >=1.20.1 && <26.1 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
//?}
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
*///?}
//? if >=1.20.6 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
//?}
//? if >=1.17 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SendModList implements ClientModInitializer {
    //? if >=1.17 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_NAME);
    //?} else {
    /*private static final Logger LOGGER = LogManager.getLogger(Constants.MOD_NAME);
    *///?}
    //? if >=1.19 {
    private static final Component VERIFYING_MODLIST_MESSAGE = Component.literal("mod検証中...");
    //?} else {
    /*private static final Component VERIFYING_MODLIST_MESSAGE = new TextComponent("mod検証中...");
    *///?}
    private static boolean awaitingModListAccepted;
    private static boolean pendingReplyViaConfiguration;
    private static boolean modListPayloadDelivered;
    private static byte[] sessionHmacKey;
    private static byte[] pendingModListPayloadBytes;
    private static boolean modListPayloadBuildFailed;
    private static int trySendLogCooldown;

    @Override
    public void onInitializeClient() {
        //? if >=1.20.6 {
        //? if >=26.1 {
        /*PayloadTypeRegistry.clientboundConfiguration().register(ModListPayload.ID, ModListPayload.CONFIG_CODEC);
        PayloadTypeRegistry.serverboundConfiguration().register(ModListPayload.ID, ModListPayload.CONFIG_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModListPayload.ID, ModListPayload.PLAY_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ModListPayload.ID, ModListPayload.PLAY_CODEC);
        *///?} else {
        PayloadTypeRegistry.configurationS2C().register(ModListPayload.ID, ModListPayload.CONFIG_CODEC);
        PayloadTypeRegistry.configurationC2S().register(ModListPayload.ID, ModListPayload.CONFIG_CODEC);
        PayloadTypeRegistry.playC2S().register(ModListPayload.ID, ModListPayload.PLAY_CODEC);
        PayloadTypeRegistry.playS2C().register(ModListPayload.ID, ModListPayload.PLAY_CODEC);
        //?}
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(ModListProtocol.CHANNEL_ID, (client, handler, buf, responseSender) -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String message = new String(bytes, StandardCharsets.UTF_8).trim();
            client.execute(() -> handleServerMessage(message, false));
        });
        *///?}

        //? if >=1.20.1 && <26.1 {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register(SendModList::renderOverlayOnScreen));

        HudRenderCallback.EVENT.register(SendModList::renderOverlayOnHud);
        //?}

        ClientTickEvents.END_CLIENT_TICK.register(client -> tryDeliverModListPayload());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetModListSession());

        //? if >=1.20.6 {
        ClientConfigurationNetworking.registerGlobalReceiver(ModListPayload.ID, (payload, context) -> {
            String message = new String(payload.payloadBytes(), StandardCharsets.UTF_8).trim();
            context.client().execute(() -> handleServerMessage(message, true));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModListPayload.ID, (payload, context) -> {
            String message = new String(payload.payloadBytes(), StandardCharsets.UTF_8).trim();
            context.client().execute(() -> handleServerMessage(message, false));
        });
        //?}
    }

    private static void resetModListSession() {
        awaitingModListAccepted = false;
        pendingReplyViaConfiguration = false;
        modListPayloadDelivered = false;
        sessionHmacKey = null;
        pendingModListPayloadBytes = null;
        modListPayloadBuildFailed = false;
        trySendLogCooldown = 0;
    }

    private static void handleServerMessage(String message, boolean fromConfiguration) {
        String requestPrefix =
                ModListProtocol.REQUEST_MODLIST + ModListProtocol.REQUEST_MODLIST_SEPARATOR;
        if (message.startsWith(requestPrefix)) {
            String keyB64 = message.substring(requestPrefix.length());
            byte[] decodedKey;
            try {
                decodedKey = Base64.getDecoder().decode(keyB64);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid HMAC session key (base64): {}", e.getMessage());
                return;
            }
            if (decodedKey.length == 0) {
                LOGGER.warn("Empty HMAC session key");
                return;
            }
            sessionHmacKey = decodedKey;
            pendingReplyViaConfiguration = fromConfiguration;
            awaitingModListAccepted = true;
            modListPayloadDelivered = false;
            pendingModListPayloadBytes = null;
            modListPayloadBuildFailed = false;
            trySendLogCooldown = 0;
            return;
        }

        if (ModListProtocol.REQUEST_MODLIST.equals(message)) {
            LOGGER.warn("Received REQUEST_MODLIST without HMAC session key; server requires signed payload");
            return;
        }

        if (ModListProtocol.MODLIST_ACCEPTED.equals(message)) {
            clearVerifyingOverlay();
            return;
        }

        LOGGER.debug("Received unknown modlist channel message: {}", message);
    }

    private static void clearVerifyingOverlay() {
        if (!awaitingModListAccepted) {
            return;
        }
        resetModListSession();
    }

    //? if >=1.20.1 && <26.1 {
    private static void renderOverlayOnScreen(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        if (!awaitingModListAccepted) {
            return;
        }
        fillOverlay(graphics, screen.width, screen.height);
    }

    //? if >=1.21.1 {
    private static void renderOverlayOnHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
    //?} else {
    /*private static void renderOverlayOnHud(GuiGraphics graphics, float tickDelta) {
    *///?}
        if (!awaitingModListAccepted) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null) {
            return;
        }
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        fillOverlay(graphics, w, h);
    }

    private static void fillOverlay(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0xC0101010);
        graphics.drawCenteredString(Minecraft.getInstance().font, VERIFYING_MODLIST_MESSAGE, width / 2, height / 2 - 9, 0xFFFFFF);
    }
    //?}

    private static void tryDeliverModListPayload() {
        if (!awaitingModListAccepted || modListPayloadDelivered) {
            return;
        }
        if (modListPayloadBuildFailed) {
            return;
        }
        if (pendingModListPayloadBytes == null) {
            pendingModListPayloadBytes = buildModListPayloadBytes();
            if (pendingModListPayloadBytes == null) {
                modListPayloadBuildFailed = true;
                return;
            }
        }

        //? if >=1.20.6 {
        ModListPayload payload = new ModListPayload(pendingModListPayloadBytes);
        if (pendingReplyViaConfiguration) {
            try {
                if (configurationNetworkingCanSend()) {
                    ClientConfigurationNetworking.send(payload);
                    pendingReplyViaConfiguration = false;
                    modListPayloadDelivered = true;
                    LOGGER.info("Sent mod list payload (configuration): {} bytes", pendingModListPayloadBytes.length);
                    return;
                }
            } catch (IllegalStateException e) {
                pendingReplyViaConfiguration = false;
            } catch (RuntimeException e) {
                pendingReplyViaConfiguration = false;
                LOGGER.warn("Configuration modlist send failed: {}", e.getMessage());
            }
        }

        if (Minecraft.getInstance().getConnection() != null) {
            try {
                ClientPlayNetworking.send(payload);
                pendingReplyViaConfiguration = false;
                modListPayloadDelivered = true;
                LOGGER.info("Sent mod list payload (play): {} bytes", pendingModListPayloadBytes.length);
            } catch (RuntimeException e) {
                LOGGER.warn("Play modlist send failed (retry next tick): {}", e.getMessage());
            }
            return;
        }
        //?} else {
        /*if (Minecraft.getInstance().getConnection() != null) {
            try {
                FriendlyByteBuf sendBuf = PacketByteBufs.create();
                sendBuf.writeBytes(pendingModListPayloadBytes);
                ClientPlayNetworking.send(ModListProtocol.CHANNEL_ID, sendBuf);
                modListPayloadDelivered = true;
                LOGGER.info("Sent mod list payload (play): {} bytes", pendingModListPayloadBytes.length);
            } catch (RuntimeException e) {
                LOGGER.warn("Play modlist send failed (retry next tick): {}", e.getMessage());
            }
            return;
        }
        *///?}

        if (trySendLogCooldown > 0) {
            trySendLogCooldown--;
        } else {
            trySendLogCooldown = 100;
            LOGGER.debug("Waiting for play connection before modlist send");
        }
    }

    private static boolean configurationNetworkingCanSend() {
        //? if >=1.20.6 {
        try {
            return ClientConfigurationNetworking.canSend(ModListPayload.ID);
        } catch (IllegalStateException e) {
            return false;
        }
        //?} else {
        /*return false;
        *///?}
    }

    private static byte[] buildModListPayloadBytes() {
        List<ModListPayloadBuilder.ModInfo> mods = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            mods.add(new ModListPayloadBuilder.ModInfo(
                    mod.getMetadata().getId(),
                    mod.getMetadata().getName(),
                    mod.getMetadata().getVersion().getFriendlyString(),
                    mod.getMetadata().getEnvironment().name()
            ));
        }

        String loaderVersion = FabricLoader.getInstance().getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        String jsonBody = ModListPayloadBuilder
                .build("fabric", loaderVersion, mods)
                .toString();
        if (sessionHmacKey == null) {
            LOGGER.warn("Cannot build mod list payload: no HMAC session key");
            return null;
        }
        byte[] payloadBytes = ModListHmac.utf8SignedPayload(sessionHmacKey, jsonBody);

        if (payloadBytes.length > ModListProtocol.MAX_PLUGIN_MESSAGE_SIZE) {
            LOGGER.warn("Mod list payload is too large to send: {} bytes", payloadBytes.length);
            return null;
        }

        return payloadBytes;
    }

    //? if >=1.20.6 {
    private static void writePayloadBytes(FriendlyByteBuf buf, ModListPayload payload) {
        buf.writeBytes(payload.payloadBytes());
    }

    private static ModListPayload readPayloadBytes(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new ModListPayload(bytes);
    }

    public record ModListPayload(byte[] payloadBytes) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ModListPayload> ID =
                new CustomPacketPayload.Type<>(ModListProtocol.CHANNEL_ID);
        public static final StreamCodec<FriendlyByteBuf, ModListPayload> CONFIG_CODEC =
                StreamCodec.of(SendModList::writePayloadBytes, SendModList::readPayloadBytes);
        public static final StreamCodec<RegistryFriendlyByteBuf, ModListPayload> PLAY_CODEC =
                StreamCodec.of(
                        (RegistryFriendlyByteBuf buf, ModListPayload payload) ->
                                SendModList.writePayloadBytes(buf, payload),
                        buf -> SendModList.readPayloadBytes(buf));

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
    //?}
}
