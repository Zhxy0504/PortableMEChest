package portablemechest.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;
import portablemechest.inventory.ItemIOPortInventory;
import portablemechest.item.ItemPortableIOPort;

/**
 * Two-slot IO port: transfers items from slot 0 -> slot 1 if target accepts.
 */
public class ContainerPortableIOPort extends Container {

    private final ItemIOPortInventory inv;
    private final ItemStack ioPortStack;
    private final int lockedSlot;

    public ContainerPortableIOPort(InventoryPlayer playerInv, ItemStack ioPortStack, int slotIndex) {
        this.ioPortStack = ioPortStack;
        this.lockedSlot = slotIndex;
        this.inv = new ItemIOPortInventory(ioPortStack);

        // Input slots (left 2x3)
        int startX = 19;
        int startY = 17;
        int idx = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                this.addSlotToContainer(new Slot(inv, idx++, startX + x * 18, startY + y * 18) {

                    @Override
                    public int getSlotStackLimit() {
                        return 1;
                    }

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return ItemPortableIOPort.isValidCell(stack);
                    }
                });
            }
        }

        // Output slots (right 2x3)
        startX = 122;
        startY = 17;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                this.addSlotToContainer(new Slot(inv, idx++, startX + x * 18, startY + y * 18) {

                    @Override
                    public int getSlotStackLimit() {
                        return 1;
                    }

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return ItemPortableIOPort.isValidCell(stack);
                    }
                });
            }
        }

        bindPlayerInventory(playerInv);
    }

    private void bindPlayerInventory(InventoryPlayer playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            final int slotIndex = hotbar;
            this.addSlotToContainer(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142) {

                @Override
                public boolean canTakeStack(EntityPlayer player) {
                    return slotIndex != lockedSlot || player.capabilities.isCreativeMode;
                }
            });
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        transferIfPossible();
    }

    private void transferIfPossible() {
        IMEInventoryHandler<IAEItemStack>[] inputs = getHandlers(0, ItemPortableIOPort.INPUT_SLOTS);
        IMEInventoryHandler<IAEItemStack>[] outputs = getHandlers(
            ItemPortableIOPort.INPUT_SLOTS,
            ItemPortableIOPort.OUTPUT_SLOTS);
        if (inputs.length == 0 || outputs.length == 0) {
            return;
        }
        BaseActionSource source = new BaseActionSource();
        for (IMEInventoryHandler<IAEItemStack> srcHandler : inputs) {
            if (srcHandler == null) {
                continue;
            }
            IItemList<IAEItemStack> list = AEApi.instance()
                .storage()
                .createItemList();
            srcHandler.getAvailableItems(list);
            for (IAEItemStack stack : list) {
                IAEItemStack toMove = AEItemStack.create(stack.getItemStack());
                long remaining = toMove.getStackSize();
                for (IMEInventoryHandler<IAEItemStack> dstHandler : outputs) {
                    if (dstHandler == null || remaining <= 0) {
                        continue;
                    }
                    IAEItemStack injectStack = toMove.copy();
                    injectStack.setStackSize(remaining);
                    IAEItemStack leftover = dstHandler.injectItems(injectStack, Actionable.MODULATE, source);
                    long inserted = remaining - (leftover == null ? 0 : leftover.getStackSize());
                    if (inserted > 0) {
                        IAEItemStack extractReq = AEItemStack.create(stack.getItemStack());
                        extractReq.setStackSize(inserted);
                        srcHandler.extractItems(extractReq, Actionable.MODULATE, source);
                        remaining -= inserted;
                    }
                }
            }
        }
        inv.markDirty();
    }

    @SuppressWarnings("unchecked")
    private IMEInventoryHandler<IAEItemStack>[] getHandlers(int start, int count) {
        IMEInventoryHandler<IAEItemStack>[] handlers = new IMEInventoryHandler[count];
        for (int i = 0; i < count; i++) {
            ItemStack cell = inv.getStackInSlot(start + i);
            if (cell != null) {
                handlers[i] = AEApi.instance()
                    .registries()
                    .cell()
                    .getCellInventory(cell, null, StorageChannel.ITEMS);
            }
        }
        return handlers;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        ItemPortableIOPort.setCells(ioPortStack, ItemPortableIOPort.getCells(ioPortStack));
        player.inventory.setInventorySlotContents(lockedSlot, ioPortStack);
        player.inventory.markDirty();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index < 2) {
                if (!this.mergeItemStack(stackInSlot, 2, this.inventorySlots.size(), true)) {
                    return null;
                }
            } else {
                if (!ItemPortableIOPort.isValidCell(stackInSlot) || !this.mergeItemStack(stackInSlot, 0, 2, false)) {
                    return null;
                }
            }

            if (stackInSlot.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
