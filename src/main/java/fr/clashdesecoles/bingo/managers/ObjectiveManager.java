package fr.clashdesecoles.bingo.managers;
import com.google.gson.*;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Objective;
import fr.clashdesecoles.bingo.utils.IconUtil;
import org.bukkit.Material;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ObjectiveManager {
    private final BingoPlugin plugin;
    // Persisted: mapping gameId -> list of objective IDs
    private final java.util.concurrent.ConcurrentMap<String, List<Integer>> gameObjectiveIds;
    // Persisted: mapping teamId -> { objectiveId -> status }
    private final java.util.concurrent.ConcurrentMap<String, java.util.concurrent.ConcurrentMap<Integer, Objective.ObjectiveStatus>> teamObjectiveStatus;
    // Objective pool loaded from JSON
    private volatile List<Objective> objectivePool;
    private final java.io.File poolFile;
    private final java.io.File stateFile;

    public ObjectiveManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.gameObjectiveIds = new java.util.concurrent.ConcurrentHashMap<>();
        this.teamObjectiveStatus = new java.util.concurrent.ConcurrentHashMap<>();
        this.poolFile = new java.io.File(plugin.getDataFolder(), "objectives.json");
        this.stateFile = new java.io.File(plugin.getDataFolder(), "objectives_state.json");
        loadObjectivePool();
        loadState();
    }
    
    // Select 25 random objectives without persisting (used for staged roll)
    public CompletableFuture<List<Objective>> chooseRandomObjectives() {
        return CompletableFuture.supplyAsync(() -> {
            List<Objective> pool = new ArrayList<>(objectivePool);
            java.util.Collections.shuffle(pool, new java.util.Random());
            return pool.size() > 25 ? new ArrayList<>(pool.subList(0,25)) : pool;
        });
    }

    // Apply a selected list to a game: persist IDs, reset team statuses, and save
    public CompletableFuture<Boolean> applyRolledObjectives(String gameId, List<Objective> selected) {
        return CompletableFuture.supplyAsync(() -> {
            if (selected == null || selected.isEmpty()) return false;
            List<Integer> ids = new ArrayList<>();
            for (Objective o : selected) ids.add(o.getId());
            gameObjectiveIds.put(gameId, java.util.Collections.unmodifiableList(ids));
            // Reset team objective statuses for teams of this game (fresh grid)
            try {
                var g = plugin.getGameManager().getGame(gameId);
                if (g != null) {
                    if (g.getTeam1Id() != null) teamObjectiveStatus.put(g.getTeam1Id(), new java.util.concurrent.ConcurrentHashMap<>());
                    if (g.getTeam2Id() != null) teamObjectiveStatus.put(g.getTeam2Id(), new java.util.concurrent.ConcurrentHashMap<>());
                }
            } catch (Exception ignored) {}
            saveStateQuiet();
            return true;
        });
    }
    
    public void exportGridFile(String gameId, java.util.List<Objective> objectives) {
        try {
            if (objectives == null) return;
            java.io.File dir = new java.io.File(plugin.getDataFolder(), "exports");
            if (!dir.exists()) dir.mkdirs();
            java.io.File out = new java.io.File(dir, "bingo_" + gameId + ".txt");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(out), java.nio.charset.StandardCharsets.UTF_8))) {
                int total = Math.min(25, objectives.size());
                // Write 5x5 grid with numbers 1..25 (or up to total)
                for (int r = 0; r < 5; r++) {
                    StringBuilder line = new StringBuilder();
                    for (int c = 0; c < 5; c++) {
                        int idx = r * 5 + c;
                        int num = idx < total ? (idx + 1) : 0;
                        String cell = num > 0 ? String.format("%2d", num) : "  ";
                        line.append("[").append(cell).append("] ");
                    }
                    pw.println(line.toString().trim());
                }
                pw.println();
                // Numbered objective names
                for (int i = 0; i < total; i++) {
                    String name = objectives.get(i).getDisplayName();
                    // Strip color codes if any
                    name = name == null ? "" : name.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
                    pw.println((i + 1) + ". " + name);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to export bingo grid file: " + e.getMessage());
        }
    }
    
    public CompletableFuture<Boolean> validateObjective(String gameId, String teamId, int objectiveId) {
        return CompletableFuture.supplyAsync(() -> {
            teamObjectiveStatus.computeIfAbsent(teamId, k -> new java.util.concurrent.ConcurrentHashMap())
                .put(objectiveId, Objective.ObjectiveStatus.SUCCESS);
            saveStateQuiet();
            plugin.getTeamManager().updateTeamPoints(teamId, gameId);
            return true;
        });
    }
    
    public CompletableFuture<Boolean> rejectObjective(String gameId, String teamId, int objectiveId) {
        return CompletableFuture.supplyAsync(() -> {
            teamObjectiveStatus.computeIfAbsent(teamId, k -> new java.util.concurrent.ConcurrentHashMap())
                .put(objectiveId, Objective.ObjectiveStatus.REJECTED);
            saveStateQuiet();
            // Update points as rejection may alter completed lines bonuses
            plugin.getTeamManager().updateTeamPoints(teamId, gameId);
            return true;
        });
    }

    private void loadObjectivePool() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            if (!poolFile.exists()) {
                // create default and save
                List<Objective> defaults = buildDefaultObjectivePool();
                savePool(defaults);
                this.objectivePool = java.util.Collections.unmodifiableList(defaults);
                return;
            }
            String json = new String(java.nio.file.Files.readAllBytes(poolFile.toPath()), StandardCharsets.UTF_8);
            JsonElement rootEl = JsonParser.parseString(json);
            List<Objective> list = new ArrayList<>();
            if (rootEl.isJsonArray()) {
                int maxId = 0;
                for (JsonElement el : rootEl.getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    int id = o.has("id") ? o.get("id").getAsInt() : 0;
                    String internal = o.has("internalName") ? o.get("internalName").getAsString() : ("obj_"+id);
                    String display = o.has("displayName") ? o.get("displayName").getAsString() : internal;
                    String itemStr = o.has("item") ? o.get("item").getAsString() : "BARRIER";
                    Material mat = parseMaterial(itemStr);
                    String actionStr = o.has("action") ? o.get("action").getAsString() : "ITEM";
                    Objective.ObjectiveAction action;
                    try { action = Objective.ObjectiveAction.valueOf(actionStr.toUpperCase()); } catch (Exception e) { action = Objective.ObjectiveAction.ITEM; }
                    String check = o.has("actionCheck") ? o.get("actionCheck").getAsString() : "";
                    if (id <= 0) id = list.size() + 1;
                    list.add(new Objective(id, internal, display, mat, action, check));
                    if (id > maxId) maxId = id;
                }
                // reassign ids if any duplicates? keep as is assuming file is valid
                this.objectivePool = java.util.Collections.unmodifiableList(list);
            } else {
                this.objectivePool = java.util.Collections.unmodifiableList(buildDefaultObjectivePool());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load objectives.json: " + e.getMessage());
            this.objectivePool = java.util.Collections.unmodifiableList(buildDefaultObjectivePool());
        }
    }

    private void savePool(List<Objective> list) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(poolFile), StandardCharsets.UTF_8)) {
            JsonArray arr = new JsonArray();
            for (Objective o : list) {
                JsonObject jo = new JsonObject();
                jo.addProperty("id", o.getId());
                jo.addProperty("internalName", o.getInternalName());
                jo.addProperty("displayName", o.getDisplayName());
                jo.addProperty("item", "minecraft:" + o.getItem().name().toLowerCase());
                jo.addProperty("action", o.getAction().name());
                jo.addProperty("actionCheck", o.getActionCheck());
                arr.add(jo);
            }
            new GsonBuilder().setPrettyPrinting().create().toJson(arr, w);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save default objectives.json: " + e.getMessage());
        }
    }

    private void loadState() {
        try {
            if (!stateFile.exists()) return;
            String json = new String(java.nio.file.Files.readAllBytes(stateFile.toPath()), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            gameObjectiveIds.clear();
            teamObjectiveStatus.clear();
            if (root.has("games")) {
                JsonObject games = root.getAsJsonObject("games");
                for (String gid : games.keySet()) {
                    List<Integer> ids = new ArrayList<>();
                    for (JsonElement el : games.getAsJsonArray(gid)) ids.add(el.getAsInt());
                    gameObjectiveIds.put(gid, java.util.Collections.unmodifiableList(ids));
                }
            }
            if (root.has("teamStatus")) {
                JsonObject ts = root.getAsJsonObject("teamStatus");
                for (String teamId : ts.keySet()) {
                    java.util.concurrent.ConcurrentMap<Integer, Objective.ObjectiveStatus> map = new java.util.concurrent.ConcurrentHashMap<>();
                    JsonObject m = ts.getAsJsonObject(teamId);
                    for (String objIdStr : m.keySet()) {
                        try {
                            int oid = Integer.parseInt(objIdStr);
                            Objective.ObjectiveStatus st = Objective.ObjectiveStatus.valueOf(m.get(objIdStr).getAsString());
                            map.put(oid, st);
                        } catch (Exception ignored) {}
                    }
                    teamObjectiveStatus.put(teamId, map);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load objectives_state.json: " + e.getMessage());
        }
    }

    private synchronized void saveStateQuiet() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            JsonObject root = new JsonObject();
            JsonObject games = new JsonObject();
            for (Map.Entry<String, List<Integer>> e : gameObjectiveIds.entrySet()) {
                JsonArray arr = new JsonArray();
                for (Integer id : e.getValue()) arr.add(id);
                games.add(e.getKey(), arr);
            }
            root.add("games", games);
            JsonObject ts = new JsonObject();
            for (Map.Entry<String, java.util.concurrent.ConcurrentMap<Integer, Objective.ObjectiveStatus>> e : teamObjectiveStatus.entrySet()) {
                JsonObject m = new JsonObject();
                for (Map.Entry<Integer, Objective.ObjectiveStatus> st : e.getValue().entrySet()) {
                    m.addProperty(String.valueOf(st.getKey()), st.getValue().name());
                }
                ts.add(e.getKey(), m);
            }
            root.add("teamStatus", ts);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(stateFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save objectives_state.json: " + e.getMessage());
        }
    }
    
    private List<Objective> buildDefaultObjectivePool() {
        List<Objective> list = new ArrayList<>();
        int id = 1;
        // ITEM crafts/collects
        list.add(new Objective(id++, "craft_wooden_pickaxe", "Craft une pioche en bois", Material.WOODEN_PICKAXE, Objective.ObjectiveAction.ITEM, "WOODEN_PICKAXE"));
        list.add(new Objective(id++, "craft_furnace", "Fabrique un four", Material.FURNACE, Objective.ObjectiveAction.ITEM, "FURNACE"));
        list.add(new Objective(id++, "get_iron_ingot", "Obtiens un lingot de fer", Material.IRON_INGOT, Objective.ObjectiveAction.ITEM, "IRON_INGOT"));
        list.add(new Objective(id++, "craft_bucket", "Fabrique un seau", Material.BUCKET, Objective.ObjectiveAction.ITEM, "BUCKET"));
        list.add(new Objective(id++, "craft_shield", "Fabrique un bouclier", Material.SHIELD, Objective.ObjectiveAction.ITEM, "SHIELD"));
        list.add(new Objective(id++, "craft_bread", "Fais du pain", Material.BREAD, Objective.ObjectiveAction.ITEM, "BREAD"));
        list.add(new Objective(id++, "craft_bow", "Fabrique un arc", Material.BOW, Objective.ObjectiveAction.ITEM, "BOW"));
        list.add(new Objective(id++, "craft_arrow", "Fabrique des flèches", Material.ARROW, Objective.ObjectiveAction.ITEM, "ARROW"));
        list.add(new Objective(id++, "craft_diamond_sword", "Fabrique une épée en diamant", Material.DIAMOND_SWORD, Objective.ObjectiveAction.ITEM, "DIAMOND_SWORD"));
        list.add(new Objective(id++, "craft_enchanting_table", "Fabrique une table d'enchantement", Material.ENCHANTING_TABLE, Objective.ObjectiveAction.ITEM, "ENCHANTING_TABLE"));
        // KILL objectives
        list.add(new Objective(id++, "kill_player", "Tue un joueur", Material.IRON_SWORD, Objective.ObjectiveAction.KILL, "PLAYER"));
        // VANILLA advancements
        list.add(new Objective(id++, "adv_mine_stone", "Avancement: Extraire de la pierre", Material.STONE_PICKAXE, Objective.ObjectiveAction.OBJECTIVE, "minecraft:story/mine_stone"));
        list.add(new Objective(id++, "adv_bake_bread", "Avancement: Pain au four", Material.BREAD, Objective.ObjectiveAction.OBJECTIVE, "minecraft:husbandry/bread"));
        list.add(new Objective(id++, "adv_iron_tools", "Avancement: Outils en fer", Material.IRON_PICKAXE, Objective.ObjectiveAction.OBJECTIVE, "minecraft:story/iron_tools"));
        list.add(new Objective(id++, "adv_enter_nether", "Avancement: Dans le Nether", Material.NETHERRACK, Objective.ObjectiveAction.OBJECTIVE, "minecraft:story/enter_the_nether"));
        list.add(new Objective(id++, "adv_form_obsidian", "Avancement: Obsidienne", Material.OBSIDIAN, Objective.ObjectiveAction.OBJECTIVE, "minecraft:story/form_obsidian"));
        list.add(new Objective(id++, "adv_enchant_item", "Avancement: Enchanter un objet", Material.ENCHANTING_TABLE, Objective.ObjectiveAction.OBJECTIVE, "minecraft:story/enchant_item"));
        // Add a few more common items to reach >25
        list.add(new Objective(id++, "craft_golden_apple", "Fabrique une pomme dorée", Material.GOLDEN_APPLE, Objective.ObjectiveAction.ITEM, "GOLDEN_APPLE"));
        list.add(new Objective(id++, "craft_firework", "Fabrique une fusée", Material.FIREWORK_ROCKET, Objective.ObjectiveAction.ITEM, "FIREWORK_ROCKET"));
        list.add(new Objective(id++, "get_ender_pearl", "Obtiens une perle de l'Ender", Material.ENDER_PEARL, Objective.ObjectiveAction.ITEM, "ENDER_PEARL"));
        list.add(new Objective(id++, "craft_book", "Fabrique un livre", Material.BOOK, Objective.ObjectiveAction.ITEM, "BOOK"));
        list.add(new Objective(id++, "craft_bed", "Fabrique un lit", Material.RED_BED, Objective.ObjectiveAction.ITEM, "RED_BED"));
        list.add(new Objective(id++, "craft_compass", "Fabrique une boussole", Material.COMPASS, Objective.ObjectiveAction.ITEM, "COMPASS"));
        list.add(new Objective(id++, "craft_map", "Fabrique une carte vierge", Material.FILLED_MAP, Objective.ObjectiveAction.ITEM, "MAP"));
        list.add(new Objective(id++, "craft_torch", "Fabrique des torches", Material.TORCH, Objective.ObjectiveAction.ITEM, "TORCH"));
        list.add(new Objective(id++, "craft_anvil", "Fabrique une enclume", Material.ANVIL, Objective.ObjectiveAction.ITEM, "ANVIL"));
        list.add(new Objective(id++, "craft_crossbow", "Fabrique une arbalète", Material.CROSSBOW, Objective.ObjectiveAction.ITEM, "CROSSBOW"));
        list.add(new Objective(id++, "craft_shears", "Fabrique des cisailles", Material.SHEARS, Objective.ObjectiveAction.ITEM, "SHEARS"));
        list.add(new Objective(id++, "craft_dispenser", "Fabrique un distributeur", Material.DISPENSER, Objective.ObjectiveAction.ITEM, "DISPENSER"));
        list.add(new Objective(id++, "craft_piston", "Fabrique un piston", Material.PISTON, Objective.ObjectiveAction.ITEM, "PISTON"));
        list.add(new Objective(id++, "craft_tnt", "Fabrique de la TNT", Material.TNT, Objective.ObjectiveAction.ITEM, "TNT"));
        return list;
    }
    
    public List<Objective> getGameObjectives(String gameId) {
        List<Integer> ids = gameObjectiveIds.get(gameId);
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        Map<Integer, Objective> byId = new HashMap<>();
        for (Objective o : objectivePool) byId.put(o.getId(), o);
        List<Objective> res = new ArrayList<>();
        for (Integer id : ids) {
            Objective o = byId.get(id);
            if (o != null) res.add(o);
        }
        return res;
    }
    
    public Objective getObjective(String gameId, int objectiveId) {
        List<Objective> list = getGameObjectives(gameId);
        for (Objective o : list) if (o.getId() == objectiveId) return o;
        return null;
    }

    public Objective getObjectiveByText(String gameId, String textIdOrName) {
        if (textIdOrName == null) return null;
        String q = textIdOrName.trim().toLowerCase(java.util.Locale.ROOT);
        for (Objective o : getGameObjectives(gameId)) {
            if (o.getInternalName() != null && o.getInternalName().equalsIgnoreCase(q)) return o;
            if (o.getDisplayName() != null && o.getDisplayName().equalsIgnoreCase(q)) return o;
        }
        return null;
    }
    
    public Objective.ObjectiveStatus getObjectiveStatus(String teamId, int objectiveId) {
        Map<Integer, Objective.ObjectiveStatus> teamStatus = teamObjectiveStatus.get(teamId);
        return teamStatus != null ? teamStatus.getOrDefault(objectiveId, Objective.ObjectiveStatus.PENDING) 
            : Objective.ObjectiveStatus.PENDING;
    }

    public String getStatusSummary(String gameId, String teamId) {
        java.util.List<Objective> list = getGameObjectives(gameId);
        StringBuilder ok = new StringBuilder("§aValidés: ");
        StringBuilder no = new StringBuilder("§cRefusés: ");
        boolean hasOk = false, hasNo = false;
        for (Objective o : list) {
            Objective.ObjectiveStatus st = getObjectiveStatus(teamId, o.getId());
            String entry = o.getInternalName() + "§7(§f" + o.getDisplayName() + "§7)§r";
            if (st == Objective.ObjectiveStatus.SUCCESS) { if (hasOk) ok.append(", "); ok.append(entry); hasOk = true; }
            if (st == Objective.ObjectiveStatus.REJECTED) { if (hasNo) no.append(", "); no.append(entry); hasNo = true; }
        }
        if (!hasOk) ok.append("§7aucun");
        if (!hasNo) no.append("§7aucun");
        return ok + "\n" + no;
    }
    
    public String generateBingoGrid(String gameId, String teamId) {
        List<Objective> objectives = getGameObjectives(gameId);
        if (objectives == null) objectives = java.util.Collections.emptyList();
        
        StringBuilder grid = new StringBuilder();
        for (int row = 0; row < 5; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                Objective obj = index < objectives.size() ? objectives.get(index) : null;
                Objective.ObjectiveStatus status = (obj != null) ? getObjectiveStatus(teamId, obj.getId()) : Objective.ObjectiveStatus.PENDING;

                // Determine icon: replace with BARRIER when completed, keep original otherwise
                String icon;
                if (obj == null) {
                    icon = "  ";
                } else if (status == Objective.ObjectiveStatus.SUCCESS) {
                    icon = IconUtil.getIcon(org.bukkit.Material.BARRIER);
                } else {
                    icon = IconUtil.getIcon(obj.getItem());
                }

                // Force a brighter icon color (white) for visibility and make it bold
                String cleaned = icon.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
                String coloredIcon = "§l§f" + cleaned;

                // Append icon without colored background squares
                line.append(coloredIcon);
                if (col < 4) line.append(" §8│ ");
            }
            // Center each line to a fixed width for TAB alignment
            String centered = fr.clashdesecoles.bingo.utils.TextUtil.center(line.toString(), 36);
            grid.append(centered);
            if (row < 4) grid.append("\n");
        }
        return grid.toString();
    }

    private Material parseMaterial(String item) {
        if (item == null) return Material.BARRIER;
        String s = item.toUpperCase(Locale.ROOT);
        if (s.startsWith("MINECRAFT:")) s = s.substring("MINECRAFT:".length());
        try {
            return Material.valueOf(s);
        } catch (Exception e) {
            // try lower
            try { return Material.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Exception ex) { return Material.BARRIER; }
        }
    }

    public void saveNow() { saveStateQuiet(); }
}
