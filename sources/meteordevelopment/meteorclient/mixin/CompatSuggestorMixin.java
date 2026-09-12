package meteordevelopment.meteorclient.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

/**
 * COMPAT autocomplete: brigadier suggestions for the meteor prefix, rendered
 * through the vanilla suggestion window. Mirrors vanilla refresh() logic but
 * parses with Commands.DISPATCHER: the reader holds the FULL field text with
 * the cursor advanced past the prefix, so suggestion ranges stay aligned with
 * the text field and vanilla's window/completion machinery works unmodified.
 */
@Mixin(ChatInputSuggestor.class)
public abstract class CompatSuggestorMixin {
    @Shadow
    TextFieldWidget textField;

    @Shadow
    private ParseResults<ClientCommandSource> parse;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    boolean completingSuggestions;

    @Shadow
    private void showCommandSuggestions() {}

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void compat$refresh(CallbackInfo ci) {
        String text = textField.getText();
        String prefix = Config.get().prefix.get();

        if (!text.startsWith(prefix)) return;
        // Same split as command dispatch: LB-owned roots (.binds, .config,
        // .toggle, ...) fall through so LB's own suggestor provides its
        // completions instead of meteor's (empty) ones.
        if (Commands.ownedByLiquidBounce(text, prefix)) return;
        if (MeteorClient.mc.player == null || MeteorClient.mc.getNetworkHandler() == null) return;

        StringReader reader = new StringReader(text);
        reader.setCursor(Math.min(prefix.length(), text.length()));

        ParseResults<CommandSource> ourParse = Commands.DISPATCHER.parse(reader, (CommandSource) MeteorClient.mc.getNetworkHandler().getCommandSource());

        // Erased at runtime: vanilla only calls getReader()/getExceptions()/getContext() on it.
        @SuppressWarnings({"unchecked", "rawtypes"})
        ParseResults<ClientCommandSource> typed = (ParseResults) ourParse;
        parse = typed;

        completingSuggestions = true;
        pendingSuggestions = Commands.DISPATCHER.getCompletionSuggestions(ourParse, textField.getCursor());
        pendingSuggestions.thenRun(this::showCommandSuggestions);

        ci.cancel();
    }
}
