/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.caves;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.ActivityModel;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityDistance;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.models.activities.type.ActivityType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CaveModel extends Model {
    private List<CaveInfo> caves = new ArrayList<>();
    private List<StyledText> caveProgress = List.of();

    public CaveModel(ActivityModel activityModel) {
        super(List.of(activityModel));
    }

    public void reloadCaves() {
        WynntilsMod.info("Requesting rescan of caves in Activity Book");
        Models.Activity.scanContentBook(ActivityType.CAVE, this::updateCavesFromQuery);
    }

    private void updateCavesFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<CaveInfo> newCaves = new ArrayList<>();

        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.CAVE) {
                WynntilsMod.warn("Incorrect cave activity type recieved: " + activity);
                continue;
            }
            CaveInfo caveInfo = getCaveInfoFromActivity(activity);
            newCaves.add(caveInfo);
        }
        caves = newCaves;
        caveProgress = progress;
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.CAVE));
        WynntilsMod.info("Updated caves from query, got " + caves.size() + " caves.");
    }

    public Optional<CaveInfo> getCaveInfoFromName(String name) {
        return caves.stream().filter(cave -> cave.getName().equals(name)).findFirst();
    }

    public List<CaveInfo> getSortedCaves(ActivitySortOrder sortOrder) {
        return sortCaveInfoList(sortOrder, caves);
    }

    private List<CaveInfo> sortCaveInfoList(ActivitySortOrder sortOrder, List<CaveInfo> caveList) {
        // All caves are always sorted by status (available then unavailable), and then
        // the given sort order, and finally a third way if the given sort order is equal.

        CaveInfo trackedCaveInfo = Models.Activity.getTrackedCaveInfo();
        String trackedCaveName = trackedCaveInfo != null ? trackedCaveInfo.getName() : "";
        Comparator<CaveInfo> baseComparator =
                Comparator.comparing(caveInfo -> !caveInfo.getName().equals(trackedCaveName));
        return switch (sortOrder) {
            case LEVEL -> caveList.stream()
                    .sorted(baseComparator
                            .thenComparing(CaveInfo::getStatus)
                            .thenComparing(CaveInfo::getRecommendedLevel)
                            .thenComparing(CaveInfo::getName))
                    .toList();
            case DISTANCE -> caveList.stream()
                    .sorted(baseComparator
                            .thenComparing(CaveInfo::getStatus)
                            .thenComparing(CaveInfo::getDistance)
                            .thenComparing(CaveInfo::getName))
                    .toList();
            case ALPHABETIC -> caveList.stream()
                    .sorted(baseComparator
                            .thenComparing(CaveInfo::getStatus)
                            .thenComparing(CaveInfo::getName)
                            .thenComparing(CaveInfo::getRecommendedLevel))
                    .toList();
        };
    }

    public List<StyledText> getCaveProgress() {
        return Collections.unmodifiableList(caveProgress);
    }

    private CaveInfo getCaveInfoFromActivity(ActivityInfo activity) {
        return new CaveInfo(
                activity.name(),
                activity.status(),
                activity.description().orElse(StyledText.EMPTY).getString(),
                activity.requirements().level().key(),
                activity.distance().orElse(ActivityDistance.NEAR),
                activity.length().orElse(ActivityLength.SHORT),
                activity.difficulty().orElse(ActivityDifficulty.EASY),
                activity.rewards());
    }
}
