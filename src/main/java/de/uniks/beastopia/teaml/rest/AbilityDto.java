package de.uniks.beastopia.teaml.rest;

public record AbilityDto(
        int id,
        String name,
        String description,
        String type,
        int maxUses,
        double accuracy,
        int power
) {
}
