package meteordevelopment.meteorclient;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.commands.SetCommand;
import meteordevelopment.meteorclient.commands.SettingsCommand;
import meteordevelopment.meteorclient.commands.ToggleCommand;
import meteordevelopment.meteorclient.commands.commands.BindCommand;
import meteordevelopment.meteorclient.commands.commands.BindsCommand;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * meteor-compat Phase 0: minimal Meteor ABI host.
 * Provides the addon-facing Meteor API (modules registry, settings, events bus,
 * commands registry, HUD registry) without built-in modules or GUI.
 * The LiquidBounce bridge (events/modules/UI) lands in Phase 1.
 */
public class MeteorClient implements ClientModInitializer {
    public static final String MOD_ID = "meteor-client";
    public static final Logger LOG = LoggerFactory.getLogger(MeteorClient.class);

    public static MinecraftClient mc;
    public static MeteorClient INSTANCE;
    public static ModMetadata MOD_META;
    public static MeteorAddon ADDON;
    public static final Version VERSION = new Version("1.0.0");
    // COMPAT: upstream reads these from mod metadata (needed by MeteorStarscript).
    public static final String NAME = MOD_ID;
    public static final String BUILD_NUMBER = "";

    public static final IEventBus EVENT_BUS = new EventBus();
    // COMPAT: kill-switch for our custom GL backends (MeteorRenderPipelines +
    // MeshRenderer uploads). true = meteor rendering restored.
    public static final boolean CUSTOM_GL_RENDER = true;
    // COMPAT: own folder (not "meteor-client") so the test host never reads or
    // overwrites real Meteor's config/modules/hud files in the same game dir.
    public static final File FOLDER = FabricLoader.getInstance().getGameDir().resolve("meteor-compat").toFile();

    public static Identifier identifier(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) INSTANCE = this;
        mc = MinecraftClient.getInstance();
        MOD_META = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();

        AddonManager.init();

        // Orbit 0.2.x needs a lambda factory per subscribed package. Upstream registers
        // addon packages only (its own classes are woven by the orbit gradle plugin);
        // compat has no weaving, so register our own package explicitly, before any subscribe.
        EVENT_BUS.registerLambdaFactory("meteordevelopment.meteorclient", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        Categories.init();
        Systems.init();

        // COMPAT: subscribe the module registry. Modules.onKey/onMouseClick
        // drive all module keybind toggles (and onGameJoined/onGameLeft keep
        // active modules subscribed across world switches). Without this,
        // modules work when toggled via commands but binds never trigger.
        EVENT_BUS.subscribe(Modules.get());

        // COMPAT Stage 3: 2D mesh batcher (HUD triangles, future GUI).
        meteordevelopment.meteorclient.renderer.Renderer2D.init();

        // Rebuilds the brigadier dispatcher on join (Phase 1 wires chat dispatch).
        EVENT_BUS.subscribe(Commands.class);
        // COMPAT Phase 1: single built-in command so headless addons can be toggled.
        Commands.add(new ToggleCommand());
        // COMPAT: Meteor bind commands (bind capture + bind list). Reachable
        // for Meteor-only module names via ownership routing in tryRun; LB
        // keeps true clashes and its own modules.
        Commands.add(new BindCommand());
        Commands.add(new BindsCommand());
        // COMPAT Phase 2: chat replacements for the ClickGUI setting panes.
        Commands.add(new SettingsCommand());
        Commands.add(new SetCommand());
        Names.init();
        EChestMemory.init();
        ChatUtils.init();

        // COMPAT addon batch (PLUS needs Starscript): manual @PreInit equivalents
        // (compat has no ReflectInit scanning; order respects dependencies).
        // Executor first: Capes + Accounts loading submit background tasks.
        meteordevelopment.meteorclient.utils.network.MeteorExecutor.init();
        meteordevelopment.meteorclient.utils.network.Capes.init();
        meteordevelopment.meteorclient.pathing.PathManagers.init();
        meteordevelopment.meteorclient.utils.misc.CPSUtils.init();
        meteordevelopment.meteorclient.utils.misc.MeteorStarscript.init();
        meteordevelopment.meteorclient.utils.misc.FakeClientPlayer.init();
        meteordevelopment.meteorclient.utils.Utils.init();
        meteordevelopment.meteorclient.utils.world.BlockUtils.init();
        meteordevelopment.meteorclient.utils.world.BlockIterator.init();
        meteordevelopment.meteorclient.utils.player.Rotations.init();
        meteordevelopment.meteorclient.gui.GuiThemes.init();
        meteordevelopment.meteorclient.gui.tabs.Tabs.init();
        // COMPAT: FullScreenRenderer.init() intentionally not called: upstream's
        // own ReflectInit only scans addon packages, so upstream never runs it
        // either (mesh.begin() needs gameRenderer, which is null at entrypoint).
        // COMPAT: Fonts.refresh() needs the GPU; Fonts retries it on ticks.
        EVENT_BUS.subscribe(meteordevelopment.meteorclient.renderer.Fonts.class);
        // COMPAT: PostProcessShaders allocates GPU textures; lazily inits on first render.

        int count = 0;
        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (addon == ADDON) continue;
            try {
                // COMPAT: obfuscated addons may report an unexpected package (or
                // none); never let factory registration block the addon init.
                String pkg = null;
                try {
                    pkg = addon.getPackage();
                } catch (Throwable t) {
                    LOG.error("meteor-compat: addon '{}' getPackage() failed, skipping lambda factory.", addon.name, t);
                }
                if (pkg != null && !pkg.isEmpty()) {
                    EVENT_BUS.registerLambdaFactory(pkg, (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
                }
                addon.onInitialize();
                count++;
                LOG.info("meteor-compat: initialized addon '{}'.", addon.name);
            } catch (Throwable t) {
                LOG.error("meteor-compat: exception during addon init '{}'.", addon.name, t);
            }
        }

        Modules.get().sortModules();
        Systems.load();

        // COMPAT Phase 2: persist on shutdown (mirrors upstream). GameLeftEvent
        // covers world switches; this covers quitting straight from a world.
        Runtime.getRuntime().addShutdownHook(new Thread(Systems::save));

        LOG.info("meteor-compat Phase 0 initialized: {} addon(s), {} module(s).", count, Modules.get().getCount());
    }
}
