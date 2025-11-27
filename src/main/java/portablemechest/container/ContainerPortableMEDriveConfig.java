package portablemechest.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import portablemechest.inventory.ItemDriveInventory;
import portablemechest.item.ItemPortableMEDrive;

public class ContainerPortableMEDriveConfig extends Container {

    private final ItemDriveInventory driveInventory;
    private final ItemStack driveStack;
    private final int lockedSlot;

    public ContainerPortableMEDriveConfig(InventoryPlayer playerInv, ItemStack driveStack, int slotIndex) {
        this.driveStack = driveStack;
        this.lockedSlot = slotIndex;
        this.driveInventory = new ItemDriveInventory(driveStack);

        // 10 slots: 2 columns x 5 rows (match AE2 drive GUI)
        int startX = 71;
        int startY = 14;
        int index = 0;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 2; col++) {
                this.addSlotToContainer(new Slot(driveInventory, index++, startX + col * 18, startY + row * 18) {

                    @Override
                    public int getSlotStackLimit() {
                        return 1;
                    }

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return ItemPortableMEDrive.isValidCell(stack);
                    }
                });
            }
        }

        bindPlayerInventory(playerInv);
    }

    private void bindPlayerInventory(InventoryPlayer playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 117 + row * 18));
            }
        }

        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            final int slotIndex = hotbar;
            this.addSlotToContainer(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 175) {

                @Override
                public boolean canTakeStack(EntityPlayer player) {
                    return slotIndex != lockedSlot || player.capabilities.isCreativeMode;
                }
            });
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        ItemPortableMEDrive.setCells(driveStack, ItemPortableMEDrive.getCells(driveStack));
        player.inventory.setInventorySlotContents(lockedSlot, driveStack);
        player.inventory.markDirty();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index < ItemPortableMEDrive.getSlotCount()) {
                if (!this.mergeItemStack(
                    stackInSlot,
                    ItemPortableMEDrive.getSlotCount(),
                    this.inventorySlots.size(),
                    true)) {
                    return null;
                }
            } else {
                if (!ItemPortableMEDrive.isValidCell(stackInSlot)
                    || !this.mergeItemStack(stackInSlot, 0, ItemPortableMEDrive.getSlotCount(), false)) {
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
