package com.villagerai.mixin;

import com.villagerai.ai.VillagerAssignmentHelper;
import com.villagerai.config.VillagerAIConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "updatePOIOnBlockStateChange", at = @At("TAIL"))
    private void onUpdatePOIOnBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (!VillagerAIConfig.get().assignNearestVillager) {
            return;
        }
        Optional<Holder<PoiType>> newPoi = PoiTypes.forState(newState);
        if (newPoi.isPresent() && !Objects.equals(PoiTypes.forState(oldState), newPoi)) {
            BlockPos immutablePos = pos.immutable();
            ServerLevel level = (ServerLevel) (Object) this;
            level.getServer().execute(() -> {
                VillagerAssignmentHelper.onWorkstationPlaced(level, immutablePos, newState);
            });
        }
    }
}
