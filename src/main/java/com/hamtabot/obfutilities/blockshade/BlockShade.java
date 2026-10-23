package com.hamtabot.obfutilities.blockshade;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.config.ModConfig;
import com.hamtabot.obfutilities.mixin.SpriteContentsAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.MipmapHelper;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockView;

import java.io.InputStream;
import java.util.*;

public final class BlockShade {

    private static final Identifier DARK_TEXTURE = new Identifier(OBFUtilities.MOD_ID, "textures/shade/dark.png");
    private static final Identifier MARKER = new Identifier("minecraft", "block/stone");

    private static NativeImage dark;
    private static final Map<Block, Set<Identifier>> blockSprites = new HashMap<>();
    private static final Map<Identifier, NativeImage> originals = new HashMap<>();
    private static Set<Identifier> applied = new HashSet<>();

    private static Sprite marker;

    private BlockShade() {}

    private static Integer appliedFingerprint;

    public static void tick(MinecraftClient client) {
        SpriteAtlasTexture atlas = atlas(client);
        if (atlas == null) return;
        Sprite current = atlas.getSprite(MARKER);
        if (current != marker) {
            marker = current;
            reset(client);
        }
        if (dark == null) return;
        int fingerprint = fingerprint();
        if (appliedFingerprint != null && appliedFingerprint == fingerprint) return;
        apply(atlas, wantedSprites());
        appliedFingerprint = fingerprint;
    }

    private static int fingerprint() {
        ModConfig cfg = OBFUtilities.config;
        int fp = cfg.blockShadeEnabled ? 1 : 2;
        for (ModConfig.BlockShadeEntry e : cfg.blockShades) fp = 31 * fp + e.blockId.hashCode() * 2 + (e.enabled ? 1 : 0);
        return fp;
    }

    private static Set<Identifier> wantedSprites() {
        ModConfig cfg = OBFUtilities.config;
        Set<Identifier> wanted = new HashSet<>();
        if (!cfg.blockShadeEnabled) return wanted;
        for (ModConfig.BlockShadeEntry e : cfg.blockShades) {
            Identifier id = Identifier.tryParse(e.blockId);
            if (e.enabled && id != null && Registries.BLOCK.containsId(id)) wanted.addAll(spritesOf(Registries.BLOCK.get(id)));
        }
        return wanted;
    }

    public static boolean isEligible(String blockId) {
        Identifier id = Identifier.tryParse(blockId);
        if (id == null || !Registries.BLOCK.containsId(id)) return false;
        BlockState state = Registries.BLOCK.get(id).getDefaultState();
        return Block.isShapeFullCube(state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
    }

    private static Set<Identifier> spritesOf(Block block) {
        return blockSprites.computeIfAbsent(block, b -> {
            Set<Identifier> out = new HashSet<>();
            Random random = Random.create(42L);
            for (BlockState state : b.getStateManager().getStates()) {
                BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
                out.add(model.getParticleSprite().getContents().getId());
                for (Direction dir : Direction.values()) addQuads(out, model.getQuads(state, dir, random));
                addQuads(out, model.getQuads(state, null, random));
            }
            return out;
        });
    }

    private static void addQuads(Set<Identifier> out, List<BakedQuad> quads) {
        for (BakedQuad q : quads) out.add(q.getSprite().getContents().getId());
    }

    private static void reset(MinecraftClient client) {
        if (dark != null) { dark.close(); dark = null; }
        originals.values().forEach(NativeImage::close);
        originals.clear();
        blockSprites.clear();
        applied = new HashSet<>();
        appliedFingerprint = null;
        OBFUtilities.config.blockShades.removeIf(e -> !isEligible(e.blockId));

        client.getResourceManager().getResource(DARK_TEXTURE).ifPresent(res -> {
            try (InputStream in = res.getInputStream()) { dark = NativeImage.read(in); }
            catch (Exception e) { OBFUtilities.LOGGER.error("[OBF] Texture sombre illisible : " + e.getMessage()); }
        });
    }

    private static void apply(SpriteAtlasTexture atlas, Set<Identifier> wanted) {
        Set<Identifier> touched = new HashSet<>(applied);
        touched.addAll(wanted);
        boolean bound = false;
        for (Identifier spriteId : touched) {
            boolean want = wanted.contains(spriteId);
            if (want == applied.contains(spriteId)) continue;
            Sprite sprite = atlas.getSprite(spriteId);
            if (!sprite.getContents().getId().equals(spriteId)) continue;
            NativeImage original = originals.get(spriteId);
            if (!want && original == null) continue;
            if (!bound) { atlas.bindTexture(); bound = true; }
            if (want) darken(sprite, spriteId);
            else restore(sprite, original);
        }
        applied = wanted;
    }

    private static void darken(Sprite sprite, Identifier spriteId) {
        NativeImage image = ((SpriteContentsAccessor) sprite.getContents()).obf$getImage();
        int w = image.getWidth(), h = image.getHeight();
        originals.computeIfAbsent(spriteId, id -> {
            NativeImage copy = new NativeImage(w, h, false);
            copy.copyFrom(image);
            return copy;
        });
        int frameH = h > w && h % w == 0 ? w : h;
        int sw = dark.getWidth(), sh = dark.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setColor(x, y, dark.getColor(x * sw / w, (y % frameH) * sh / frameH));
            }
        }
        reupload(sprite);
    }

    private static void restore(Sprite sprite, NativeImage original) {
        ((SpriteContentsAccessor) sprite.getContents()).obf$getImage().copyFrom(original);
        reupload(sprite);
    }

    private static void reupload(Sprite sprite) {
        SpriteContentsAccessor acc = (SpriteContentsAccessor) sprite.getContents();
        NativeImage[] old = acc.obf$getMipmapLevelsImages();
        acc.obf$setMipmapLevelsImages(MipmapHelper.getMipmapLevelsImages(new NativeImage[]{ acc.obf$getImage() }, old.length - 1));
        for (int i = 1; i < old.length; i++) old[i].close();
        sprite.upload();
    }

    private static SpriteAtlasTexture atlas(MinecraftClient client) {
        if (client.getBakedModelManager() == null) return null;
        try { return client.getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE); }
        catch (Exception e) { return null; }
    }
}
