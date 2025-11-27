package portablemechest.item;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import portablemechest.PortableMEChest;
import portablemechest.client.gui.PortableMEChestGuiHandler;
import portablemechest.items.content.PortableMEChestGuiObject;
import portablemechest.util.MagnetMode;
import portablemechest.util.MagnetUtil;

public class ItemPortableMEChest extends AEBaseItem implements IGuiItem, IItemGroup {

    private static final String TAG_CELL = "CellItem";
    private IIcon iconEmpty;
    private IIcon iconFilled;
    private IIcon iconMagnetEmpty;
    private IIcon iconMagnetFilled;

    public ItemPortableMEChest() {
        super();
        this.setFeature(EnumSet.of(AEFeature.StorageCells));
        this.setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }

        if (player.isSneaking() && hasCell(stack)) {
            MagnetMode next = MagnetUtil.toggleMode(stack);
            return stack;
        }

        if (hasCell(stack)) {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_ACCESS, world, 0, 0, 0);
        } else {
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_CONFIG, world, 0, 0, 0);
        }
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (player.isSneaking()) {
            // Sneak-use on block: open config GUI like AE chest.
            player.openGui(PortableMEChest.instance, PortableMEChestGuiHandler.GUI_CONFIG, world, 0, 0, 0);
            return true;
        }
        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        lines.add(StatCollector.translateToLocal("tooltip.portablemechest.use.insert"));
        lines.add(StatCollector.translateToLocal("tooltip.portablemechest.use.access"));
        if (hasCell(stack)) {
            lines.add(
                StatCollector.translateToLocal("tooltip.portablemechest.magnet") + ": "
                    + StatCollector.translateToLocal(
                        "tooltip.portablemechest.magnet." + MagnetUtil.getMode(stack)
                            .name()
                            .toLowerCase()));
            lines.add(StatCollector.translateToLocal("tooltip.portablemechest.magnet.toggle"));
        }
        if (hasCell(stack)) {
            lines.add(GuiText.StorageCells.getLocal());
            ItemStack cell = getStoredCell(stack);
            if (cell != null) {
                lines.add("- " + cell.getDisplayName());
                IMEInventoryHandler<IAEItemStack> cdi = AEApi.instance()
                    .registries()
                    .cell()
                    .getCellInventory(cell, null, StorageChannel.ITEMS);
                if (cdi instanceof ICellInventoryHandler) {
                    ICellInventory inv = ((ICellInventoryHandler) cdi).getCellInv();
                    if (inv != null) {
                        lines.add(
                            NumberFormat.getInstance()
                                .format(inv.getUsedBytes()) + " / "
                                + NumberFormat.getInstance()
                                    .format(inv.getTotalBytes())
                                + " "
                                + GuiText.BytesUsed.getLocal());
                        lines.add(
                            NumberFormat.getInstance()
                                .format(inv.getStoredItemTypes()) + " / "
                                + NumberFormat.getInstance()
                                    .format(inv.getTotalItemTypes())
                                + " "
                                + GuiText.Types.getLocal());
                    }
                }
            }
        }
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.iconEmpty = register.registerIcon(PortableMEChest.MODID + ":portable_me_chest_empty");
        this.iconFilled = register.registerIcon(PortableMEChest.MODID + ":portable_me_chest");
        this.iconMagnetEmpty = register.registerIcon(PortableMEChest.MODID + ":portable_me_chest_magnet_empty");
        this.iconMagnetFilled = register.registerIcon(PortableMEChest.MODID + ":portable_me_chest_magnet");
        this.itemIcon = iconEmpty;
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return this.itemIcon;
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        boolean on = MagnetUtil.getMode(stack) == MagnetMode.on;
        if (hasCell(stack)) {
            return on ? iconMagnetFilled : iconFilled;
        }
        // No cell: always use empty (no magnet variant)
        return iconEmpty;
    }

    @Override
    public IIcon getIconIndex(ItemStack stack) {
        return getIcon(stack, 0);
    }

    @Override
    public IGuiItemObject getGuiObject(ItemStack stack, World world, int x, int y, int z) {
        return new PortableMEChestGuiObject(stack, x);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean held) {
        super.onUpdate(stack, world, entity, slot, held);
        if (!world.isRemote && entity instanceof EntityPlayer && world.getTotalWorldTime() % 10 == 0) {
            EntityPlayer player = (EntityPlayer) entity;
            if (!player.isSneaking() && hasCell(stack) && MagnetUtil.getMode(stack) == MagnetMode.on) {
                MagnetUtil.doMagnet(stack, player, () -> MagnetUtil.createHandlerForChest(stack, player));
            }
        }
    }

    @Override
    public String getUnlocalizedGroupName(java.util.Set<ItemStack> others, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    public static boolean hasCell(ItemStack chest) {
        return getStoredCell(chest) != null;
    }

    public static ItemStack getStoredCell(ItemStack chest) {
        if (chest == null) {
            return null;
        }
        NBTTagCompound tag = chest.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_CELL)) {
            return null;
        }
        NBTTagCompound cellTag = tag.getCompoundTag(TAG_CELL);
        return ItemStack.loadItemStackFromNBT(cellTag);
    }

    public static void setStoredCell(ItemStack chest, ItemStack cell) {
        if (chest == null) {
            return;
        }
        NBTTagCompound tag = chest.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            chest.setTagCompound(tag);
        }
        if (cell == null) {
            tag.removeTag(TAG_CELL);
        } else {
            NBTTagCompound cellTag = new NBTTagCompound();
            cell.writeToNBT(cellTag);
            tag.setTag(TAG_CELL, cellTag);
        }
    }

    public static boolean isValidCell(ItemStack maybeCell) {
        if (maybeCell == null) {
            return false;
        }
        return maybeCell.getItem() instanceof IStorageCell || AEApi.instance()
            .registries()
            .cell()
            .getHandler(maybeCell) != null;
    }
}
