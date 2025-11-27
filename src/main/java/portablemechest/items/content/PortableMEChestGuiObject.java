package portablemechest.items.content;

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
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.me.storage.NullInventory;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import portablemechest.item.ItemPortableMEChest;

public class PortableMEChestGuiObject extends appeng.api.storage.MEMonitorHandler<IAEItemStack>
    implements IPortableCell, IInventorySlotAware {

    private final ItemStack chestStack;
    private final ItemStack cellStack;
    private final int slotIndex;

    public PortableMEChestGuiObject(ItemStack chest, int slotIndex) {
        this(createHandlerFromCell(chest), chest, slotIndex);
    }

    private PortableMEChestGuiObject(IMEInventoryHandler<IAEItemStack> handler, ItemStack chest, int slotIndex) {
        super(handler);
        this.chestStack = chest;
        this.cellStack = ItemPortableMEChest.getStoredCell(chest);
        this.slotIndex = slotIndex;
    }

    private static IMEInventoryHandler<IAEItemStack> createHandlerFromCell(ItemStack chest) {
        ItemStack cell = ItemPortableMEChest.getStoredCell(chest);
        if (cell == null) {
            return new NullInventory<>();
        }
        IMEInventoryHandler<IAEItemStack> handler = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(cell, new ChestSaveProvider(chest, cell), StorageChannel.ITEMS);
        return handler == null ? new NullInventory<>() : handler;
    }

    public boolean hasCell() {
        return cellStack != null;
    }

    @Override
    public ItemStack getItemStack() {
        return this.chestStack;
    }

    @Override
    public int getInventorySlot() {
        return this.slotIndex;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        // Portable chest does not consume power; always report request satisfied.
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
            NBTTagCompound data = Platform.openNbtData(PortableMEChestGuiObject.this.chestStack);
            manager.writeToNBT(data);
        });

        configManager.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        configManager.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        configManager.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);

        configManager.readFromNBT(
            (NBTTagCompound) Platform.openNbtData(this.chestStack)
                .copy());
        return configManager;
    }

    public ItemStack getContainedCell() {
        return cellStack;
    }

    /**
     * Minimal save provider to push changes back into the chest item.
     */
    private static class ChestSaveProvider implements ISaveProvider {

        private final ItemStack chestStack;
        private final ItemStack cellStack;

        ChestSaveProvider(ItemStack chestStack, ItemStack cellStack) {
            this.chestStack = chestStack;
            this.cellStack = cellStack;
        }

        @Override
        public void saveChanges(IMEInventory cellInventory) {
            ItemPortableMEChest.setStoredCell(chestStack, cellStack);
        }
    }
}
