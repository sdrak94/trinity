/*
 * Decompiled with CFR 0.139.
 */
package luna.custom.antibot.configsEngine;

import luna.custom.antibot.configsEngine.configs.impl.AntibotConfigs;

public class ConfigsController {
    public void reloadSunriseConfigs() {
        AntibotConfigs.getInstance().loadConfigs();
        
    }

    public static ConfigsController getInstance() {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder {
        protected static final ConfigsController _instance = new ConfigsController();

        private SingletonHolder() {
        }
    }

}