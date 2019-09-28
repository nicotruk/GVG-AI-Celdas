package ar.uba.fi.celdas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {
    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;
    /**
     * List of available actions for the agent
     */
    protected ArrayList<Types.ACTIONS> actions;


    protected Theories theories;

    private Theory lastUsedTheory;

    /**
     * Public constructor with state observation and time due.
     *
     * @param so           state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        randomGenerator = new Random();
        actions = so.getAvailableActions();
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     *
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        Perception perception = new Perception(stateObs);
        System.out.println(perception.toString());

        // Debemos actualizar la ultima teoria usada
        updateLastUsedTheory(perception.getLevel(), stateObs.isAvatarAlive());

        if (theories == null) {
            theories = TheoryPersistant.load();
        }
        Theory theoryToUse = new Theory();
        theoryToUse.setCurrentState(perception.getLevel());
        List<Theory> currentStateAvailableTheories = theories.getTheories().get(theoryToUse.hashCodeOnlyCurrentState());
        if (currentStateAvailableTheories == null || currentStateAvailableTheories.size() == 0) {
            // No existen teorias para el estado de juego actual
            theoryToUse = createNewTheory(perception.getLevel());
        } else {
            // Existen teorias para el estado de juego actual
            float maxUtility = 0;
            for (Theory theory : currentStateAvailableTheories) {
                if (theory.getUtility() > maxUtility) {
                    maxUtility = theory.getUtility();
                    theoryToUse = theory;
                }
            }
            if (theoryToUse.getUtility() < 0.25f && currentStateAvailableTheories.size() != stateObs.getAvailableActions().size()) {
                // Ninguna teoria cumple con una utilidad alta, y existen movimientos por explorar
                theoryToUse = createTheoryWithUnusedAction(perception.getLevel(), currentStateAvailableTheories, stateObs.getAvailableActions());
            }
            theoryToUse.setUsedCount(theoryToUse.getUsedCount() + 1);
        }

        System.out.println("AGENTE decide moverse: " + theoryToUse.getAction());
        lastUsedTheory = theoryToUse;

        TheoryPersistant.save(theories);
        return theoryToUse.getAction();
    }

    private void updateLastUsedTheory(char[][] currentState, boolean isPlayerAlive) {
        if (lastUsedTheory != null) {
            for (Theory theory : theories.getTheories().get(lastUsedTheory.hashCodeOnlyCurrentState())) {
                if (theory.getCurrentState() == lastUsedTheory.getCurrentState() && theory.getAction() == lastUsedTheory.getAction()) {
                    theory.setPredictedState(currentState);
                    if (isPlayerAlive) {
                        theory.setSuccessCount(theory.getSuccessCount() + 1);
                    }
                    theory.setUtility(new Float(theory.getSuccessCount()) / theory.getUsedCount() / theory.getUsedCount());
                }
            }
        }
    }

    private Theory createNewTheory(char[][] currentState) {
        Theory result = new Theory();
        result.setCurrentState(currentState);
        int index = randomGenerator.nextInt(actions.size());
        result.setAction(actions.get(index));
        result.setUsedCount(1);
        try {
            theories.add(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private Theory createTheoryWithUnusedAction(char[][] currentState, List<Theory> currentStateAvailableTheories, Collection<Types.ACTIONS> availableActions) {
        Set<Types.ACTIONS> unusedActions = new HashSet<>(availableActions);
        for (Theory theory : currentStateAvailableTheories) {
            unusedActions.remove(theory.getAction());
        }
        Theory result = new Theory();
        result.setCurrentState(currentState);
        int index = randomGenerator.nextInt(unusedActions.size());
        result.setAction(new ArrayList<>(unusedActions).get(index));
        result.setUsedCount(1);
        try {
            theories.add(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
