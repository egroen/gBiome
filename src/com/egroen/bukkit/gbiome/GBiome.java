package com.egroen.bukkit.gbiome;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;

public class GBiome extends JavaPlugin {
    public static GBiome plugin;
    private WorldEditPlugin worldedit = null;
    
    @Override
    public void onEnable() {
        plugin = this;
        worldedit = (WorldEditPlugin)getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldedit == null) {
            getLogger().severe("WorldEdit has not been found!");
        }
        //getConfig().options().copyDefaults(true);
        //saveConfig();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player = null;
        if (sender instanceof Player) player = (Player)sender;      // Should always be since permissions disallow console
        
        if (cmd.getLabel().equalsIgnoreCase("gbiome")) {
            if (args.length == 0) return false;                     // Just quit...
            
            if (args[0].equalsIgnoreCase("biometypes")) {           // Process biometypes
                sender.sendMessage(ChatColor.BLUE+"The following biomes are known:");
                
                Biome[] biomes = Biome.values();                    // Looping trough it, with sub-loop for steps by 3.
                for (int i=0; i<biomes.length;) {
                    StringBuilder message = new StringBuilder();
                    for (int x=0; i+x < biomes.length && x < 3; x++) {
                        if (x != 0) message.append("    ");
                        message.append(biomes[i+x].toString());
                    }
                    sender.sendMessage(ChatColor.AQUA+message.toString());
                    i += 3;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("read")) {          // Process read
                if (player == null) { sender.sendMessage(ChatColor.RED+"You must be an player to do this!"); return true; }
                
                Region region;
                try {
                    region = getSingleHeightRegion(player);
                } catch (IncompleteRegionException e) {
                    player.sendMessage(ChatColor.RED+"The region has not been completed!");
                    return true;
                }
                
                // Create a map of all biomes in the region.
                HashMap<Biome, Integer> biomeCount = new HashMap<Biome, Integer>();
                World world = Bukkit.getWorld(region.getWorld().getName());
                for (BlockVector bv : region) {
                    Biome biome = world.getBiome(bv.getBlockX(), bv.getBlockZ());
                    biomeCount.put(biome, biomeCount.containsKey(biome) ? biomeCount.get(biome)+1 : 1);
                }
                
                // Send list of biomes to player
                int selectionSize = region.getArea();
                sender.sendMessage(ChatColor.BLUE+"The area exists out of "+selectionSize+" blocks containing:");
                for (Biome biome : biomeCount.keySet()) {
                    int content = biomeCount.get(biome);
                    sender.sendMessage(ChatColor.AQUA+" - "+biome.toString()+": "+content+" ("+round(((double)content/(double)selectionSize*100.0), 3)+"%)");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("set")) {
                if (player == null) { sender.sendMessage(ChatColor.RED+"You must be an player to do this!"); return true; }
                if (args.length != 2) return false;             // We need the new type.. just give usage.
                
                // get biome & check existence.
                Biome biome = Biome.valueOf(args[1].toUpperCase());
                if (biome == null) { sender.sendMessage(ChatColor.RED+"Biome has not been found!"); return true; }
                
                // iterate trough blocks and set biome
                Region region;
                try {
                    region = getSingleHeightRegion(player);
                } catch (IncompleteRegionException e) {
                    player.sendMessage(ChatColor.RED+"The region has not been completed!");
                    return true;
                }
                World world = Bukkit.getWorld(region.getWorld().getName());
                for (BlockVector bv : region) {
                    try { world.setBiome(bv.getBlockX(), bv.getBlockZ(), biome); }
                    catch (NullPointerException ex) { sender.sendMessage("Biome does not work in this world!"); return true; }
                }
                
                // DONE!
                player.sendMessage(ChatColor.GREEN+"Done!");
                return true;
            }
        }
        return false;
    }
    
    private Region getSingleHeightRegion(Player player) throws IncompleteRegionException {
        LocalSession WESes = worldedit.getSession(player);    // Get player session from worldedit
        if (WESes == null) return null;
        
        Region region = WESes.getSelection(WESes.getSelectionWorld());

        // Setting region to 1 block height so we do not iterate trough the same x/z multiple times.
        if (region.getHeight() != 1) {
            try {
                region.contract(new Vector(0, region.getHeight()-1, 0));
            } catch (RegionOperationException e) {
                // Then just do it multi-level.
            }
        }
        return region;
    }
    
    private static double round(double value, int decs) {
        value = value * Math.pow(10, decs);
        value = Math.round(value);
        value = value / Math.pow(10, decs);
        return value;
    }
}
