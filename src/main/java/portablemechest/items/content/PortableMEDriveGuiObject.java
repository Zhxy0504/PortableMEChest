package portablemechest.items.content;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.me.storage.NullInventory;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import portablemechest.item.ItemPortableMEDrive;

public class PortableMEDriveGuiObject extends appeng.api.storage.MEMonitorHandler<IAEItemStack>
    implements IPortableCell, IInventorySlotAware {

    private final ItemStack driveStack;
    private final List<ItemStack> cellStacks;
    private final int slotIndex;

    public PortableMEDriveGuiObject(ItemStack drive, int slotIndex) {
        this(drive, slotIndex, ItemPortableMEDrive.getCells(drive));
    }

    private PortableMEDriveGuiObject(ItemStack drive, int slotIndex, List<ItemStack> cells) {
        super(createHandler(drive, cells));
        this.driveStack = drive;
        this.cellStacks = cells;
        this.slotIndex = slotIndex;
    }

    private static IMEInventoryHandler<IAEItemStack> createHandler(ItemStack drive, List<ItemStack> cells) {
        List<MultiCellInventoryHandler.HandlerEntry> handlers = new ArrayList<>();
        int slot = 0;
        for (ItemStack cell : cells) {
            if (cell != null) {
                IMEInventoryHandler<IAEItemStack> handler = AEApi.instance()
                    .registries()
                    .cell()
                    .getCellInventory(cell, new DriveSaveProvider(drive, cells, slot), StorageChannel.ITEMS);
                if (handler != null) {
                    handlers.add(new MultiCellInventoryHandler.HandlerEntry(handler, slot));
                }
            }
            slot++;
        }
        if (handlers.isEmpty()) {
            return new NullInventory<>();
        }
        return new MultiCellInventoryHandler(handlers);
    }

    public boolean hasCells() {
        return !cellStacks.isEmpty();
    }

    @Override
    public ItemStack getItemStack() {
        return this.driveStack;
    }

    @Override
    public int getInventorySlot() {
        return this.slotIndex;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        return amt; // no power consumption
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        return this;
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        return null;
    }

    @Override
    public IConfigManager getConfigManager() {
        ConfigManager configManager = new ConfigManager((manager, settingName, newValue) -> {
            NBTTagCompound data = Platform.openNbtData(PortableMEDriveGuiObject.this.driveStack);
            manager.writeToNBT(data);
        });

        configManager.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        configManager.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        configManager.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);

        configManager.readFromNBT(
            (NBTTagCompound) Platform.openNbtData(this.driveStack)
                .copy());
        return configManager;
    }

    public List<ItemStack> getContainedCells() {
        return cellStacks;
    }

    private static class DriveSaveProvider implements appeng.api.storage.ISaveProvider {

        private final ItemStack driveStack;
        private final List<ItemStack> cellStacksRef;
        private final int slotIndex;

        DriveSaveProvider(ItemStack driveStack, List<ItemStack> cellStacksRef, int slotIndex) {
            this.driveStack = driveStack;
            this.cellStacksRef = cellStacksRef;
            this.slotIndex = slotIndex;
        }

        @Override
        public void saveChanges(appeng.api.storage.IMEInventory cellInventory) {
            List<ItemStack> cells = ItemPortableMEDrive.getCells(driveStack);
            if (slotIndex >= 0 && slotIndex < cells.size() && slotIndex < cellStacksRef.size()) {
                ItemStack current = cellStacksRef.get(slotIndex);
                cells.set(slotIndex, current);
                cellStacksRef.set(slotIndex, current);
            }
            ItemPortableMEDrive.setCells(driveStack, cells);
        }
    }
}
