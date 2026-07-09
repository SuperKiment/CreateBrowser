package net.createbrowser.forge;

import net.createbrowser.compat.CreateBridge;
import net.minecraftforge.fml.ModList;

import java.util.List;

/** Forge implementation of CreateBridge — queries ModList at runtime. */
public final class CreateBridgeForge implements CreateBridge {

    @Override
    public boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    @Override
    public String getCreateVersion() {
        return ModList.get().getModContainerById("create")
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse(null);
    }

    @Override
    public List<String> getInstalledCreateAddons() {
        return ModList.get().getMods().stream()
            .filter(info -> info.getDependencies().stream()
                .anyMatch(dep -> dep.getModId().equals("create")))
            .map(info -> info.getModId())
            .filter(id -> !id.equals("create"))
            .toList();
    }
}
