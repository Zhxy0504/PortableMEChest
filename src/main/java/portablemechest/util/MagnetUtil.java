package portablemechest.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import portablemechest.item.ItemPortableMEChest;
import portablemechest.item.ItemPortableMEDrive;
import portablemechest.items.content.MultiCellInventoryHandler;

public class MagnetUtil {

    private static final String TAG_MAGNET = "MagnetMode";
    private static final int RANGE = 6;

    public static MagnetMode getMode(ItemStack stack) {
        NBTTagCompound tag = Platform.openNbtData(stack);
        if (!tag.hasKey(TAG_MAGNET)) {
            return MagnetMode.off;
        }
        try {
            return MagnetMode.valueOf(tag.getString(TAG_MAGNET));
        } catch (Exception ignored) {
            return MagnetMode.off;
        }
    }

    public static MagnetMode toggleMode(ItemStack stack) {
        MagnetMode next = getMode(stack) == MagnetMode.off ? MagnetMode.on : MagnetMode.off;
        Platform.openNbtData(stack)
            .setString(TAG_MAGNET, next.name());
        return next;
    }

    public static void doMagnet(ItemStack stack, EntityPlayer player,
        Supplier<IMEInventoryHandler<IAEItemStack>> handlerSupplier) {
        if (getMode(stack) == MagnetMode.off) {
            return;
        }
        IMEInventoryHandler<IAEItemStack> handler = handlerSupplier.get();
        if (handler == null) {
            return;
        }
        World world = player.worldObj;
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(player.posX, player.posY, player.posZ, player.posX, player.posY, player.posZ)
            .expand(RANGE, RANGE, RANGE);
        @SuppressWarnings("unchecked")
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, box);
        for (EntityItem ei : items) {
            if (ei.isDead || ei.getEntityItem() == null) continue;
            if (ei.delayBeforeCanPickup > 0) {
                ei.delayBeforeCanPickup = 0;
            }
            IAEItemStack toInsert = AEApi.instance()
                .storage()
                .createItemStack(ei.getEntityItem());
            if (toInsert == null) {
                continue;
            }
            IAEItemStack remaining = handler.injectItems(toInsert, Actionable.MODULATE, null);
            if (remaining == null || remaining.getStackSize() == 0) {
                ei.setDead();
            } else {
                ItemStack rest = remaining.getItemStack();
                ei.setEntityItemStack(rest);
            }
        }
    }

    public static IMEInventoryHandler<IAEItemStack> createHandlerForChest(ItemStack chest, EntityPlayer player) {
        ItemStack cell = ItemPortableMEChest.getStoredCell(chest);
        if (cell == null) {
            return null;
        }
        return AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(cell, new ChestSaveProvider(chest, cell), StorageChannel.ITEMS);
    }

    public static IMEInventoryHandler<IAEItemStack> createHandlerForDrive(ItemStack drive, EntityPlayer player) {
        List<ItemStack> cells = ItemPortableMEDrive.getCells(drive);
        List<MultiCellInventoryHandler.HandlerEntry> handlers = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            ItemStack cell = cells.get(i);
            if (cell == null) {
                continue;
            }
            IMEInventoryHandler<IAEItemStack> h = AEApi.instance()
                .registries()
                .cell()
                .getCellInventory(cell, new DriveSaveProvider(drive, cells, i), StorageChannel.ITEMS);
            if (h != null) {
                handlers.add(new MultiCellInventoryHandler.HandlerEntry(h, i));
            }
        }
        if (handlers.isEmpty()) {
            return null;
        }
        return new MultiCellInventoryHandler(handlers);
    }

    private static class ChestSaveProvider implements ISaveProvider {

        private final ItemStack chestStack;
        private final ItemStack cellStack;

        ChestSaveProvider(ItemStack chest, ItemStack cell) {
            this.chestStack = chest;
            this.cellStack = cell;
        }

        @Override
        public void saveChanges(appeng.api.storage.IMEInventory cellInventory) {
            ItemPortableMEChest.setStoredCell(chestStack, cellStack);
        }
    }

    private static class DriveSaveProvider implements ISaveProvider {

        private final ItemStack drive;
        private final List<ItemStack> cells;
        private final int slot;

        DriveSaveProvider(ItemStack drive, List<ItemStack> cells, int slot) {
            this.drive = drive;
            this.cells = cells;
            this.slot = slot;
        }

        @Override
        public void saveChanges(appeng.api.storage.IMEInventory cellInventory) {
            ItemPortableMEDrive.setCells(drive, cells);
        }
    }
}
