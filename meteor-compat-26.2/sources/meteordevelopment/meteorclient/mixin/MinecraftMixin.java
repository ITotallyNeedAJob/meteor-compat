/*
 * Compat port of upstream MinecraftMixin for MC 26.2 (Mojmap).
 * CRITICAL: onInit performs the SECOND MeteorClient.onInitializeClient() call
 * (full init incl. mc = Minecraft.getInstance()); without this mixin Meteor
 * never initializes and every event shim NPEs on MeteorClient.mc.
 * Module-coupled hooks (FastUse/Multitask/HighwayBuilder/ESP/InventoryTweaks/
 * Freecam) are dropped: those default modules are stripped in the pure-host
 * build and addons re-provide their own equivalents.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.DoAttackEvent;
import meteordevelopment.meteorclient.events.entity.player.DoItemUseEvent;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ResolutionChangedEvent;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IMinecraft;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.CPSUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.meteordev.starscript.Script;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(value = Minecraft.class, priority = 1001)
public abstract class MinecraftMixin implements IMinecraft {
    @Unique
    private boolean startUseItemCalled;
    @Unique
    private boolean rightClick;
    @Unique
    private long lastTime;
    @Unique
    private boolean firstFrame;

    @Shadow
    public ClientLevel level;
    @Shadow
    @Final
    public Options options;

    @Shadow
    protected abstract void startUseItem();

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    protected abstract void continueAttack(boolean down);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MeteorClient.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo ci) {
        OnlinePlayers.update();

        startUseItemCalled = false;

        Profiler.get().push(MeteorClient.MOD_ID + "_pre_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Pre.get());
        Profiler.get().pop();

        if (rightClick && !startUseItemCalled && gameMode != null) startUseItem();
        rightClick = false;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci) {
        Profiler.get().push(MeteorClient.MOD_ID + "_post_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Post.get());
        Profiler.get().pop();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        CPSUtils.onAttack();
        if (MeteorClient.EVENT_BUS.post(DoAttackEvent.get()).isCancelled()) cir.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onStartUseItem(CallbackInfo ci) {
        startUseItemCalled = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean keepResourcePacks, boolean stopSound, CallbackInfo ci) {
        if (level != null) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;"), cancellable = true)
    private void onStartUseItemBeforeHands(CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(DoItemUseEvent.get()).isCancelled()) ci.cancel();
    }

    @ModifyExpressionValue(method = "startUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;", ordinal = 1, opcode = Opcodes.GETFIELD))
    private HitResult startUseItemMinecraftClientCrosshairTargetProxy(HitResult original) {
        return MeteorClient.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(original)).target;
    }

    @ModifyReturnValue(method = "reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private CompletableFuture<Void> onReloadResourcePacksNewCompletableFuture(CompletableFuture<Void> original) {
        return original.thenRun(() -> MeteorClient.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
    }

    @ModifyArg(method = "updateTitle", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setTitle(Ljava/lang/String;)V"))
    private String setTitle(String original) {
        if (Config.get() == null || !Config.get().customWindowTitle.get()) return original;

        String customTitle = Config.get().customWindowTitleText.get();
        Script script = MeteorStarscript.compile(customTitle);

        if (script != null) {
            String title = MeteorStarscript.run(script);
            if (title != null) customTitle = title;
        }

        return customTitle;
    }

    @Inject(method = "resizeGui", at = @At("TAIL"))
    private void onResizeGui(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ResolutionChangedEvent.get());
    }

    // Time delta

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(CallbackInfo ci) {
        long time = System.currentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }

    // Interface

    @Override
    public void meteor$rightClick() {
        rightClick = true;
    }
}
