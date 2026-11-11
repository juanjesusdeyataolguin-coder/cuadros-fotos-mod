package com.fraxxz.fotos;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class PhotoFrameBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH = Block.createCuboidShape(0, 0, 14, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 2);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);

    public PhotoFrameBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new PhotoFrameBlockEntity(pos, state); }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        switch (state.get(FACING)) {
            case SOUTH: return SOUTH;
            case EAST: return EAST;
            case WEST: return WEST;
            default: return NORTH;
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        if (side.getAxis().isVertical()) return null;
        BlockState st = getDefaultState().with(FACING, side);
        return st.canPlaceAt(ctx.getWorld(), ctx.getBlockPos()) ? st : null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction f = state.get(FACING);
        BlockPos back = pos.offset(f.getOpposite());
        return world.getBlockState(back).isSideSolidFullSquare(world, back, f);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.get(FACING).getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            ServerPlayNetworking.send(sp, FotosMod.OPEN_FRAME, buf);
        }
        return ActionResult.success(world.isClient);
    }
}
