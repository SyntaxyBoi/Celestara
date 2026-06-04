package moth.celestara.client.mixin;

import moth.celestara.event.CelestaraEvents;
import net.minecraft.server.command.TimeCommand;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {
    @Redirect(
            method = "executeSet",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setTimeOfDay(J)V")
    )
    private static void celestara$setDayTimeWithoutResettingDay(ServerWorld world, long requestedTime) {
        long currentDay = CelestaraEvents.dayIndex(world.getTimeOfDay());
        long requestedPhase = Math.floorMod(requestedTime, CelestaraEvents.DAY_LENGTH_TICKS);
        world.setTimeOfDay(currentDay * CelestaraEvents.DAY_LENGTH_TICKS + requestedPhase);
    }
}
