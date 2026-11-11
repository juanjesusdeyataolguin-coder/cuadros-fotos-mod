package com.fraxxz.fotos;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

public class PhotoFrameBlockEntity extends BlockEntity {
    private String photoId = "";

    public PhotoFrameBlockEntity(BlockPos pos, BlockState state) {
        super(FotosMod.FRAME_BE, pos, state);
    }

    public String getPhotoId() { return photoId; }

    public void setPhotoId(String id) {
        this.photoId = id;
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("photo", photoId);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        photoId = nbt.getString("photo");
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }

    @Override
    public NbtCompound toInitialChunkDataNbt() { return createNbt(); }
}
