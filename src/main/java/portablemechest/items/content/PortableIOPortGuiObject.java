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
import portablemechest.item.ItemPortableIOPort;

/**
 * GUI object to expose the target cell (slot 1) as monitorable. Slot 0 is source.
 */
public class PortableIOPortGuiObject extends appeng.api.storage.MEMonitorHandler<IAEItemStack>
    implements IPortableCell, IInventorySlotAware {

    private final ItemStack ioPortStack;
    private final ItemStack[] cells;
    private final int slotIndex;

    public PortableIOPortGuiObject(ItemStack ioport, int slotIndex) {
        this(ioport, slotIndex, ItemPortableIOPort.getCells(ioport));
    }

    private PortableIOPortGuiObject(ItemStack ioport, int slotIndex, ItemStack[] cells) {
        super(createHandler(ioport, cells));
        this.ioPortStack = ioport;
        this.cells = cells;
        this.slotIndex = slotIndex;
    }

    private static IMEInventoryHandler<IAEItemStack> createHandler(ItemStack ioport, ItemStack[] cells) {
        // aggregate all output slots as a view
        List<MultiCellInventoryHandler.HandlerEntry> handlers = new ArrayList<>();
        for (int i = ItemPortableIOPort.INPUT_SLOTS; i
            < ItemPortableIOPort.INPUT_SLOTS + ItemPortableIOPort.OUTPUT_SLOTS; i++) {
            ItemStack cell = cells[i];
            if (cell != null) {
                IMEInventoryHandler<IAEItemStack> handler = AEApi.instance()
                    .registries()
                    .cell()
                    .getCellInventory(cell, new IOSaveProvider(ioport, cells, i), StorageChannel.ITEMS);
                if (handler != null) {
                    handlers.add(new MultiCellInventoryHandler.HandlerEntry(handler, i));
                }
            }
        }
        if (handlers.isEmpty()) {
            return new NullInventory<>();
        }
        return new MultiCellInventoryHandler(handlers);
    }

    @Override
    public ItemStack getItemStack() {
        return this.ioPortStack;
    }

    @Override
    public int getInventorySlot() {
        return this.slotIndex;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        return amt;
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
            NBTTagCompound data = Platform.openNbtData(PortableIOPortGuiObject.this.ioPortStack);
            manager.writeToNBT(data);
        });

        configManager.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        configManager.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        configManager.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);

        configManager.readFromNBT(
            (NBTTagCompound) Platform.openNbtData(this.ioPortStack)
                .copy());
        return configManager;
    }

    public ItemStack[] getContainedCells() {
        return cells;
    }

    private static class IOSaveProvider implements appeng.api.storage.ISaveProvider {

        private final ItemStack ioPortStack;
        private final ItemStack[] cells;
        private final int slot;

        IOSaveProvider(ItemStack ioPortStack, ItemStack[] cells, int slot) {
            this.ioPortStack = ioPortStack;
            this.cells = cells;
            this.slot = slot;
        }

        @Override
        public void saveChanges(appeng.api.storage.IMEInventory cellInventory) {
            if (slot >= 0 && slot < cells.length) {
                ItemPortableIOPort.setCells(ioPortStack, cells);
            }
        }
    }
}
