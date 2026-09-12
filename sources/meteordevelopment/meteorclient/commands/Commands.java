/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    private static final Logger LOG = LogUtils.getLogger();
    public static final List<Command> COMMANDS = new ArrayList<>();
    public static CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();

    // COMPAT Phase 0: no built-in commands. Addons register via Commands.add().
    // Phase 1 wires chat dispatch + dispatcher rebuild subscription.

    public static void add(Command command) {
        COMMANDS.removeIf(existing -> existing.getName().equals(command.getName()));
        COMMANDS.add(command);
    }

    public static void dispatch(String message) throws CommandSyntaxException {
        DISPATCHER.execute(message, mc.getNetworkHandler().getCommandSource());
    }

    /**
     * COMPAT: shared chat entry point so meteor commands run ALONGSIDE other
     * same-prefix systems (LiquidBounce also uses "."). Returns true only when
     * meteor actually handled the message; false lets the caller pass it on
     * (LB / vanilla) untouched. Unknown commands and bad args fall through
     * silently so shadowed names (e.g. ".toggle") resolve on the other side.
     */
    public static boolean tryRun(String message) {
        String prefix = Config.get().prefix.get();
        if (!message.startsWith(prefix)) return false;

        // COMPAT merged bridge: LiquidBounce owns its command names. When both
        // clients share this prefix and LB defines the root word (.binds,
        // .config, .toggle, ...), fall through so LB handles it instead of
        // meteor shadowing it. Meteor-only names still run here.
        if (ownedByLiquidBounce(message, prefix)) return false;

        try {
            dispatch(message.substring(prefix.length()));
        } catch (CommandSyntaxException e) {
            return false;
        } catch (RuntimeException e) {
            LOG.error("[meteor-compat] Command '{}' threw:", message, e);
            ChatUtils.error("Internal error: " + e);
            return true;
        }

        MeteorClient.mc.inGameHud.getChatHud().addToMessageHistory(message);
        return true;
    }

    public static Command get(String name) {
        for (Command command : COMMANDS) {
            if (command.getName().equals(name)) {
                return command;
            }
        }

        return null;
    }

    // Soft LiquidBounce link (reflection only, cached): compat must still boot
    // when LB is absent, so no compile-time reference is used here even though
    // the bridge now ships in this same mod.
    private static volatile Set<String> lbNames = null;
    private static volatile long lbNamesAt = 0;

    // Single choke point for the soft LB link: loads Class + INSTANCE, null on
    // any failure (LB absent or API drift). Callers keep their own fallbacks.
    private static Object lbInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getField("INSTANCE").get(null);
        }
        catch (Throwable t) {
            return null;
        }
    }

    // Public for CompatSuggestorMixin: suggestions follow the same ownership.
    public static boolean ownedByLiquidBounce(String message, String meteorPrefix) {
        try {
            if (!FabricLoader.getInstance().isModLoaded("liquidbounce")) return false;

            // LB's live prefix; a clash is only possible when both use the same one.
            Object settings = lbInstance("net.ccbluex.liquidbounce.features.command.CommandManager$GlobalSettings");
            if (settings == null) return false;
            String lbPrefix = (String) settings.getClass().getMethod("getPrefix").invoke(settings);
            if (lbPrefix == null || !lbPrefix.equals(meteorPrefix)) return false;
            if (!message.startsWith(lbPrefix)) return false;

            String rest = message.substring(lbPrefix.length());
            int end = 0;
            while (end < rest.length() && !Character.isWhitespace(rest.charAt(end))) end++;
            if (end == 0) return false;
            String root = rest.substring(0, end).toLowerCase(Locale.ROOT);

            long now = System.currentTimeMillis();
            Set<String> names = lbNames;
            if (names == null || now - lbNamesAt > 5000) {
                names = readLiquidBounceCommandNames();
                lbNames = names;
                lbNamesAt = now;
            }
            if (!names.contains(root)) return false;

            // COMPAT: roots both clients define that take a module name
            // (.bind, .toggle). Route to Meteor only when the target exists
            // solely on the Meteor side (e.g. `.bind auto-tnt`). True clashes
            // (name on both sides) stay LB-owned.
            if (MODULE_ROUTED_ROOTS.contains(root)) {
                String target = firstArg(rest, end);
                if (target != null && meteorOwnsModule(target)) return false;
            }
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private static final Set<String> MODULE_ROUTED_ROOTS = Set.of("bind", "toggle");

    private static String firstArg(String rest, int rootEnd) {
        int i = rootEnd;
        while (i < rest.length() && Character.isWhitespace(rest.charAt(i))) i++;
        if (i >= rest.length()) return null;
        int end = i;
        while (end < rest.length() && !Character.isWhitespace(rest.charAt(end))) end++;
        return rest.substring(i, end);
    }

    private static boolean meteorOwnsModule(String name) {
        try {
            Modules modules = Modules.get();
            if (modules == null || modules.get(name) == null) return false;
            return !lbOwnsModule(name);
        }
        catch (Throwable t) {
            return false;
        }
    }

    // Soft LB link (same reflection-only pattern as the command names above).
    // Bridge proxies (com.meteorbridge.*) mirror Meteor modules into LB's
    // manager, so they are excluded: only genuine LB modules count.
    private static boolean lbOwnsModule(String name) {
        try {
            Object manager = lbInstance("net.ccbluex.liquidbounce.features.module.ModuleManager");
            if (manager == null) return true;
            Object modules = manager.getClass().getMethod("getModules").invoke(manager);
            if (modules instanceof java.util.Collection<?> list) {
                for (Object mod : list) {
                    if (mod == null || mod.getClass().getName().startsWith("com.meteorbridge.")) continue;
                    Object modName = mod.getClass().getMethod("getName").invoke(mod);
                    if (modName != null && name.equalsIgnoreCase(modName.toString())) return true;
                }
            }
        }
        catch (Throwable t) {
            // Unreadable = treat as LB-owned so a true clash can't leak to Meteor.
            return true;
        }
        return false;
    }

    private static Set<String> readLiquidBounceCommandNames() {
        Set<String> names = new HashSet<>();
        try {
            Object manager = lbInstance("net.ccbluex.liquidbounce.features.command.CommandManager");
            if (manager == null) return names;
            Class<?> commandClass = Class.forName("net.ccbluex.liquidbounce.features.command.Command");
            java.lang.reflect.Method getName = commandClass.getMethod("getName");
            java.lang.reflect.Method getAliases = commandClass.getMethod("getAliases");

            for (Object command : (Iterable<?>) manager) {
                Object name = getName.invoke(command);
                if (name != null) names.add(name.toString().toLowerCase(Locale.ROOT));
                Object aliases = getAliases.invoke(command);
                if (aliases instanceof Iterable) {
                    for (Object alias : (Iterable<?>) aliases) {
                        if (alias != null) names.add(alias.toString().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        catch (Throwable t) {
            // Whatever was collected (possibly empty = meteor handles everything).
        }
        return names;
    }

    /**
     * Argument types that rely on Minecraft registries access those registries through a {@link CommandRegistryAccess}
     * object. Since dynamic registries are specific to each server, we need to make a new CommandRegistryAccess object
     * every time we join a server.
     * <p>
     * The command tree and by extension the {@link CommandDispatcher} also have to be rebuilt because:
     * <ol>
     * <li>Argument types that require registries use a registry wrapper object that is created and stored in the
     *     argument type objects when the command tree is built.
     * <li>Registry entries and keys are compared using referential equality. Even if the data encoded is the same,
     *     registry wrapper objects' dynamic data becomes stale after joining another server.
     * <li>The CommandDispatcher's node merging only adds missing children, it cannot replace stale argument type
     *     objects.
     * </ol>
     *
     * @author Crosby
     */
    @EventHandler
    private static void onJoin(GameJoinedEvent event) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        Command.REGISTRY_ACCESS = CommandRegistryAccess.of(networkHandler.getRegistryManager(), networkHandler.getEnabledFeatures());

        DISPATCHER = new CommandDispatcher<>();
        for (Command command : COMMANDS) {
            command.registerTo(DISPATCHER);
        }
    }
}
