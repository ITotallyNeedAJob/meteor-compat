package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * COMPAT 26.2: brigadier autocomplete for the meteor prefix, rendered through
 * the vanilla suggestion window. Verbatim upstream port of
 * CommandSuggestionsMixin (stage-2 adds the LB-owned-roots fallthrough).
 */
@Mixin(CommandSuggestions.class)
public abstract class CompatSuggestorMixin {
    @Shadow
    private @Nullable ParseResults<ClientSuggestionProvider> currentParse;

    @Shadow
    @Final
    private EditBox input;

    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    private boolean keepSuggestions;

    @Shadow
    private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    protected abstract void updateUsageInfo(ParseResults<ClientSuggestionProvider> currentParse, Suggestions suggestions);

    @Inject(method = "updateCommandInfo",
        at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false),
        cancellable = true
    )
    private void compat$onRefresh(CallbackInfo ci, @Local(name = "reader") StringReader reader) {
        String prefix = Config.get().prefix.get();
        int length = prefix.length();

        if (!reader.canRead(length) || !reader.getString().startsWith(prefix, reader.getCursor())) return;
        // Same split as command dispatch: LB-owned roots (.binds, .config,
        // .toggle, ...) fall through so LB's own suggestor provides its
        // completions instead of meteor's (empty) ones.
        if (Commands.ownedByLiquidBounce(reader.getString(), prefix)) return;
        if (MeteorClient.mc.player == null || MeteorClient.mc.getConnection() == null) return;
        reader.setCursor(reader.getCursor() + length);

        if (this.currentParse == null) {
            this.currentParse = Commands.DISPATCHER.parse(reader, mc.getConnection().getSuggestionsProvider());
        }

        int cursor = input.getCursorPosition();
        if (cursor >= length && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = Commands.DISPATCHER.getCompletionSuggestions(this.currentParse, cursor);
            this.pendingSuggestions.thenAccept(suggestionResult -> {
                if (this.pendingSuggestions.isDone()) {
                    this.updateUsageInfo(this.currentParse, suggestionResult);
                }
            });
        }

        ci.cancel();
    }
}
