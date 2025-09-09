package com.ruru.gRollCommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class RollCommandExecutor implements CommandExecutor, TabCompleter {

    private final GRollCommand plugin;

    private final Map<UUID, Map<String, Long>> cooldownMap = new HashMap<>();

    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "Roll" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private static final Pattern COMMAND_KEY = Pattern.compile("^command\\d*$", Pattern.CASE_INSENSITIVE);

    public RollCommandExecutor(GRollCommand plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("rollreload")) {
            if (!sender.hasPermission("groll.reload")) {
                sender.sendMessage(PREFIX + ChatColor.RED + "권한이 없습니다.");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(PREFIX + ChatColor.GREEN + "config.yml 리로드 완료!");
            return true;
        }

        if (!cmd.getName().equalsIgnoreCase("roll")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "사용법: /" + label + " <리스트명>");
            Set<String> lists = getListNames();
            if (!lists.isEmpty()) {
                player.sendMessage(PREFIX + "가능한 리스트: " + ChatColor.AQUA + String.join(ChatColor.GRAY + ", " + ChatColor.AQUA, lists));
            }
            return true;
        }

        String listName = args[0];
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection listsSec = cfg.getConfigurationSection("commandLists");
        if (listsSec == null || !listsSec.isConfigurationSection(listName)) {
            player.sendMessage(PREFIX + ChatColor.RED + "존재하지 않는 리스트입니다: " + ChatColor.YELLOW + listName);
            return true;
        }

        ConfigurationSection thisList = listsSec.getConfigurationSection(listName);
        int cooldownSec = Math.max(0, thisList.getInt("cooldown", 0));

        if (cooldownSec > 0) {
            long now = System.currentTimeMillis();
            long end = cooldownMap
                    .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .getOrDefault(listName.toLowerCase(Locale.ROOT), 0L);

            if (end > now) {
                long remain = (end - now + 999) / 1000;
                player.sendMessage(PPREFIX() + ChatColor.RED + "아직 쿨타임입니다. "
                        + ChatColor.YELLOW + remain + "초 " + ChatColor.RED + "후에 다시 시도하세요.");
                return true;
            }
        }

        List<Map<String, Object>> entries = readEntries(thisList);
        if (entries.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.RED + "해당 리스트에 commands가 비어 있습니다.");
            return true;
        }

        int index = pickIndexByWeight(entries);
        if (index < 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "가중치 합계가 0이거나 유효한 항목이 없습니다.");
            return true;
        }

        Map<String, Object> chosen = entries.get(index);
        List<String> commandsToRun = extractCommands(chosen);
        if (commandsToRun.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.RED + "선택된 항목에 실행할 커맨드가 없습니다.");
            return true;
        }

        for (String raw : commandsToRun) {
            String parsed = applyPlaceholders(raw, player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(parsed));
        }

        if (cooldownSec > 0) {
            cooldownMap
                    .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(listName.toLowerCase(Locale.ROOT), System.currentTimeMillis() + cooldownSec * 1000L);
        }

        return true;
    }

    private String PPREFIX() { return PREFIX; }


    private Set<String> getListNames() {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection listsSec = cfg.getConfigurationSection("commandLists");
        if (listsSec == null) return Collections.emptySet();
        return listsSec.getKeys(false);
    }

    private List<Map<String, Object>> readEntries(ConfigurationSection listSection) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<?> raw = listSection.getList("commands");
        if (raw == null) return out;

        for (Object o : raw) {
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                out.add(m);
            }
        }
        return out;
    }

    private int pickIndexByWeight(List<Map<String, Object>> entries) {
        int total = 0;
        int[] weights = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            Object w = entries.get(i).get("weight");
            int ww = 1;
            if (w instanceof Number) ww = ((Number) w).intValue();
            else if (w instanceof String) {
                try { ww = Integer.parseInt((String) w); } catch (NumberFormatException ignored) {}
            }
            ww = Math.max(0, ww);
            weights[i] = ww;
            total += ww;
        }
        if (total <= 0) return -1;

        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r < acc) return i;
        }
        return -1;
    }

    private List<String> extractCommands(Map<String, Object> chosen) {
        return chosen.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> COMMAND_KEY.matcher(e.getKey()).matches())
                .map(e -> String.valueOf(e.getValue()))
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String applyPlaceholders(String cmd, Player p) {
        return cmd.replace("%p", p.getName())
                .replace("%uuid", p.getUniqueId().toString());
    }

    private String stripLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("roll")) return Collections.emptyList();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return getListNames().stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
