package net.jhbach.bastionfall;

import net.jhbach.bastionfall.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {

	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BastionFall.MODID);

	public static final RegistryObject<CreativeModeTab> BASTIONFALL_TAB = CREATIVE_MODE_TABS.register("bastionfall_tab",
			() -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.CLAIM_BLOCK.get()))
					.title(Component.translatable("creativetab.bastionfall_tab"))
					.displayItems((itemDisplayParameters, output) -> {
						output.accept(ModBlocks.CLAIM_BLOCK.get());
					})
					.build());

	public static void register(IEventBus eventBus) {
		CREATIVE_MODE_TABS.register(eventBus);
	}
}

