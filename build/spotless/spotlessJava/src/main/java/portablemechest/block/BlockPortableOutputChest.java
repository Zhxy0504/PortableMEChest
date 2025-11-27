package portablemechest.block;

import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.util.Platform;
import portablemechest.PortableMEChest;
import portablemechest.client.gui.PortableMEChestGuiHandler;
import portablemechest.item.ItemPortableMEChest;
import portablemechest.tile.TilePortableOutputChest;

/**
 * World-placed variant of a portable ME chest with a configurable output side.
 */
public class BlockPortableOutputChest extends BlockContainer {

    public BlockPortableOutputChest() {
        super(Material.iron);
        setBlockName(PortableMEChest.MODID + ".portable_output_chest");
        setHardness(2.0F);
        setResistance(5.0F);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc);
        // Use AE2 ME chest texture for now.
        setBlockTextureName(PortableMEChest.MODID + ":portable_output_chest");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TilePortableOutputChest();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TilePortableOutputChest)) {
            return false;
        }
        TilePortableOutputChest chest = (TilePortableOutputChest) te;
        ItemStack held = player.getHeldItem();

        // Wrench: set output side
        if (Platform.isWrench(player, held, x, y, z)) {
            if (!world.isRemote) {
                chest.setOutputFace(ForgeDirection.getOrientation(side));
                world.markBlockForUpdate(x, y, z);
            }
            return true;
        }

        // Sneak-right-click: extract cell
        if (player.isSneaking()) {
            if (!world.isRemote) {
                ItemStack cell = chest.getCellStack();
                if (cell != null) {
                    chest.setCellStack(null);
                    if (!player.inventory.addItemStackToInventory(cell)) {
                        player.dropPlayerItemWithRandomChoice(cell, false);
                    }
                    player.inventory.markDirty();
                    chest.markDirty();
                }
            }
            return true;
        }

        // Empty and holding a valid cell: insert directly
        if (!world.isRemote && chest.getCellStack() == null && ItemPortableMEChest.isValidCell(held)) {
            ItemStack toInsert = held.splitStack(1);
            chest.setCellStack(toInsert);
            if (held.stackSize <= 0) {
                player.setCurrentItemOrArmor(0, null);
            }
            player.inventory.markDirty();
            chest.markDirty();
            return true;
        }

        // Empty: open config (cell slot)
        if (!chest.hasCell()) {
            if (!world.isRemote) {
                player.openGui(
                    PortableMEChest.instance,
                    PortableMEChestGuiHandler.GUI_BLOCK_OUTPUT_CONFIG,
                    world,
                    x,
                    y,
                    z);
            }
            return true;
        }

        // Has cell: open access terminal
        if (!world.isRemote) {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_BLOCK_OUTPUT_ACCESS, world, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TilePortableOutputChest) {
            ((TilePortableOutputChest) te).dropContents();
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public int getRenderType() {
        // Standard block render type to avoid fire-like render path.
        return 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }
}
