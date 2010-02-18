package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;



/**
 * The purpose of this class is to enable flexible route planning that allows for movement one step at a time.
 */
public abstract class NavigationGoal {
    public boolean completed = false;

    /**
     * This method is called every time moveOnce is called.  It should return the direction in which the robot should move next.
     */
    public abstract Direction getDirection();

    /**
     * This method should return true when the robot is at the goal.
     */
    public boolean done() {
        return false;
    }
}