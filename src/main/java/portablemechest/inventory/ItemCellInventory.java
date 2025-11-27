package portablemechest.inventory;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import portablemechest.item.ItemPortableMEChest;

/**
 * Single-slot inventory that stores a storage cell inside the chest item NBT.
 */
public class ItemCellInventory extends InventoryBasic {

    private final ItemStack chest;

    public ItemCellInventory(ItemStack chestStack) {
        super("PortableCell", false, 1);
        this.chest = chestStack;
        ItemStack stored = ItemPortableMEChest.getStoredCell(chestStack);
        if (stored != null) {
            this.setInventorySlotContents(0, stored);
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        saveToItem(chest);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return ItemPortableMEChest.isValidCell(stack);
    }

    public void saveToItem(ItemStack chestStack) {
        ItemStack cell = this.getStackInSlot(0);
        if (cell != null && cell.stackSize > 1) {
            cell.stackSize = 1;
        }
        ItemPortableMEChest.setStoredCell(chestStack, cell);
    }
}
