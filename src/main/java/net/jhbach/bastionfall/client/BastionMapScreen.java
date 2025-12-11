package net.jhbach.bastionfall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class BastionMapScreen extends Screen {
    private final BlockPos bastionPos;
    private final int minChunkX;
    private final int minChunkZ;
    private final int sizeX;
    private final int sizeZ;
    private final boolean[] claimed;

    private ResourceLocation mapTexture;
    private int textureWidth;
    private int textureHeight;

    public BastionMapScreen(BlockPos pos, int minChunkX, int minChunkZ, int sizeX, int sizeZ, boolean[] claimed) {
        super(Component.literal("Bastion Map"));
        this.bastionPos = pos;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.claimed = claimed;
    }

    @Override
    protected void init() {
        super.init();
        generateMapTexture();
    }

    private void generateMapTexture() {
        textureWidth = sizeX * 16;
        textureHeight = sizeZ * 16;
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        DynamicTexture dyn = new DynamicTexture(textureWidth, textureHeight, true);
        NativeImage img = dyn.getPixels();
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        int worldXStart = minChunkX * 16;
        int worldZStart = minChunkZ * 16;
        for (int dx = 0; dx < textureWidth; dx++) {
            for (int dz = 0; dz < textureHeight; dz++) {
                int worldX = worldXStart + dx;
                int worldZ = worldZStart + dz;
                BlockPos topPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(worldX, 0, worldZ)).below();
                BlockState state = level.getBlockState(topPos);
                int color = state.getMapColor(level, topPos).col | 0xFF000000;
                img.setPixelRGBA(dx, dz, color);
            }
        }
        dyn.upload();
        mapTexture = tm.register("bastion_map_" + bastionPos.asLong(), dyn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        if (mapTexture != null) {
            int x = (this.width - textureWidth) / 2;
            int y = (this.height - textureHeight) / 2;
            guiGraphics.blit(mapTexture, x, y, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
            for (int cx = 0; cx < sizeX; cx++) {
                for (int cz = 0; cz < sizeZ; cz++) {
                    int index = cx + cz * sizeX;
                    int col = claimed[index] ? 0x4000FF00 : 0x40FF0000;
                    guiGraphics.fill(x + cx * 16, y + cz * 16, x + (cx + 1) * 16, y + (cz + 1) * 16, col);
                }
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
