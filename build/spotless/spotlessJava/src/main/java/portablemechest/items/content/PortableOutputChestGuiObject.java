package portablemechest.items.content;

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
import portablemechest.tile.TilePortableOutputChest;

/**
 * Gui object wrapper for the world-placed output chest tile to reuse AE's portable cell GUI.
 */
public class PortableOutputChestGuiObject extends appeng.api.storage.MEMonitorHandler<IAEItemStack>
    implements IPortableCell, IInventorySlotAware {

    private final TilePortableOutputChest tile;

    public PortableOutputChestGuiObject(TilePortableOutputChest tile) {
        super(createHandler(tile));
        this.tile = tile;
    }

    private static IMEInventoryHandler<IAEItemStack> createHandler(TilePortableOutputChest tile) {
        if (!tile.hasCell()) {
            return new NullInventory<>();
        }
        IMEInventoryHandler<IAEItemStack> handler = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(tile.getCellStack(), new TileSaveProvider(tile), StorageChannel.ITEMS);
        return handler == null ? new NullInventory<>() : handler;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        // No power use.
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
            NBTTagCompound data = tile.getConfigData();
            manager.writeToNBT(data);
            tile.setConfigData(data);
        });

        configManager.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        configManager.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        configManager.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);

        configManager.readFromNBT(
            (NBTTagCompound) tile.getConfigData()
                .copy());
        return configManager;
    }

    @Override
    public int getInventorySlot() {
        return -1;
    }

    @Override
    public net.minecraft.item.ItemStack getItemStack() {
        return tile.getCellStack();
    }

    private static class TileSaveProvider implements ISaveProvider {

        private final TilePortableOutputChest tile;

        TileSaveProvider(TilePortableOutputChest tile) {
            this.tile = tile;
        }

        @Override
        public void saveChanges(IMEInventory cellInventory) {
            // mark dirty so tile saves the updated cell NBT
            tile.markDirty();
        }
    }
}
