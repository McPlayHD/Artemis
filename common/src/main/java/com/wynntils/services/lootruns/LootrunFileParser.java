/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.lootruns;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.services.lootruns.type.LootrunNote;
import com.wynntils.services.lootruns.type.LootrunPath;
import com.wynntils.services.lootruns.type.LootrunSaveResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.PositionImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class LootrunFileParser {
    public static LootrunUncompiled readJson(File file, JsonObject json) {
        JsonArray points = json.getAsJsonArray("points");
        LootrunPath pointsList = new LootrunPath(new ArrayList<>());
        for (JsonElement element : points) {
            JsonObject pointJson = element.getAsJsonObject();
            Vec3 position = new Vec3(
                    pointJson.get("x").getAsDouble(),
                    pointJson.get("y").getAsDouble(),
                    pointJson.get("z").getAsDouble());
            pointsList.points().add(position);
        }
        JsonArray chestsJson = json.getAsJsonArray("chests");
        Set<BlockPos> chests = new HashSet<>();
        if (chestsJson != null) {
            for (JsonElement element : chestsJson) {
                JsonObject chestJson = element.getAsJsonObject();
                BlockPos pos = new BlockPos(
                        chestJson.get("x").getAsInt(),
                        chestJson.get("y").getAsInt(),
                        chestJson.get("z").getAsInt());
                chests.add(pos);
            }
        }
        JsonArray notesJson = json.getAsJsonArray("notes");
        List<LootrunNote> notes = new ArrayList<>();
        if (notesJson != null) {
            for (JsonElement element : notesJson) {
                JsonObject noteJson = element.getAsJsonObject();
                JsonObject positionJson = noteJson.getAsJsonObject("position");

                // Artemis builds, until this point have used a slightly different format for notes
                // This perserves support for those files, as this commit fixes the format to match legacy
                if (positionJson == null) {
                    positionJson = noteJson.getAsJsonObject("location");
                }

                Position position = new PositionImpl(
                        positionJson.get("x").getAsDouble(),
                        positionJson.get("y").getAsDouble(),
                        positionJson.get("z").getAsDouble());
                Component component = Component.Serializer.fromJson(noteJson.get("note"));
                LootrunNote note = new LootrunNote(position, component);
                notes.add(note);
            }
        }
        return new LootrunUncompiled(pointsList, chests, notes, file);
    }

    public static LootrunSaveResult writeJson(LootrunUncompiled activeLootrun, File file) {
        try {
            boolean result = file.createNewFile();

            if (!result) {
                return LootrunSaveResult.ERROR_ALREADY_EXISTS;
            }

            JsonObject json = new JsonObject();
            JsonArray points = new JsonArray();
            for (Position point : activeLootrun.path().points()) {
                JsonObject pointJson = new JsonObject();
                pointJson.addProperty("x", point.x());
                pointJson.addProperty("y", point.y());
                pointJson.addProperty("z", point.z());
                points.add(pointJson);
            }
            json.add("points", points);

            JsonArray chests = new JsonArray();
            for (BlockPos chest : activeLootrun.chests()) {
                JsonObject chestJson = new JsonObject();
                chestJson.addProperty("x", chest.getX());
                chestJson.addProperty("y", chest.getY());
                chestJson.addProperty("z", chest.getZ());
                chests.add(chestJson);
            }
            json.add("chests", chests);

            JsonArray notes = new JsonArray();
            for (LootrunNote note : activeLootrun.notes()) {
                JsonObject noteJson = new JsonObject();
                JsonObject locationJson = new JsonObject();

                Position position = note.position();
                locationJson.addProperty("x", position.x());
                locationJson.addProperty("y", position.y());
                locationJson.addProperty("z", position.z());
                noteJson.add("location", locationJson);

                noteJson.add("note", Component.Serializer.toJsonTree(note.component()));
                notes.add(noteJson);
            }
            json.add("notes", notes);

            json.addProperty(
                    "date",
                    DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US)
                            .format(new Date()));
            FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);
            WynntilsMod.GSON.toJson(json, writer);
            writer.close();
            return LootrunSaveResult.SAVED;
        } catch (IOException ex) {
            return LootrunSaveResult.ERROR_SAVING;
        }
    }
}
