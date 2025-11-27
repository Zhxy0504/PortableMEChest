package portablemechest;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import portablemechest.client.gui.PortableMEChestGuiHandler;
import portablemechest.item.ItemPortableIOPort;
import portablemechest.item.ItemPortableMEChest;
import portablemechest.item.ItemPortableMEDrive;

@Mod(
    modid = PortableMEChest.MODID,
    name = PortableMEChest.NAME,
    version = PortableMEChest.VERSION,
    dependencies = "required-after:appliedenergistics2")
public class PortableMEChest {

    public static final String MODID = "portablemechest";
    public static final String NAME = "PortableMEChest";
    public static final String VERSION = "0.1.0";

    @Mod.Instance(PortableMEChest.MODID)
    public static PortableMEChest instance;

    private Item portableChest;
    private Item portableDrive;
    private Item portableIOPort;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        portableChest = new ItemPortableMEChest().setUnlocalizedName(MODID + ".portable_me_chest")
            .setTextureName(MODID + ":portable_me_chest")
            .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(portableChest, "portable_me_chest");

        portableDrive = new ItemPortableMEDrive().setUnlocalizedName(MODID + ".portable_me_drive")
            .setTextureName(MODID + ":portable_me_drive")
            .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(portableDrive, "portable_me_drive");

        portableIOPort = new ItemPortableIOPort().setUnlocalizedName(MODID + ".portable_io_port")
            .setTextureName(MODID + ":portable_ioport")
            .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(portableIOPort, "portable_io_port");

    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new PortableMEChestGuiHandler());
    }

    public Item getPortableChest() {
        return portableChest;
    }

    public Item getPortableDrive() {
        return portableDrive;
    }

    public Item getPortableIOPort() {
        return portableIOPort;
    }

}
