package gg.nextforge.core.plugin;

import dev.mzcy.LicensedPlugin;

import java.util.UUID;

public abstract class NextForgePlugin extends LicensedPlugin {

    @Override
    public abstract UUID pluginId();

    public abstract int metricsId();

    @Override
    public abstract void enablePlugin();

    @Override
    public abstract void disablePlugin();

}
