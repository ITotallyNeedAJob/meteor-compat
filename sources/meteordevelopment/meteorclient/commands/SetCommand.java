package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.command.CommandSource;

/**
 * COMPAT Phase 2: edits a module's setting from chat.
 * Replaces the ClickGUI setting widgets (not ported) for headless use.
 * Usage: .set <module> <setting> <value>
 */
public class SetCommand extends Command {
    public SetCommand() {
        super("set", "Sets a module's setting value.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.create()).then(argument("setting", StringArgumentType.word()).then(argument("value", StringArgumentType.greedyString()).executes(context -> {
            Module m = ModuleArgumentType.get(context);
            String settingName = StringArgumentType.getString(context, "setting");
            String value = StringArgumentType.getString(context, "value");

            Setting<?> setting = m.settings.get(settingName);
            if (setting == null) {
                error("Setting '%s' doesn't exist in module '%s'.", settingName, m.name);
                return 0;
            }

            if (setting.parse(value)) {
                info("%s = %s", setting.name, setting.get());
            } else {
                error("Invalid value '%s' for setting '%s' (current: %s).", value, setting.name, setting.get());
            }

            return SINGLE_SUCCESS;
        }))));
    }
}
