package portablemechest.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.container.implementations.ContainerMEPortableCell;
import portablemechest.item.ItemPortableMEChest;
import portablemechest.item.ItemPortableMEDrive;
import portablemechest.items.content.PortableMEChestGuiObject;
import portablemechest.items.content.PortableMEDriveGuiObject;

/**
 * Portable ME access container that writes the embedded storage cell back into the chest item when closed.
 */
public class ContainerPortableMEChestAccess extends ContainerMEPortableCell {

    private final ItemStack chestStack;
    private final Object guiObject;
    private final int lockedSlot;

    public ContainerPortableMEChestAccess(final InventoryPlayer ip, final Object monitorable) {
        super(ip, (appeng.api.implementations.guiobjects.IPortableCell) monitorable);
        this.guiObject = monitorable;
        if (monitorable instanceof PortableMEChestGuiObject) {
            PortableMEChestGuiObject obj = (PortableMEChestGuiObject) monitorable;
            this.chestStack = obj.getItemStack();
            this.lockedSlot = obj.getInventorySlot();
        } else if (monitorable instanceof PortableMEDriveGuiObject) {
            PortableMEDriveGuiObject obj = (PortableMEDriveGuiObject) monitorable;
            this.chestStack = obj.getItemStack();
            this.lockedSlot = obj.getInventorySlot();
        } else {
            this.chestStack = null;
            this.lockedSlot = -1;
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (chestStack != null) {
            if (guiObject instanceof PortableMEChestGuiObject) {
                ItemPortableMEChest
                    .setStoredCell(chestStack, ((PortableMEChestGuiObject) guiObject).getContainedCell());
            } else if (guiObject instanceof PortableMEDriveGuiObject) {
                ItemPortableMEDrive.setCells(chestStack, ((PortableMEDriveGuiObject) guiObject).getContainedCells());
            }
            if (lockedSlot >= 0) {
                player.inventory.setInventorySlotContents(lockedSlot, chestStack);
                player.inventory.markDirty();
            }
        }
    }
}
