package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class Base {

    public NovaPlayer player;
    public RobotController controller;
    public Robot robot;

    public Base(NovaPlayer player) {
        this.player = player;
        robot = player.robot;
        controller = player.controller;
    }

    public Base(RobotController controller) {
        this.controller = controller;
        robot = controller.getRobot();
    }

    public void pr(String s) {
        if(false) {
            return;
        }

        if(controller.getRobot().getID() == 103) {
            System.out.println(s);
        }
    }

    /**
     * This is the same as System.out.println
     */
    public void pa(String s) {
        System.out.println(s);
    }

    /**
     * Like SOP, but may be disabled.
     */
    public void p(String s) {
        if(false) return;
        System.out.println(s);
    }

    class BroadcastMessage {
        static final int everyone = -1;
        static final int lowEnergon = 1;//LOW_ENERGON (has less than 25% energon) //status, sent when pinged
        static final int enemyInSight = 3;//(can see an enemy) //broadcasted  (how many enemies and enemy types)
        static final int newUnit = 5; //NEW_UNIT (type of unit location on map maybe unique id) //broadcasted
        static final int followRequest = 9;
        static final int move = 11; //MOVE BITCH GET OUT DA WAY
        static final int support = 14;
        static final int towerBuildLocationRequest = 15, towerBuildLocationResponse = 16;
    }

    class Status {

        public static final int success = 1, fail = 0;
        public static final int notEnoughEnergon = 2;
        public static final int cantMoveThere = 3, goalBlocked = 4, noGoal = 5;
        public static final int outOfRange = 10, turnsNotIdle = 11, cannotSupportUnit = 12;
    }

    static class Goal {

        public static final int collectingFlux = 2;
        public static final int attackingEnemyArchons = 53;
        public static final int idle = 20, scouting = 21;
        public static final int placingTower = 34, movingToTowerSpawnLocation = 35, placingTeleporter=36, movingToPreviousTowerLocation = 37, askingForTowerLocation = 38;;

        public static String toString(int goal) {
            switch(goal) {
                case collectingFlux:
                    return "Collecting flux";
                case idle:
                    return "Idle";
                case placingTower:
                    return "Placing Tower";
                case attackingEnemyArchons:
                    return "Attacking Enemy Archons";
                case movingToTowerSpawnLocation:
                    return "Moving to Tower Spawn Location";
                case placingTeleporter:
                    return "Placing Teleporter";
                case movingToPreviousTowerLocation:
                    return "Moving to Previous Tower Location";
                case askingForTowerLocation:
                    return "Asking for Tower Location";
            }
            return "? - "+goal;
        }
    }
}
