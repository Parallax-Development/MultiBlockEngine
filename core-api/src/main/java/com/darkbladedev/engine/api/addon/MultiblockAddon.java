package com.darkbladedev.engine.api.addon;

public interface MultiblockAddon {
    String getId();
    String getVersion();
    
    void onLoad(AddonContext context) throws AddonException;
    void onEnable() throws AddonException;
    void onDisable();
}
