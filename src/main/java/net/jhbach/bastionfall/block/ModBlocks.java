package net.jhbach.bastionfall.block;

import net.jhbach.bastionfall.BastionFall;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static net.jhbach.bastionfall.item.ModItems.ITEMS;

public class ModBlocks {

	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(ForgeRegistries.BLOCKS, BastionFall.MODID);

	public static final RegistryObject<Block> CLAIM_BLOCK = registerBlock("claim_block",
			() -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
					.strength(5.0f)
					.noOcclusion()
			));

	private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
		RegistryObject<T> toReturn = BLOCKS.register(name, block);
		registerBlockItem(name, toReturn);
		return toReturn;
	}

	private static <T extends Block>RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
	}

	public static void register(IEventBus eventBus) {
		BLOCKS.register(eventBus);
	}

}
