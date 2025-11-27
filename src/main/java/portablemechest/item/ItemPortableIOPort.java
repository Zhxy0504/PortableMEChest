package portablemechest.item;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import appeng.api.AEApi;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import portablemechest.PortableMEChest;
import portablemechest.client.gui.PortableMEChestGuiHandler;
import portablemechest.items.content.PortableIOPortGuiObject;

/**
 * Portable IO Port: move items between two cells based on target rules.
 */
public class ItemPortableIOPort extends AEBaseItem implements IGuiItem, IItemGroup {

    private static final String TAG_CELLS = "Cells";
    public static final int INPUT_SLOTS = 6;
    public static final int OUTPUT_SLOTS = 6;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    private IIcon iconEmpty;
    private IIcon iconFilled;

    public ItemPortableIOPort() {
        super();
        this.setFeature(EnumSet.of(AEFeature.StorageCells));
        this.setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_IOPORT, world, 0, 0, 0);
        }
        return stack;
    }

    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        lines.add(StatCollector.translateToLocal("tooltip.portablemechest.ioport.insert"));
        lines.add(StatCollector.translateToLocal("tooltip.portablemechest.ioport.transfer"));
        int filled = 0;
        for (ItemStack cell : getCells(stack)) {
            if (cell != null) {
                filled++;
            }
        }
        lines.add(filled + " / " + TOTAL_SLOTS + " " + GuiText.StorageCells.getLocal());
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.iconEmpty = register.registerIcon(PortableMEChest.MODID + ":portable_ioport_empty");
        this.iconFilled = register.registerIcon(PortableMEChest.MODID + ":portable_ioport");
        this.itemIcon = iconEmpty;
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        return hasAnyCell(stack) ? iconFilled : iconEmpty;
    }

    @Override
    public IIcon getIconIndex(ItemStack stack) {
        return getIcon(stack, 0);
    }

    @Override
    public IGuiItemObject getGuiObject(ItemStack stack, World world, int x, int y, int z) {
        return new PortableIOPortGuiObject(stack, x);
    }

    @Override
    public String getUnlocalizedGroupName(java.util.Set<ItemStack> others, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    public static ItemStack[] getCells(ItemStack ioport) {
        ItemStack[] slots = new ItemStack[TOTAL_SLOTS];
        NBTTagCompound tag = ioport.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_CELLS)) {
            return slots;
        }
        NBTTagList list = tag.getTagList(TAG_CELLS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (i < list.tagCount()) {
                slots[i] = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
            }
        }
        return slots;
    }

    public static void setCells(ItemStack ioport, ItemStack[] cells) {
        NBTTagCompound tag = ioport.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            ioport.setTagCompound(tag);
        }
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack cell = i < cells.length ? cells[i] : null;
            NBTTagCompound ctag = new NBTTagCompound();
            if (cell != null) {
                cell.writeToNBT(ctag);
            }
            list.appendTag(ctag);
        }
        tag.setTag(TAG_CELLS, list);
    }

    public static boolean hasAnyCell(ItemStack drive) {
        for (ItemStack cell : getCells(drive)) {
            if (cell != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidCell(ItemStack maybeCell) {
        if (maybeCell == null) {
            return false;
        }
        if (maybeCell.getItem() instanceof IStorageCell) {
            return true;
        }
        IMEInventoryHandler<IAEItemStack> handler = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(maybeCell, null, StorageChannel.ITEMS);
        return handler != null;
    }
}
