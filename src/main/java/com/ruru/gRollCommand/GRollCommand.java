package com.ruru.gRollCommand;

import org.bukkit.plugin.java.JavaPlugin;

public final class GRollCommand extends JavaPlugin {

    private static GRollCommand instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        RollCommandExecutor executor = new RollCommandExecutor(this);
        if (getCommand("roll") != null) {
            getCommand("roll").setExecutor(executor);
            getCommand("roll").setTabCompleter(executor); // 리스트명 자동완성
        }
        if (getCommand("rollreload") != null) {
            getCommand("rollreload").setExecutor(executor); // 리로드 커맨드
        }

        getLogger().info("[GRollCommand] enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[GRollCommand] disabled.");
    }

    public static GRollCommand getInstance() {
        return instance;
    }
}
