package portablemechest.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import portablemechest.inventory.ItemCellInventory;
import portablemechest.item.ItemPortableMEChest;

public class ContainerPortableMEChestCell extends Container {

    private final ItemCellInventory cellInventory;
    private final ItemStack chestStack;
    private final int lockedSlot;

    public ContainerPortableMEChestCell(InventoryPlayer playerInv, ItemStack chestStack, int slotIndex) {
        this.chestStack = chestStack;
        this.lockedSlot = slotIndex;
        this.cellInventory = new ItemCellInventory(chestStack);

        this.addSlotToContainer(new Slot(cellInventory, 0, 80, 37) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return ItemPortableMEChest.isValidCell(stack);
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });

        bindPlayerInventory(playerInv);
    }

    public String getSlotTitle() {
        return "Storage Cell";
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
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        cellInventory.saveToItem(chestStack);
        player.inventory.setInventorySlotContents(lockedSlot, chestStack);
        player.inventory.markDirty();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                if (!this.mergeItemStack(stackInSlot, 1, this.inventorySlots.size(), true)) {
                    return null;
                }
            } else {
                if (!ItemPortableMEChest.isValidCell(stackInSlot) || !this.mergeItemStack(stackInSlot, 0, 1, false)) {
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
