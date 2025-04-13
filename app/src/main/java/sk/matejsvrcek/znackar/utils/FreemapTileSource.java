package sk.matejsvrcek.znackar.utils;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

public class FreemapTileSource extends OnlineTileSourceBase {

    //Tile source for the map, must be done this way because otherwise
    // the @2x.png extension doesn't work
    public FreemapTileSource() {
        super("FreemapOutdoor",
                0, 19, 256, ".png",
                new String[]{"https://outdoor.tiles.freemap.sk"},
                "© freemap.sk");
    }

    @Override
    public String getTileURLString(long pMapTileIndex) {
        int z = MapTileIndex.getZoom(pMapTileIndex);
        int x = MapTileIndex.getX(pMapTileIndex);
        int y = MapTileIndex.getY(pMapTileIndex);

        return getBaseUrl() + "/" + z + "/" + x + "/" + y + "@2x.png";
    }
}
