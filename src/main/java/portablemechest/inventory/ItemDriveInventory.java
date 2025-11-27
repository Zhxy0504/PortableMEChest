package portablemechest.inventory;

import java.util.List;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import portablemechest.item.ItemPortableMEDrive;

/**
 * Simple inventory backing the portable ME drive's cell slots.
 */
public class ItemDriveInventory extends InventoryBasic {

    private final ItemStack driveStack;

    public ItemDriveInventory(ItemStack driveStack) {
        super("PortableDrive", false, ItemPortableMEDrive.getSlotCount());
        this.driveStack = driveStack;

        List<ItemStack> cells = ItemPortableMEDrive.getCells(driveStack);
        for (int i = 0; i < cells.size(); i++) {
            this.setInventorySlotContents(i, cells.get(i));
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        ItemPortableMEDrive.setCells(driveStack, getCellsSnapshot());
    }

    private List<ItemStack> getCellsSnapshot() {
        List<ItemStack> cells = ItemPortableMEDrive.getCells(driveStack);
        for (int i = 0; i < this.getSizeInventory(); i++) {
            cells.set(i, this.getStackInSlot(i));
        }
        return cells;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return ItemPortableMEDrive.isValidCell(stack);
    }
}
