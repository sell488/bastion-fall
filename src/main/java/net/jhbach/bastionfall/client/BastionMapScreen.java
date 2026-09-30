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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

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

    int panelWidth = 320;
    int panelHeight = 200;

    int panelX = (this.width - panelWidth) / 2;
    int panelY = (this.height - panelHeight) / 2;

    int mapWidth = textureWidth;
    int mapHeight = textureHeight;

    int mapX = panelX + 8;
    int mapY = panelY + 8;

    int sideX = mapX + mapWidth + 8;
    int sideY = panelY + 8;
    int sideWidth = panelX + panelWidth - 8 - sideX;
    int sideHeight = panelHeight - 16;

    // viewport and scrolling
    private int viewportWidth = 0;
    private int viewportHeight = 0;
    private int scrollX = 0; // source x offset into texture
    private int scrollY = 0; // source y offset into texture
    private boolean draggingH = false;
    private boolean draggingV = false;
    private int dragOffset = 0;

    // scaling/draw size
    private float drawScale = 1.0f;
    private int mapDrawWidth = 0;
    private int mapDrawHeight = 0;
    private boolean scaledToFit = false;


    private static boolean isIgnoredTopLayer(BlockState state) {
        return state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS) // tall grass, etc.
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CLIMBABLE)          // vines, ladders
                || state.is(Blocks.GRASS)                 // single grass block
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN);
    }


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
        mapHeight = Math.min(mapHeight, panelHeight - 16);
        mapWidth  = Math.min(mapWidth, panelWidth - 110);
    }

    private int adjustColor(int abgr, BlockState state, FluidState fluid) {
        int a = (abgr >> 24) & 0xFF;
        int r = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = abgr & 0xFF;

        boolean isLava = state.is(Blocks.LAVA) || fluid.is(FluidTags.LAVA);
        boolean isNether = state.getTags().anyMatch(tag ->
                tag.location().getNamespace().equals("minecraft") &&
                        tag.location().getPath().contains("nether")
        );

        boolean tooRed = r > g + 40 && r > b + 40;

        if (tooRed && !isLava && !isNether) {
            int gray = (r + g + b) / 3;
            r = (r + gray * 2) / 3;
            g = (g + gray * 2) / 3;
            b = (b + gray * 2) / 3;
        }

        return (a << 24) | (r << 16) | (g << 8) | b;
    }


    private void generateMapTexture() {
        textureWidth = sizeX * 16;
        textureHeight = sizeZ * 16;
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        DynamicTexture dyn = new DynamicTexture(textureWidth, textureHeight, true);
        NativeImage img = dyn.getPixels();
        if (img == null) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        int worldXStart = minChunkX * 16;
        int worldZStart = minChunkZ * 16;
        for (int dx = 0; dx < textureWidth; dx++) {
            for (int dz = 0; dz < textureHeight; dz++) {
                int worldX = worldXStart + dx;
                int worldZ = worldZStart + dz;

                BlockPos surfacePos = level.getHeightmapPos(
                        Heightmap.Types.WORLD_SURFACE,
                        new BlockPos(worldX, 0, worldZ)
                );

                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                pos.set(surfacePos);

                BlockState chosenState = null;
                FluidState chosenFluid = Fluids.EMPTY.defaultFluidState();

                for (int y = surfacePos.getY(); y >= level.getMinBuildHeight(); y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) {
                        continue;
                    }

                    if (isIgnoredTopLayer(state)) {
                        continue;
                    }

                    chosenState = state;
                    chosenFluid = level.getFluidState(pos);
                    break;
                }

                int color;

                if (chosenState == null) {
                    color = 0xFF000000;
                } else {
                    boolean isWater =
                            chosenState.is(Blocks.WATER)
                                    || chosenFluid.is(FluidTags.WATER);

                    if (isWater) {
                        color = 0xFFFF4A1F;
                    } else {
                        int base = chosenState.getMapColor(level, pos).col | 0xFF000000;
                        color = adjustColor(base, chosenState, chosenFluid);
                    }
                }

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
            computeLayout();

            guiGraphics.fill(
                    panelX, panelY,
                    panelX + panelWidth, panelY + panelHeight,
                    0xC0101010 // alpha C0, like vanilla container background
            );

            // draw the map, either scaled to fit or as a 1:1 viewport window
            if (scaledToFit) {
                guiGraphics.blit(
                        mapTexture,
                        mapX, mapY,
                        0, 0,
                        mapDrawWidth, mapDrawHeight,
                        textureWidth, textureHeight
                );
            } else {
                guiGraphics.blit(
                        mapTexture,
                        mapX, mapY,            // screen position
                        scrollX, scrollY,      // u, v in texture
                        mapDrawWidth, mapDrawHeight,
                        textureWidth, textureHeight
                );
            }

            // draw claimed/unclaimed overlays for visible chunks (respect scaling)
            if (claimed != null) {
                if (scaledToFit) {
                    float s = drawScale;
                    int chunkPixel = (int)Math.ceil(16 * s);
                    for (int cz = 0; cz < sizeZ; cz++) {
                        for (int cx = 0; cx < sizeX; cx++) {
                            int index = cx + cz * sizeX;
                            if (index < 0 || index >= claimed.length) continue;
                            int col = claimed[index] ? 0x4000FF00 : 0x40FF0000;
                            int sx = mapX + (int)Math.round(cx * 16 * s);
                            int sy = mapY + (int)Math.round(cz * 16 * s);
                            int ex = sx + chunkPixel;
                            int ey = sy + chunkPixel;

                            // clip to panel viewport
                            int vx0 = panelX + 8;
                            int vy0 = panelY + 8;
                            int vx1 = vx0 + viewportWidth;
                            int vy1 = vy0 + viewportHeight;

                            int drawX0 = Math.max(sx, vx0);
                            int drawY0 = Math.max(sy, vy0);
                            int drawX1 = Math.min(ex, vx1);
                            int drawY1 = Math.min(ey, vy1);

                            if (drawX1 > drawX0 && drawY1 > drawY0) {
                                guiGraphics.fill(drawX0, drawY0, drawX1, drawY1, col);
                            }
                        }
                    }
                } else {
                    int firstChunkX = clamp(scrollX / 16, 0, sizeX - 1);
                    int lastChunkX = clamp((scrollX + viewportWidth - 1) / 16, 0, sizeX - 1);
                    int firstChunkZ = clamp(scrollY / 16, 0, sizeZ - 1);
                    int lastChunkZ = clamp((scrollY + viewportHeight - 1) / 16, 0, sizeZ - 1);

                    for (int cz = firstChunkZ; cz <= lastChunkZ; cz++) {
                        for (int cx = firstChunkX; cx <= lastChunkX; cx++) {
                            int index = cx + cz * sizeX;
                            if (index < 0 || index >= claimed.length) continue;
                            int col = claimed[index] ? 0x4000FF00 : 0x40FF0000;
                            int sx = mapX + cx * 16 - scrollX;
                            int sy = mapY + cz * 16 - scrollY;
                            int ex = sx + 16;
                            int ey = sy + 16;

                            // clip to viewport
                            int vx0 = mapX;
                            int vy0 = mapY;
                            int vx1 = mapX + viewportWidth;
                            int vy1 = mapY + viewportHeight;

                            int drawX0 = Math.max(sx, vx0);
                            int drawY0 = Math.max(sy, vy0);
                            int drawX1 = Math.min(ex, vx1);
                            int drawY1 = Math.min(ey, vy1);

                            if (drawX1 > drawX0 && drawY1 > drawY0) {
                                guiGraphics.fill(drawX0, drawY0, drawX1, drawY1, col);
                            }
                        }
                    }
                }
            }

            // side text
            guiGraphics.drawString(this.font, "Bastion Map", sideX, sideY, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "More UI coming...", sideX, sideY + 12, 0xAAAAAA, false);

            // scrollbars only when not scaled
            if (!scaledToFit && textureWidth > viewportWidth) {
                int hX = panelX + 8;
                int hY = panelY + 8 + viewportHeight + 4;
                int hW = viewportWidth;
                int hH = 8;

                int maxScrollX = Math.max(1, textureWidth - viewportWidth);
                int handleW = Math.max(8, (int)((float)viewportWidth * ((float)viewportWidth / (float)textureWidth)));
                int handleX = hX + (int)((float)scrollX / (float)maxScrollX * (hW - handleW));

                guiGraphics.fill(hX, hY, hX + hW, hY + hH, 0xFF000000);
                guiGraphics.fill(handleX, hY, handleX + handleW, hY + hH, 0xFFFFFFFF);
            }

            if (!scaledToFit && textureHeight > viewportHeight) {
                int vX = panelX + 8 + viewportWidth + 4;
                int vY = panelY + 8;
                int vW = 8;
                int vH = viewportHeight;

                int maxScrollY = Math.max(1, textureHeight - viewportHeight);
                int handleH = Math.max(8, (int)((float)viewportHeight * ((float)viewportHeight / (float)textureHeight)));
                int handleY = vY + (int)((float)scrollY / (float)maxScrollY * (vH - handleH));

                guiGraphics.fill(vX, vY, vX + vW, vY + vH, 0xFF000000);
                guiGraphics.fill(vX, handleY, vX + vW, handleY + handleH, 0xFFFFFFFF);
            }
        } else {
            guiGraphics.drawString(this.font, "Generating map...", 10, 10, 0xFFFFFF, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void computeLayout() {
        // Panel size
        panelWidth = 320;
        panelHeight = 220;

        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int padding = 8;

        // Max allowed map drawing space
        int maxMapWidth  = panelWidth - 110;        // leave space for side panel
        int maxMapHeight = panelHeight - padding*2; // vertical padding

        viewportWidth = maxMapWidth;
        viewportHeight = maxMapHeight;

        // Compute scale to fit the whole texture into the viewport when needed.
        drawScale = Math.min((float)viewportWidth / Math.max(1, textureWidth), (float)viewportHeight / Math.max(1, textureHeight));
        if (drawScale > 1.0f) drawScale = 1.0f; // never upscale
        scaledToFit = drawScale < 1.0f;

        mapDrawWidth = Math.max(1, (int)(textureWidth * drawScale));
        mapDrawHeight = Math.max(1, (int)(textureHeight * drawScale));

        if (scaledToFit) {
            scrollX = 0;
            scrollY = 0;
            mapX = panelX + padding + (viewportWidth - mapDrawWidth) / 2;
            mapY = panelY + padding + (viewportHeight - mapDrawHeight) / 2;
        } else {
            scrollX = clamp(scrollX, 0, Math.max(0, textureWidth - viewportWidth));
            scrollY = clamp(scrollY, 0, Math.max(0, textureHeight - viewportHeight));
            mapDrawWidth = viewportWidth;
            mapDrawHeight = viewportHeight;
            mapX = panelX + padding;
            mapY = panelY + padding;
        }

        sideX = mapX + mapDrawWidth + padding;
        sideY = mapY;
        sideWidth = panelX + panelWidth - padding - sideX;
        sideHeight = panelHeight - padding * 2;
    }

    private int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // horizontal scrollbar area
        int hX = panelX + 8;
        int hY = panelY + 8 + viewportHeight + 4;
        int hW = viewportWidth;
        int hH = 8;

        int vX = panelX + 8 + viewportWidth + 4;
        int vY = panelY + 8;
        int vW = 8;
        int vH = viewportHeight;

        if (!scaledToFit && textureWidth > viewportWidth && mouseY >= hY && mouseY <= hY + hH && mouseX >= hX && mouseX <= hX + hW) {
            int maxScrollX = Math.max(1, textureWidth - viewportWidth);
            int handleW = Math.max(8, (int)((float)viewportWidth * ((float)viewportWidth / (float)textureWidth)));
            int handleX = hX + (int)((float)scrollX / (float)maxScrollX * (hW - handleW));
            if ((int)mouseX >= handleX && (int)mouseX <= handleX + handleW) {
                draggingH = true;
                dragOffset = (int)mouseX - handleX;
                return true;
            }
        }

        if (!scaledToFit && textureHeight > viewportHeight && mouseX >= vX && mouseX <= vX + vW && mouseY >= vY && mouseY <= vY + vH) {
            int maxScrollY = Math.max(1, textureHeight - viewportHeight);
            int handleH = Math.max(8, (int)((float)viewportHeight * ((float)viewportHeight / (float)textureHeight)));
            int handleY = vY + (int)((float)scrollY / (float)maxScrollY * (vH - handleH));
            if ((int)mouseY >= handleY && (int)mouseY <= handleY + handleH) {
                draggingV = true;
                dragOffset = (int)mouseY - handleY;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingH) {
            int hX = panelX + 8;
            int hW = viewportWidth;
            int maxScrollX = Math.max(1, textureWidth - viewportWidth);
            int handleW = Math.max(8, (int)((float)viewportWidth * ((float)viewportWidth / (float)textureWidth)));
            int pos = (int)mouseX - hX - dragOffset;
            pos = clamp(pos, 0, hW - handleW);
            if (maxScrollX > 0) {
                scrollX = (int)((float)pos / (hW - handleW) * maxScrollX);
            } else {
                scrollX = 0;
            }
            return true;
        }

        if (draggingV) {
            int vY = panelY + 8;
            int vH = viewportHeight;
            int maxScrollY = Math.max(1, textureHeight - viewportHeight);
            int handleH = Math.max(8, (int)((float)viewportHeight * ((float)viewportHeight / (float)textureHeight)));
            int pos = (int)mouseY - vY - dragOffset;
            pos = clamp(pos, 0, vH - handleH);
            if (maxScrollY > 0) {
                scrollY = (int)((float)pos / (vH - handleH) * maxScrollY);
            } else {
                scrollY = 0;
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingH = false;
        draggingV = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!scaledToFit && mouseX >= mapX && mouseX <= mapX + viewportWidth && mouseY >= mapY && mouseY <= mapY + viewportHeight) {
            int maxScrollY = Math.max(0, textureHeight - viewportHeight);
            if (maxScrollY > 0) {
                scrollY = clamp(scrollY - (int)(delta * 16), 0, maxScrollY);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
