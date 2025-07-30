package net.jhbach.bastionfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ClaimBlockEntity extends BlockEntity {
	private UUID owner;

	public ClaimBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CLAIM_BLOCK_ENTITY.get(), pos, state);
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	public UUID getOwner() {
		return this.owner;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (owner != null) {
			tag.putUUID("Owner", owner);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.hasUUID("Owner")) {
			this.owner = tag.getUUID("Owner");
		}
	}
}
