package com.fraxxz.fotos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FotosMod implements ModInitializer {
    public static final String ID = "fotos";
    public static final int CHUNK = 30000;
    public static final Identifier UPLOAD = new Identifier(ID, "upload");
    public static final Identifier SET_FRAME = new Identifier(ID, "set_frame");
    public static final Identifier REQUEST = new Identifier(ID, "request");
    public static final Identifier OPEN_FRAME = new Identifier(ID, "open_frame");
    public static final Identifier PHOTO = new Identifier(ID, "photo");

    public static final Block FRAME_BLOCK = new PhotoFrameBlock(
        FabricBlockSettings.copyOf(Blocks.OAK_PLANKS).nonOpaque().strength(0.5f));
    public static final BlockEntityType<PhotoFrameBlockEntity> FRAME_BE =
        FabricBlockEntityTypeBuilder.create(PhotoFrameBlockEntity::new, FRAME_BLOCK).build();
    public static final Item CAMERA = new CameraItem(new FabricItemSettings().maxCount(1));

    private static final Map<String, Assembler> PENDING = new HashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier(ID, "photo_frame"), FRAME_BLOCK);
        Registry.register(Registries.ITEM, new Identifier(ID, "photo_frame"), new BlockItem(FRAME_BLOCK, new FabricItemSettings()));
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(ID, "photo_frame"), FRAME_BE);
        Registry.register(Registries.ITEM, new Identifier(ID, "camera"), CAMERA);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(CAMERA));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> e.add(FRAME_BLOCK.asItem()));

        ServerPlayNetworking.registerGlobalReceiver(UPLOAD, (server, player, handler, buf, sender) -> {
            String id = buf.readString(40);
            int idx = buf.readVarInt();
            int total = buf.readVarInt();
            byte[] data = buf.readByteArray(CHUNK + 10);
            server.execute(() -> onUpload(server, player, id, idx, total, data));
        });
        ServerPlayNetworking.registerGlobalReceiver(SET_FRAME, (server, player, handler, buf, sender) -> {
            BlockPos pos = buf.readBlockPos();
            String id = buf.readString(40);
            server.execute(() -> onSetFrame(server, player, pos, id));
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) -> {
            String id = buf.readString(40);
            server.execute(() -> onRequest(server, player, id));
        });
    }

    static Path dir(MinecraftServer s) {
        Path p = s.getSavePath(WorldSavePath.ROOT).resolve("fotos");
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    static boolean validId(String id) { return id.matches("[0-9a-f\\-]{36}"); }

    static void onUpload(MinecraftServer s, ServerPlayerEntity p, String id, int idx, int total, byte[] data) {
        if (!validId(id) || total < 1 || total > 120 || idx < 0 || idx >= total) return;
        String key = p.getUuidAsString() + ":" + id;
        if (PENDING.size() > 64) PENDING.clear();
        Assembler a = PENDING.computeIfAbsent(key, k -> new Assembler(total));
        if (a.parts.length != total) { PENDING.remove(key); return; }
        byte[] all = a.add(idx, data);
        if (all == null) return;
        PENDING.remove(key);
        if (all.length < 8 || all.length > 3_600_000
            || (all[0] & 0xFF) != 0x89 || all[1] != 'P' || all[2] != 'N' || all[3] != 'G') {
            p.sendMessage(Text.literal("§cFoto no válida"), true);
            return;
        }
        Path f = dir(s).resolve(id + ".png");
        try {
            if (!Files.exists(f)) Files.write(f, all);
        } catch (IOException e) {
            p.sendMessage(Text.literal("§cNo pude guardar la foto en el servidor"), true);
        }
    }

    static void onSetFrame(MinecraftServer s, ServerPlayerEntity p, BlockPos pos, String id) {
        if (p.squaredDistanceTo(Vec3d.ofCenter(pos)) > 100) return;
        BlockEntity be = p.getWorld().getBlockEntity(pos);
        if (!(be instanceof PhotoFrameBlockEntity fr)) return;
        if (id.isEmpty()) { fr.setPhotoId(""); return; }
        if (!validId(id) || !Files.exists(dir(s).resolve(id + ".png"))) {
            p.sendMessage(Text.literal("§cLa foto no llegó al servidor, intenta otra vez"), true);
            return;
        }
        fr.setPhotoId(id);
    }

    static void onRequest(MinecraftServer s, ServerPlayerEntity p, String id) {
        if (!validId(id)) return;
        Path f = dir(s).resolve(id + ".png");
        if (!Files.exists(f)) return;
        try {
            byte[] all = Files.readAllBytes(f);
            List<byte[]> parts = Chunker.split(all);
            for (int i = 0; i < parts.size(); i++) {
                PacketByteBuf b = PacketByteBufs.create();
                b.writeString(id, 40);
                b.writeVarInt(i);
                b.writeVarInt(parts.size());
                b.writeByteArray(parts.get(i));
                ServerPlayNetworking.send(p, PHOTO, b);
            }
        } catch (IOException ignored) { }
    }
}
