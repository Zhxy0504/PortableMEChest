package portablemechest.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import portablemechest.PortableMEChest;
import portablemechest.container.ContainerPortableMEDriveConfig;

public class GuiPortableMEDriveConfig extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/drive.png");
    private final ContainerPortableMEDriveConfig container;

    public GuiPortableMEDriveConfig(ContainerPortableMEDriveConfig container, EntityPlayer player) {
        super(container);
        this.container = container;
        this.xSize = 195;
        this.ySize = 199;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(PortableMEChest.NAME, 8, 6, 4210752);
        this.fontRendererObj.drawString("Drive Slots", 8, 18, 4210752);
        this.fontRendererObj.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }
}
