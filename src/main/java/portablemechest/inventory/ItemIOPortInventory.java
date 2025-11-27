package portablemechest.inventory;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import portablemechest.item.ItemPortableIOPort;

public class ItemIOPortInventory extends InventoryBasic {

    private final ItemStack ioPortStack;

    public ItemIOPortInventory(ItemStack ioPortStack) {
        super("PortableIOPort", false, ItemPortableIOPort.INPUT_SLOTS + ItemPortableIOPort.OUTPUT_SLOTS);
        this.ioPortStack = ioPortStack;
        ItemStack[] cells = ItemPortableIOPort.getCells(ioPortStack);
        for (int i = 0; i < cells.length; i++) {
            this.setInventorySlotContents(i, cells[i]);
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        ItemStack[] cells = new ItemStack[this.getSizeInventory()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = this.getStackInSlot(i);
        }
        ItemPortableIOPort.setCells(ioPortStack, cells);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return ItemPortableIOPort.isValidCell(stack);
    }
}
