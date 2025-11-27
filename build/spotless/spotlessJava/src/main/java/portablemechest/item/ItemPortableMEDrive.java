package portablemechest.item;

import java.util.ArrayList;
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
import portablemechest.items.content.PortableMEDriveGuiObject;
import portablemechest.util.MagnetMode;
import portablemechest.util.MagnetUtil;

/**
 * Portable ME Drive: holds multiple storage cells and exposes their contents.
 */
public class ItemPortableMEDrive extends AEBaseItem implements IGuiItem, IItemGroup {

    private static final String TAG_CELLS = "Cells";
    private static final int DRIVE_SLOTS = 10;

    private IIcon iconEmpty;
    private IIcon iconFilled;
    private IIcon iconMagnetEmpty;
    private IIcon iconMagnetFilled;

    public ItemPortableMEDrive() {
        super();
        this.setFeature(EnumSet.of(AEFeature.StorageCells));
        this.setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }

        if (player.isSneaking() && hasAnyCell(stack)) {
            MagnetMode next = MagnetUtil.toggleMode(stack);
            return stack;
        }

        if (hasAnyCell(stack)) {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_DRIVE_ACCESS, world, 0, 0, 0);
        } else {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_DRIVE_CONFIG, world, 0, 0, 0);
        }
        return stack;
    }

    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        lines.add(StatCollector.translateToLocal("tooltip.portablemechest.drive.insert"));
        lines.add(StatCollector.translateToLocal("tooltip.portablemechest.drive.access"));
        if (hasAnyCell(stack)) {
            lines.add(
                StatCollector.translateToLocal("tooltip.portablemechest.magnet") + ": "
                    + StatCollector.translateToLocal(
                        "tooltip.portablemechest.magnet." + MagnetUtil.getMode(stack)
                            .name()
                            .toLowerCase()));
            lines.add(StatCollector.translateToLocal("tooltip.portablemechest.magnet.toggle"));
        }
        List<ItemStack> cells = getCells(stack);
        int filled = 0;
        for (ItemStack cell : cells) {
            if (cell != null) {
                filled++;
            }
        }
        lines.add(filled + " / " + DRIVE_SLOTS + " " + GuiText.StorageCells.getLocal());
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.iconEmpty = register.registerIcon(PortableMEChest.MODID + ":portable_me_drive_empty");
        this.iconFilled = register.registerIcon(PortableMEChest.MODID + ":portable_me_drive");
        this.iconMagnetEmpty = register.registerIcon(PortableMEChest.MODID + ":portable_me_drive_magnet_empty");
        this.iconMagnetFilled = register.registerIcon(PortableMEChest.MODID + ":portable_me_drive_magnet");
        this.itemIcon = iconEmpty;
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        boolean on = MagnetUtil.getMode(stack) == MagnetMode.on;
        if (hasAnyCell(stack)) {
            return on ? iconMagnetFilled : iconFilled;
        }
        return iconEmpty;
    }

    @Override
    public IIcon getIconIndex(ItemStack stack) {
        return getIcon(stack, 0);
    }

    @Override
    public IGuiItemObject getGuiObject(ItemStack stack, World world, int x, int y, int z) {
        return new PortableMEDriveGuiObject(stack, x);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (player.isSneaking()) {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_DRIVE_CONFIG, world, 0, 0, 0);
            return true;
        }
        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public String getUnlocalizedGroupName(java.util.Set<ItemStack> others, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean held) {
        super.onUpdate(stack, world, entity, slot, held);
        if (!world.isRemote && entity instanceof EntityPlayer && world.getTotalWorldTime() % 10 == 0) {
            EntityPlayer player = (EntityPlayer) entity;
            if (!player.isSneaking() && hasAnyCell(stack) && MagnetUtil.getMode(stack) == MagnetMode.on) {
                MagnetUtil.doMagnet(stack, player, () -> MagnetUtil.createHandlerForDrive(stack, player));
            }
        }
    }

    public static List<ItemStack> getCells(ItemStack drive) {
        List<ItemStack> result = new ArrayList<>(DRIVE_SLOTS);
        NBTTagCompound tag = drive.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_CELLS)) {
            for (int i = 0; i < DRIVE_SLOTS; i++) {
                result.add(null);
            }
            return result;
        }
        NBTTagList list = tag.getTagList(TAG_CELLS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < DRIVE_SLOTS; i++) {
            if (i < list.tagCount()) {
                ItemStack cell = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                result.add(cell);
            } else {
                result.add(null);
            }
        }
        while (result.size() < DRIVE_SLOTS) {
            result.add(null);
        }
        return result;
    }

    public static void setCells(ItemStack drive, List<ItemStack> cells) {
        NBTTagCompound tag = drive.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            drive.setTagCompound(tag);
        }
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < DRIVE_SLOTS; i++) {
            ItemStack cell = i < cells.size() ? cells.get(i) : null;
            if (cell != null) {
                NBTTagCompound ctag = new NBTTagCompound();
                cell.writeToNBT(ctag);
                list.appendTag(ctag);
            } else {
                list.appendTag(new NBTTagCompound());
            }
        }
        tag.setTag(TAG_CELLS, list);
    }

    public static boolean hasAnyCell(ItemStack drive) {
        List<ItemStack> cells = getCells(drive);
        for (ItemStack cell : cells) {
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

    public static int getSlotCount() {
        return DRIVE_SLOTS;
    }
}
