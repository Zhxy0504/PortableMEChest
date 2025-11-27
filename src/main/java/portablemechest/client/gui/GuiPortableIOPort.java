package portablemechest.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import portablemechest.container.ContainerPortableIOPort;
import portablemechest.items.content.PortableIOPortGuiObject;

public class GuiPortableIOPort extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/ioport.png");
    private final PortableIOPortGuiObject guiObject;

    public GuiPortableIOPort(ContainerPortableIOPort container, EntityPlayer player, PortableIOPortGuiObject obj) {
        super(container);
        this.guiObject = obj;
        this.xSize = 176;
        this.ySize = 197;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Labels aligned above slot grids (input starts at 19, output at 122)
        this.fontRendererObj.drawString("Input", 19, 8, 4210752);
        this.fontRendererObj.drawString("Output", 122, 8, 4210752);
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
