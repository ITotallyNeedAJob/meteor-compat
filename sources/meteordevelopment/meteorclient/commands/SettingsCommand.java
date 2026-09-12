package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.command.CommandSource;

/**
 * COMPAT Phase 2: lists a module's settings with current values.
 * Replaces the ClickGUI setting panes (not ported) for headless use.
 * Usage: .settings <module>
 */
public class SettingsCommand extends Command {
    public SettingsCommand() {
        super("settings", "Lists a module's settings and values.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.create()).executes(context -> {
            Module m = ModuleArgumentType.get(context);

            for (SettingGroup group : m.settings.groups) {
                info("(highlight)%s(default):", group.name);
                for (Setting<?> setting : group) {
                    Object value = setting.get();
                    info("  %s = %s", setting.name, value != null ? value.toString() : "null");
                }
            }

            return SINGLE_SUCCESS;
        }));
    }
}
