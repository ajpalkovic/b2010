package team153;

import java.util.*;
import battlecode.common.*;

public class NovaMapData {

    public int x, y;
    public MapLocation location = null;
    public TerrainTile tile = null;
    
    public String toString() {
        return "(" + x + " " + y + ")";
    }

    public String debug_toStringFull() {
        String ret = "(" + x + " " + y + ")";
        //ret += " flux: "+flux;
        if(tile != null) {
            ret += " tile: " + tile;
        }

        return ret;
    }

    public boolean onMap() {
        return tile != TerrainTile.OFF_MAP;
    }

    public boolean equals(Object other) {
        NovaMapData data = (NovaMapData) other;
        return data.x == x && data.y == y;
    }

    public NovaMapData(MapLocation location) {
        this.x = location.getX();
        this.y = location.getY();
    }

    public NovaMapData(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public MapLocation toMapLocation() {
        if(location == null) {
            location = new MapLocation(x, y);
        }
        return location;
    }
}
