package com.sinthoras.visualprospecting.database.veintypes;

import static com.sinthoras.visualprospecting.Utils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sinthoras.visualprospecting.Tags;

import gregtech.api.enums.OreMixes;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VeinTypeCaching {

    private static final Object2ObjectMap<String, VeinType> veinTypes = new Object2ObjectOpenHashMap<>();

    public static void init() {
        veinTypes.put(Tags.ORE_MIX_NONE_NAME, VeinType.NO_VEIN);

        for (OreMixes mix : OreMixes.values()) {
            veinTypes.put(mix.oreMixBuilder.oreMixName, new VeinType(mix.oreMixBuilder));
        }
    }

    public static @NotNull VeinType getVeinType(String veinTypeName) {
        return veinTypes.getOrDefault(veinTypeName, VeinType.NO_VEIN);
    }

    public static Collection<VeinType> getVeinTypes() {
        return veinTypes.values();
    }

    public static void recalculateSearch(@Nullable Pattern filterPattern) {
        for (VeinType veinType : veinTypes.values()) {
            if (veinType == VeinType.NO_VEIN) continue;
            if (filterPattern != null) {
                List<String> searchableStrings = new ArrayList<>(veinType.getOreMaterialNames());
                searchableStrings.add(veinType.getVeinName());
                final boolean match = searchableStrings.stream().map(EnumChatFormatting::getTextWithoutFormattingCodes)
                        .map(String::toLowerCase)
                        .anyMatch(searchableString -> filterPattern.matcher(searchableString).find());

                veinType.setNEISearchHighlight(match);
            } else {
                veinType.setNEISearchHighlight(true);
            }
        }
    }
}
