package team001;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import java.io.PrintStream;

public class RobotPlayer implements Runnable {

    private final RobotController myRC;

    public RobotPlayer(RobotController rc) {
        this.myRC = rc;
    }

    public void run() {
        while(true) {
            try {
                if(this.myRC.isMovementActive()) {
                    this.myRC.yield();
                }

                if(this.myRC.canMove(this.myRC.getDirection())) {
                    //System.out.println("about to move");
                    this.myRC.moveForward();
                } else {
                    this.myRC.setDirection(this.myRC.getDirection().rotateRight());
                }
                this.myRC.yield();
            } catch(Exception e) {
                //System.out.println("caught exception:");
                //e.printStackTrace();
            }
        }
    }
}
