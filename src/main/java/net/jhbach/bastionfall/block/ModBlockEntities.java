package net.jhbach.bastionfall.block;

import net.jhbach.bastionfall.BastionFall;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = BastionFall.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BastionFall.MODID);

	public static final RegistryObject<BlockEntityType<ClaimBlockEntity>> CLAIM_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("claim_block",
					() -> BlockEntityType.Builder.of(ClaimBlockEntity::new,
					ModBlocks.CLAIM_BLOCK_TIER_1.get(),
					ModBlocks.CLAIM_BLOCK_TIER_2.get())
							.build(null));

	public static void register(IEventBus eventBus) {
		BLOCK_ENTITIES.register(eventBus);
	}
}
