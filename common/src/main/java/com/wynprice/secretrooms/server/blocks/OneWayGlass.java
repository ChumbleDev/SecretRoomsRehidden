package com.wynprice.secretrooms.server.blocks;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.wynprice.secretrooms.server.blocks.states.OneWayGlassState;
import com.wynprice.secretrooms.server.data.SecretBlockTags;
import com.wynprice.secretrooms.server.tileentity.SecretTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class OneWayGlass extends SecretBaseBlock {

    // Direction properties for each face - true = glass, false = mimic texture
    private static final BooleanProperty NORTH = PipeBlock.NORTH;
    private static final BooleanProperty EAST = PipeBlock.EAST;
    private static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    private static final BooleanProperty WEST = PipeBlock.WEST;
    private static final BooleanProperty UP = PipeBlock.UP;
    private static final BooleanProperty DOWN = PipeBlock.DOWN;

    public OneWayGlass(Properties properties) {
        super(properties);

        // Default state: all faces are mimic texture (false)
        this.registerDefaultState(this.defaultBlockState()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, true)  // Set bottom face to glass by default
        );
    }

    @Override
    protected BlockState createNewState(Block block, ImmutableMap<Property<?>, Comparable<?>> propertiesToValueMap, MapCodec<BlockState> codec) {
        return new OneWayGlassState(block, propertiesToValueMap, codec);
    }

    @Override
    public Boolean getSolidValue() {
        return true;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos offFace = context.replacingClickedOnBlock() ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState state = context.getLevel().getBlockState(offFace);
        if(state.getBlock() == this) {
            return state;
        }
        
        // Start with normal placement logic
        Direction horizontalFacing = context.getHorizontalDirection();
        BlockState placementState = super.getStateForPlacement(context).setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(horizontalFacing), true);
        
        // Always set bottom face to glass mode to prevent mimic texture showing from below
        BooleanProperty downProperty = PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN);
        if (downProperty != null && placementState.hasProperty(downProperty)) {
            placementState = placementState.setValue(downProperty, true);
        }
        
        // Auto-connect to adjacent OneWayGlass blocks
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = context.getClickedPos().relative(direction);
            BlockState neighborState = context.getLevel().getBlockState(neighborPos);
            
            if (neighborState.getBlock() instanceof OneWayGlass) {
                // Set this face to glass mode to connect with the neighbor
                BooleanProperty thisProperty = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
                if (thisProperty != null && placementState.hasProperty(thisProperty)) {
                    placementState = placementState.setValue(thisProperty, true);
                }
            }
        }
        
        // Special handling for vertical stacking - ensure bottom face is glass when there's a OneWayGlass below
        BlockPos belowPos = context.getClickedPos().relative(Direction.DOWN);
        BlockState belowState = context.getLevel().getBlockState(belowPos);
        if (belowState.getBlock() instanceof OneWayGlass) {
            // Set bottom face to glass mode for seamless stacking (redundant but ensuring it's set)
            if (downProperty != null && placementState.hasProperty(downProperty)) {
                placementState = placementState.setValue(downProperty, true);
            }
        }
        
        return placementState;
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if(player.getItemInHand(handIn).isEmpty()) {
            // Toggle the clicked face between glass and mimic
            worldIn.setBlock(pos, state.cycle(PipeBlock.PROPERTY_BY_DIRECTION.get(hit.getDirection())), 3);
            BlockEntity tileEntity = worldIn.getBlockEntity(pos);
            if(tileEntity instanceof SecretTileEntity te) {
                te.requestModelDataUpdateIfPossible();
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(state, worldIn, pos, player, handIn, hit);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        
        // Update adjacent OneWayGlass blocks to create bidirectional connections
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = worldIn.getBlockState(neighborPos);
            
            if (neighborState.getBlock() instanceof OneWayGlass) {
                // Set the neighbor's face that touches this block to glass mode
                Direction oppositeSide = direction.getOpposite();
                BooleanProperty neighborProperty = PipeBlock.PROPERTY_BY_DIRECTION.get(oppositeSide);
                
                if (neighborProperty != null && neighborState.hasProperty(neighborProperty) && !neighborState.getValue(neighborProperty)) {
                    BlockState newNeighborState = neighborState.setValue(neighborProperty, true);
                    worldIn.setBlock(neighborPos, newNeighborState, 3);
                    
                    // Request model update for the neighbor block
                    BlockEntity tileEntity = worldIn.getBlockEntity(neighborPos);
                    if (tileEntity instanceof SecretTileEntity te) {
                        te.requestModelDataUpdateIfPossible();
                    }
                }
            }
        }
        
        // Special handling for vertical stacking - update the top face of the block below
        BlockPos belowPos = pos.relative(Direction.DOWN);
        BlockState belowState = worldIn.getBlockState(belowPos);
        if (belowState.getBlock() instanceof OneWayGlass) {
            BooleanProperty upProperty = PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP);
            if (upProperty != null && belowState.hasProperty(upProperty) && !belowState.getValue(upProperty)) {
                BlockState newBelowState = belowState.setValue(upProperty, true);
                worldIn.setBlock(belowPos, newBelowState, 3);
                
                // Request model update for the block below
                BlockEntity tileEntity = worldIn.getBlockEntity(belowPos);
                if (tileEntity instanceof SecretTileEntity te) {
                    te.requestModelDataUpdateIfPossible();
                }
            }
        }
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 15; // Full light blocking to prevent x-ray
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 0.2F;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isOcclusionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        // Never skip rendering to prevent x-ray
        return false;
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        BlockEntity tileEntity = worldIn.getBlockEntity(currentPos);
        if(tileEntity instanceof SecretTileEntity te && tileEntity.getLevel().isClientSide) {
            te.requestModelDataUpdateIfPossible();
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    // Utility methods for connected textures
    public static boolean isGlassFace(BlockState state, Direction direction) {
        // Safety check: null direction means we can't determine glass face
        if (direction == null) {
            return false;
        }
        
        // Safety check: only check OneWayGlass properties if this is actually a OneWayGlass block
        if (!(state.getBlock() instanceof OneWayGlass)) {
            // For non-OneWayGlass blocks (like regular glass), return false (all faces are mimic)
            return false;
        }
        
        // Get the property for the direction
        BooleanProperty directionProperty = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
        if (directionProperty == null) {
            // Property doesn't exist for this direction, return false
            return false;
        }
        
        // Check if the property exists on this block state
        if (!state.hasProperty(directionProperty)) {
            // Property doesn't exist on this block state, return false  
            return false;
        }
        
        // Get the property value - true means glass face, false means mimic face
        return state.getValue(directionProperty);
    }

    public static boolean canConnectTo(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        
        // Connect to other OneWayGlass blocks
        if (neighborState.getBlock() instanceof OneWayGlass) {
            return true;
        }
        
        // Connect to regular glass blocks and all glass types
        return isGlassBlock(neighborState);
    }

    private static boolean isGlassBlock(BlockState state) {
        String blockName = state.getBlock().toString().toLowerCase();
        
        // Check for various glass types
        return blockName.contains("glass") || 
               blockName.contains("pane") ||
               state.getBlock() == Blocks.GLASS ||
               state.getBlock() == Blocks.WHITE_STAINED_GLASS ||
               state.getBlock() == Blocks.ORANGE_STAINED_GLASS ||
               state.getBlock() == Blocks.MAGENTA_STAINED_GLASS ||
               state.getBlock() == Blocks.LIGHT_BLUE_STAINED_GLASS ||
               state.getBlock() == Blocks.YELLOW_STAINED_GLASS ||
               state.getBlock() == Blocks.LIME_STAINED_GLASS ||
               state.getBlock() == Blocks.PINK_STAINED_GLASS ||
               state.getBlock() == Blocks.GRAY_STAINED_GLASS ||
               state.getBlock() == Blocks.LIGHT_GRAY_STAINED_GLASS ||
               state.getBlock() == Blocks.CYAN_STAINED_GLASS ||
               state.getBlock() == Blocks.PURPLE_STAINED_GLASS ||
               state.getBlock() == Blocks.BLUE_STAINED_GLASS ||
               state.getBlock() == Blocks.BROWN_STAINED_GLASS ||
               state.getBlock() == Blocks.GREEN_STAINED_GLASS ||
               state.getBlock() == Blocks.RED_STAINED_GLASS ||
               state.getBlock() == Blocks.BLACK_STAINED_GLASS;
    }

    // Check if Continuity mod is loaded for connected textures
    public static boolean isContinuityLoaded() {
        try {
            Class.forName("me.pepperbell.continuity.client.ContinuityClient");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


}
