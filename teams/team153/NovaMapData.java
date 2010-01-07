package team153;

import java.util.*;
import battlecode.common.*;

public class NovaMapData {

    public int x, y;
    public int lastUpdate = -1;
    public MapLocation location = null;
    public Robot groundRobot = null, airRobot = null;
    public RobotInfo groundRobotInfo = null, airRobotInfo = null;
    public TerrainTile tile = null;
    public int pathCost, flux;
    
    public String toString() {
        return "(" + x + " " + y + ")";
    }

    public String toStringFull() {
        String ret = "(" + x + " " + y + ")";
        ret += " flux: "+flux;
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
