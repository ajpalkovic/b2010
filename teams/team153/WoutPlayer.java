package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {

    public WoutPlayer(RobotController controller) {
        super(controller);
    }
    
    public void step() {
        pr("step");
        if(energon.isEnergonLow()) {
            while(!energon.isEnergonFull()) {
                pr("in loop");
                energon.requestEnergonTransfer();
                controller.yield();
            }
        }
        pr("out of loop");

        NovaMapData square = findSquareWithFlux();
    }

    public boolean tileSensedCallback(NovaMapData data) {
        return super.tileSensedCallback(data);
    }

    public NovaMapData findSquareWithFlux() {
        return null;
    }
}
