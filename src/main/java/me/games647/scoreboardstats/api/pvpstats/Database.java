package me.games647.scoreboardstats.api.pvpstats;

import com.avaje.ebean.EbeanServer;
import java.util.HashMap;
import java.util.Map;
import static me.games647.scoreboardstats.ScoreboardStats.getSettings;

public final class Database {

    private static EbeanServer databaseinstance;
    private static Map<String, Cache> cache = new HashMap<String, Cache>();

    public static void setDatabase(final EbeanServer base) {
        Database.databaseinstance = base;
    }

    public static Cache getCache(final String name) {
        return cache.get(name);
    }

    public static void loadAccount(final String name) {
        final PlayerStats stats = databaseinstance.find(PlayerStats.class).where().eq("playername", name).findUnique();

        cache.put(name
                , stats == null ? new Cache() : new Cache(stats.getKills(), stats.getMobkills(), stats.getDeaths(), stats.getKillstreak()));
    }

    public static int getKdr(final String name) {
        final Cache stats = getCache(name);

        return stats.getDeaths() == 0 ? stats.getKills() : (stats.getKills() / stats.getDeaths());
    }

    public static void saveAccount(final String name, final boolean remove) {
        final Cache playercache = cache.get(name);

        if ((playercache == null) || ((playercache.getKills() == 0) && (playercache.getDeaths() == 0) && (playercache.getMob() == 0))) {
            return;
        }

        PlayerStats stats = databaseinstance.find(PlayerStats.class).where().eq("playername", name).findUnique();

        if (stats == null) {
            stats = new PlayerStats();
            stats.setPlayername(name);
        }

        stats.setDeaths(playercache.getDeaths());
        stats.setKills(playercache.getKills());
        stats.setMobkills(playercache.getMob());
        stats.setKillstreak(playercache.getStreak());
        databaseinstance.save(stats);

        if (remove) {
            cache.remove(name);
        }
    }

    public static void saveAll() {
        for (String playername : cache.keySet()) {
            saveAccount(playername, false);
        }

        cache.clear();
    }

    public static Map<String, Integer> getTop() {
        java.util.List<PlayerStats> list;
        final String type = getSettings().getToptype();
        final Map<String, Integer> top = new HashMap<String, Integer>(getSettings().getTopitems());

        if ("%killstreak%".equals(type)) {
            list = databaseinstance.find(PlayerStats.class).orderBy("killstreak desc").setMaxRows(getSettings().getTopitems()).findList();
            for (int i = 0; i < list.size(); i++) {
                final PlayerStats stats = list.get(i);
                top.put(stats.getPlayername(), stats.getKillstreak());
            }
            return top;
        } else if ("%mobkills%".equals(type)) {
            list = databaseinstance.find(PlayerStats.class).orderBy("mobkills desc").setMaxRows(getSettings().getTopitems()).findList();
            for (int i = 0; i < list.size(); i++) {
                final PlayerStats stats = list.get(i);
                top.put(stats.getPlayername(), stats.getMobkills());
            }
            return top;
        } else {
            list = databaseinstance.find(PlayerStats.class).orderBy("kills desc").setMaxRows(getSettings().getTopitems()).findList();
            for (int i = 0; i < list.size(); i++) {
                final PlayerStats stats = list.get(i);
                top.put(stats.getPlayername(), stats.getKills());
            }
            return top;
        }
    }
}
