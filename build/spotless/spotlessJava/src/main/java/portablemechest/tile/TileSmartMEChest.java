package portablemechest.tile;

/**
 * Smart ME Chest tile: same logic as portable output chest, kept separate for clarity.
 */
public class TileSmartMEChest extends TilePortableOutputChest {

    @Override
    public String getInventoryName() {
        return "tile.smart_me_chest";
    }
}
