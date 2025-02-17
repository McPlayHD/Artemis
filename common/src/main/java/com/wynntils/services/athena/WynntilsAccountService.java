/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.athena;

import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.services.athena.event.AthenaLoginEvent;
import com.wynntils.utils.mc.McUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Crypt;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.codec.binary.Hex;

public final class WynntilsAccountService extends Manager {
    private static final String NO_TOKEN = "<no token>";

    private String token = NO_TOKEN;
    private boolean loggedIn = false;

    private final HashMap<String, String> encodedConfigs = new HashMap<>();
    private final HashMap<String, String> md5Verifications = new HashMap<>();

    public WynntilsAccountService() {
        super(List.of());
        login();
    }

    public void reauth() {
        loggedIn = false;
        login();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (!event.isFirstJoinWorld()) return;

        if (!loggedIn) {
            MutableComponent failed = Component.literal(
                            "Welps! Trying to connect and set up the Wynntils Account with your data has failed. "
                                    + "Most notably, cloud config syncing will not work. To try this action again, run ")
                    .withStyle(ChatFormatting.GREEN);
            failed.append(Component.literal("/wynntils reauth")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wynntils reauth"))));

            McUtils.sendMessageToClient(failed);
        }
    }

    private void login() {
        if (loggedIn) return;

        doLogin();
    }

    public String getToken() {
        return token;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public Map<String, String> getEncodedConfigs() {
        return encodedConfigs;
    }

    public void dumpEncodedConfig(String name) {
        encodedConfigs.remove(name);
    }

    private void doLogin() {
        // generating secret key
        ApiResponse apiResponse = Managers.Net.callApi(UrlId.API_ATHENA_AUTH_PUBLIC_KEY);
        apiResponse.handleJsonObject(json -> {
            String secretKey = parseAndJoinPublicKey(json.get("publicKeyIn").getAsString());

            Map<String, String> arguments = new HashMap<>();
            arguments.put("key", secretKey);
            arguments.put("username", McUtils.mc().getUser().getName());
            arguments.put("version", String.format("A%s %s", WynntilsMod.getVersion(), WynntilsMod.getModLoader()));

            ApiResponse apiAuthResponse = Managers.Net.callApi(UrlId.API_ATHENA_AUTH_RESPONSE, arguments);
            apiAuthResponse.handleJsonObject(authJson -> {
                token = authJson.get("authToken").getAsString(); /* md5 hashes*/
                JsonObject hashes = authJson.getAsJsonObject("hashes");
                hashes.entrySet()
                        .forEach((k) ->
                                md5Verifications.put(k.getKey(), k.getValue().getAsString())); /* configurations*/
                JsonObject configFiles = authJson.getAsJsonObject("configFiles");
                configFiles
                        .entrySet()
                        .forEach((k) ->
                                encodedConfigs.put(k.getKey(), k.getValue().getAsString()));
                loggedIn = true;
                WynntilsMod.info("Successfully connected to Athena!");

                WynntilsMod.postEvent(new AthenaLoginEvent());
            });
        });
    }

    private String parseAndJoinPublicKey(String key) {
        try {
            byte[] publicKeyBy = Hex.decodeHex(key.toCharArray());

            SecretKey secretkey = Crypt.generateSecretKey();

            PublicKey publicKey = Crypt.byteToPublicKey(publicKeyBy);

            String s1 = (new BigInteger(Crypt.digestData("", publicKey, secretkey))).toString(16);

            McUtils.mc()
                    .getMinecraftSessionService()
                    .joinServer(
                            McUtils.mc().getUser().getGameProfile(),
                            McUtils.mc().getUser().getAccessToken(),
                            s1.toLowerCase(Locale.ROOT));

            byte[] secretKeyEncrypted = Crypt.encryptUsingKey(publicKey, secretkey.getEncoded());

            return Hex.encodeHexString(secretKeyEncrypted);
        } catch (Exception ex) {
            WynntilsMod.error("Failed to parse public key.", ex);
            return "";
        }
    }
}
