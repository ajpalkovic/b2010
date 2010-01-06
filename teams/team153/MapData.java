package team153;

import java.util.*;
import battlecode.common.*;

public class MapData {

    public int x, y;
    public int lastUpdate = -1;
    public MapLocation location = null;
    public Robot groundRobot = null, airRobot = null;
    public RobotInfo groundRobotInfo = null, airRobotInfo = null;
    public TerrainTile tile = null;
    public int pathCost;
    
    public String toString() {
        return "(" + x + " " + y + ")";
    }

    public String toStringFull() {
        String ret = "(" + x + " " + y + ")";
        if(groundRobot != null) {
            ret += " groundRobot: " + groundRobot;
        }
        if(airRobot != null) {
            ret += " airRobot: " + airRobot;
        }
        if(tile != null) {
            ret += " tile: " + tile;
        }
        ret += " lastUpdate: " + lastUpdate;

        return ret;
    }

    public boolean equals(Object other) {
        MapData data = (MapData) other;
        return data.x == x && data.y == y;
    }

    public MapData(MapLocation location) {
        this.x = location.getX();
        this.y = location.getY();
    }

    public MapData(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public MapData(int x, int y, int height) {
        this.x = x;
        this.y = y;
    }

    public MapData(int x, int y, int terrainHeight, int blockHeight, boolean isFluxDeposit) {
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
