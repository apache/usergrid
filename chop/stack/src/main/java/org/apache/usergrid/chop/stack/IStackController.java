package org.apache.usergrid.chop.stack;


public interface IStackController {
    /**
     * Resets this IStackController if it has been prematurely stopped.
     */
    void reset();


    /**
     * Gets the State of this IStackController.
     *
     * @return the current state
     */
    SetupStackState getState();


    /**
     * Starts this IStackController which begins running the suite of chops.
     */
    void start();

    /**
     * Prematurely stops this IStackController. The IController will naturally stop
     * itself after running all chops to fall back into the State.READY state.
     */
    void stop();


    void setup();


    void deploy();


    void destroy();


    /**
     * Takes a signal parameter to determine whether to start, stop, reset etc.
     */
    void send( SetupStackSignal signal );

}
