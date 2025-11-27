package portablemechest.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.implementations.ContainerMEMonitorable;
import cpw.mods.fml.common.network.IGuiHandler;
import portablemechest.container.ContainerPortableIOPort;
import portablemechest.container.ContainerPortableMEChestAccess;
import portablemechest.container.ContainerPortableMEChestCell;
import portablemechest.container.ContainerPortableMEDriveConfig;
import portablemechest.items.content.PortableIOPortGuiObject;
import portablemechest.items.content.PortableMEChestGuiObject;
import portablemechest.items.content.PortableMEDriveGuiObject;

public class PortableMEChestGuiHandler implements IGuiHandler {

    public static final int GUI_CONFIG = 0;
    public static final int GUI_ACCESS = 1;
    public static final int GUI_DRIVE_CONFIG = 2;
    public static final int GUI_DRIVE_ACCESS = 3;
    public static final int GUI_IOPORT = 4;
    public static final int GUI_BLOCK_OUTPUT_ACCESS = 5;
    public static final int GUI_BLOCK_OUTPUT_CONFIG = 6;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        ItemStack held = player.getCurrentEquippedItem();

        if (id == GUI_CONFIG) {
            if (held == null) {
                return null;
            }
            return new ContainerPortableMEChestCell(player.inventory, held, player.inventory.currentItem);
        }

        if (id == GUI_ACCESS) {
            if (held == null) {
                return null;
            }
            PortableMEChestGuiObject obj = new PortableMEChestGuiObject(held, player.inventory.currentItem);
            if (obj.hasCell()) {
                return new ContainerPortableMEChestAccess(player.inventory, obj);
            }
        }

        if (id == GUI_DRIVE_CONFIG) {
            if (held == null) {
                return null;
            }
            return new ContainerPortableMEDriveConfig(player.inventory, held, player.inventory.currentItem);
        }

        if (id == GUI_DRIVE_ACCESS) {
            if (held == null) {
                return null;
            }
            PortableMEDriveGuiObject obj = new PortableMEDriveGuiObject(held, player.inventory.currentItem);
            return new ContainerPortableMEChestAccess(player.inventory, obj);
        }

        if (id == GUI_IOPORT) {
            if (held == null) {
                return null;
            }
            return new ContainerPortableIOPort(player.inventory, held, player.inventory.currentItem);
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        ItemStack held = player.getCurrentEquippedItem();

        if (id == GUI_CONFIG) {
            if (held == null) {
                return null;
            }
            ContainerPortableMEChestCell container = new ContainerPortableMEChestCell(
                player.inventory,
                held,
                player.inventory.currentItem);
            return new GuiPortableMEChestCell(container, player);
        }

        if (id == GUI_ACCESS) {
            if (held == null) {
                return null;
            }
            PortableMEChestGuiObject obj = new PortableMEChestGuiObject(held, player.inventory.currentItem);
            if (!obj.hasCell()) {
                return null;
            }
            ContainerPortableMEChestAccess container = new ContainerPortableMEChestAccess(player.inventory, obj);
            return new GuiMEMonitorable(player.inventory, obj, (ContainerMEMonitorable) container);
        }

        if (id == GUI_DRIVE_CONFIG) {
            if (held == null) {
                return null;
            }
            ContainerPortableMEDriveConfig container = new ContainerPortableMEDriveConfig(
                player.inventory,
                held,
                player.inventory.currentItem);
            return new GuiPortableMEDriveConfig(container, player);
        }

        if (id == GUI_DRIVE_ACCESS) {
            if (held == null) {
                return null;
            }
            PortableMEDriveGuiObject obj = new PortableMEDriveGuiObject(held, player.inventory.currentItem);
            ContainerPortableMEChestAccess container = new ContainerPortableMEChestAccess(player.inventory, obj);
            return new GuiMEMonitorable(player.inventory, obj, (ContainerMEMonitorable) container);
        }

        if (id == GUI_IOPORT) {
            if (held == null) {
                return null;
            }
            ContainerPortableIOPort container = new ContainerPortableIOPort(
                player.inventory,
                held,
                player.inventory.currentItem);
            PortableIOPortGuiObject obj = new PortableIOPortGuiObject(held, player.inventory.currentItem);
            return new GuiPortableIOPort(container, player, obj);
        }

        return null;
    }
}
