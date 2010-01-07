package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {

    public WoutPlayer(RobotController controller) {
        super(controller);
    }
    
    public void step() {
        sensing.senseSurroundingSquares();
        if(energon.isEnergonLow()) {
            while(!energon.isEnergonFull()) {
                energon.requestEnergonTransfer();
                controller.yield();
            }
        }


    }
}
