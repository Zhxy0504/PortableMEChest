package portablemechest.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.InventoryAdaptor;
import appeng.util.item.AEItemStack;
import portablemechest.item.ItemPortableMEChest;

/**
 * Tile that holds one storage cell and periodically pushes items out a configured side.
 * Pulling from other sides goes into a small buffer that tries to inject into the cell.
 */
public class TilePortableOutputChest extends TileEntity implements ISidedInventory {

    private static final int SLOT_CELL = 0;
    private static final int SLOT_BUFFER = 1;
    private static final int[] CELL_SLOT = new int[] { SLOT_CELL };
    private static final int[] BUFFER_SLOT = new int[] { SLOT_BUFFER };

    private final ItemStack[] inv = new ItemStack[2];
    private ForgeDirection outputFace = ForgeDirection.UNKNOWN;
    private int tickCounter = 0;
    private ItemStack cachedCell;
    private IMEInventoryHandler<IAEItemStack> cachedHandler;
    private NBTTagCompound configData = new NBTTagCompound();
    private static final BaseActionSource ACTION_SOURCE = new BaseActionSource() {

        @Override
        public boolean isMachine() {
            return true;
        }
    };

    public boolean hasCell() {
        return inv[SLOT_CELL] != null;
    }

    public ItemStack getCellStack() {
        return inv[SLOT_CELL];
    }

    public void setCellStack(ItemStack stack) {
        inv[SLOT_CELL] = stack;
        invalidateHandler();
        markDirty();
    }

    public ForgeDirection getOutputFace() {
        return outputFace;
    }

    public void setOutputFace(ForgeDirection face) {
        outputFace = face == null ? ForgeDirection.UNKNOWN : face;
        markDirty();
    }

    private void invalidateHandler() {
        cachedCell = null;
        cachedHandler = null;
    }

    private IMEInventoryHandler<IAEItemStack> getHandler() {
        ItemStack cell = inv[SLOT_CELL];
        if (cell == null || cell.stackSize <= 0) {
            cachedHandler = null;
            cachedCell = null;
            return null;
        }
        if (cachedHandler != null && ItemStack.areItemStacksEqual(cell, cachedCell)) {
            return cachedHandler;
        }
        cachedCell = cell.copy();
        cachedHandler = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(cell, new TileSaveProvider(), StorageChannel.ITEMS);
        return cachedHandler;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        tickCounter++;
        IMEInventoryHandler<IAEItemStack> handler = getHandler();
        if (handler != null) {
            // First, inject buffer items into the cell.
            ItemStack buffer = inv[SLOT_BUFFER];
            if (buffer != null) {
                IAEItemStack injected = handler
                    .injectItems(AEItemStack.create(buffer), Actionable.MODULATE, ACTION_SOURCE);
                if (injected == null || injected.getStackSize() == 0) {
                    inv[SLOT_BUFFER] = null;
                } else {
                    inv[SLOT_BUFFER] = injected.getItemStack();
                }
                markDirty();
            }

            // Every 5 ticks -> up to 4 stacks per second.
            if (tickCounter % 5 == 0 && outputFace != ForgeDirection.UNKNOWN) {
                TileEntity targetTe = worldObj.getTileEntity(
                    xCoord + outputFace.offsetX,
                    yCoord + outputFace.offsetY,
                    zCoord + outputFace.offsetZ);
                InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(targetTe, outputFace.getOpposite());
                if (adaptor != null) {
                    appeng.api.storage.data.IItemList<IAEItemStack> list = handler.getAvailableItems(
                        handler.getChannel()
                            .createList());
                    for (IAEItemStack aeStack : list) {
                        if (aeStack == null) {
                            continue;
                        }
                        IAEItemStack toExtract = aeStack.copy();
                        long maxPerPulse = Math.min(64, aeStack.getStackSize());
                        toExtract.setStackSize(maxPerPulse);
                        IAEItemStack extracted = handler.extractItems(toExtract, Actionable.MODULATE, ACTION_SOURCE);
                        if (extracted == null || extracted.getStackSize() == 0) {
                            continue;
                        }
                        ItemStack toSend = extracted.getItemStack();
                        ItemStack leftover = adaptor.addItems(toSend);
                        if (leftover != null) {
                            handler.injectItems(AEItemStack.create(leftover), Actionable.MODULATE, ACTION_SOURCE);
                        }
                        break; // only one stack per pulse
                    }
                }
            }
        }
    }

    @Override
    public int getSizeInventory() {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = inv[slot];
        if (stack != null) {
            if (stack.stackSize <= amount) {
                inv[slot] = null;
            } else {
                stack = stack.splitStack(amount);
                if (inv[slot].stackSize == 0) {
                    inv[slot] = null;
                }
            }
        }
        if (slot == SLOT_CELL) {
            invalidateHandler();
        }
        markDirty();
        return stack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = inv[slot];
        inv[slot] = null;
        if (slot == SLOT_CELL) {
            invalidateHandler();
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inv[slot] = stack;
        if (slot == SLOT_CELL) {
            invalidateHandler();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "tile.portable_output_chest";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return player.worldObj == worldObj && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_CELL) {
            return ItemPortableMEChest.isValidCell(stack);
        }
        return slot == SLOT_BUFFER;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (dir == outputFace) {
            return new int[0];
        }
        return BUFFER_SLOT;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        return slot == SLOT_BUFFER && dir != outputFace;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (slot == SLOT_BUFFER) {
            return dir != outputFace;
        }
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagCompound invTag = new NBTTagCompound();
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                inv[i].writeToNBT(stackTag);
                invTag.setTag("Slot" + i, stackTag);
            }
        }
        tag.setTag("Inventory", invTag);
        tag.setByte("OutputFace", (byte) outputFace.ordinal());
        tag.setTag("Config", configData.copy());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagCompound invTag = tag.getCompoundTag("Inventory");
        for (int i = 0; i < inv.length; i++) {
            String key = "Slot" + i;
            if (invTag.hasKey(key)) {
                inv[i] = ItemStack.loadItemStackFromNBT(invTag.getCompoundTag(key));
            } else {
                inv[i] = null;
            }
        }
        outputFace = ForgeDirection.getOrientation(tag.getByte("OutputFace"));
        configData = tag.hasKey("Config") ? tag.getCompoundTag("Config") : new NBTTagCompound();
        invalidateHandler();
    }

    public void dropContents() {
        for (ItemStack stack : inv) {
            if (stack != null) {
                java.util.List<ItemStack> drops = java.util.Collections.singletonList(stack);
                appeng.util.Platform.spawnDrops(worldObj, xCoord, yCoord, zCoord, drops);
            }
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    public NBTTagCompound getConfigData() {
        return configData;
    }

    public void setConfigData(NBTTagCompound tag) {
        configData = tag;
        markDirty();
    }

    private class TileSaveProvider implements ISaveProvider {

        @Override
        public void saveChanges(IMEInventory cellInventory) {
            // Changes already applied to the cell stack; just mark dirty so tile saves.
            markDirty();
        }
    }
}
