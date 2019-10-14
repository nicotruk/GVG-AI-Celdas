package ar.uba.fi.celdas;

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

    private char[][] lastGameState;
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

        // Actualizamos la ultima teoria usada
        updateLastUsedTheory(perception.getLevel(), stateObs.isAvatarAlive());

        if (theories == null) {
            theories = TheoryPersistant.load();
        }

        Theory theoryToUse = new Theory();
        theoryToUse.setCurrentState(buildTheoryState(perception.getLevel()));
        List<Theory> currentStateAvailableTheories = theories.getTheories().get(theoryToUse.hashCodeOnlyCurrentState());
        if (currentStateAvailableTheories == null || currentStateAvailableTheories.size() == 0) {
            // No existen teorias para el estado de juego actual
            theoryToUse = createNewTheory(perception.getLevel());
        } else {
            // Existen teorias para el estado de juego actual
            float maxUtility = 0;
            List<Theory> maxUtilityTheories = new ArrayList<>();
            for (Theory theory : currentStateAvailableTheories) {
                if (theory.getUtility() > maxUtility) {
                    maxUtility = theory.getUtility();
                    maxUtilityTheories = new ArrayList<>();
                    maxUtilityTheories.add(theory);
                } else if (theory.getUtility() == maxUtility) {
                    maxUtilityTheories.add(theory);
                }
            }
            Random random = new Random();
            if (maxUtilityTheories.size() > 0) {
                theoryToUse = maxUtilityTheories.get(random.nextInt(maxUtilityTheories.size()));
            }

            // Probabilidad de explorar una nueva teoria
            boolean explore = false;
            if (random.nextFloat() > 0.7) {
                explore = true;
            }
            if (theoryToUse.getUtility() == 0f || (theoryToUse.getUtility() < 10f && explore && currentStateAvailableTheories.size() != stateObs.getAvailableActions().size())) {
                // Ninguna teoria cumple con la utilidad de ganar, y existen movimientos por explorar
                theoryToUse = createTheoryWithUnusedAction(perception.getLevel(), currentStateAvailableTheories, stateObs.getAvailableActions());
            }
            theoryToUse.setUsedCount(theoryToUse.getUsedCount() + 1);
        }

        System.out.println("AGENTE decide moverse: " + theoryToUse.getAction());
        lastGameState = perception.getLevel();
        lastUsedTheory = theoryToUse;

        TheoryPersistant.save(theories, lastUsedTheory);
        return theoryToUse.getAction();
    }

    private char[][] buildTheoryState(char[][] gameState) {
        // Buscamos la posicion del agente
        int playerX = 0;
        int playerY = 0;
        for (int i = 0; i < gameState.length; i++) {
            for (int j = 0; j < gameState[i].length; j++) {
                if (gameState[i][j] == 'A') {
                    playerX = i;
                    playerY = j;
                }
            }
        }
        // Inicializacion del resultado vacio
        char[][] result = new char[3][3];
        for (int x = 0; x < result.length; x++) {
            for (int y = 0; y < result[x].length; y++) {
                result[x][y] = ' ';
            }
        }
        // Cargamos en el resultado al agente y su entorno
        // Oeste y Este -----------------------------------
        int auxX = playerX - 1;
        int auxY = playerY;
        for (int x = 0; x < result.length; x++) {
            if (auxX >= 0 && auxX < gameState[0].length) {
                result[x][1] = gameState[auxX][auxY];
            }
            auxX++;
        }
        // Norte y Sur ------------------------------------
        auxX = playerX;
        auxY = playerY - 1;
        for (int y = 0; y < result[1].length; y++) {
            if (auxY >= 0 && auxY < gameState[0].length) {
                result[1][y] = gameState[auxX][auxY];
            }
            auxY++;
        }
        return result;
    }

    private void updateLastUsedTheory(char[][] currentGameState, boolean isPlayerAlive) {
        if (lastUsedTheory != null) {
            for (Theory theory : theories.getTheories().get(lastUsedTheory.hashCodeOnlyCurrentState())) {
                if (theory.getCurrentState() == lastUsedTheory.getCurrentState() && theory.getAction() == lastUsedTheory.getAction()) {
                    theory.setPredictedState(buildTheoryState(currentGameState));
                    if (isPlayerAlive) {
                        theory.setSuccessCount(theory.getSuccessCount() + 1);
                        if(currentGameState.hashCode() == lastGameState.hashCode()) {
                            theory.setUtility(1f);
                        } else {
                            theory.setUtility(5f);
                        }
                    }
                }
            }
        }
    }

    private Theory createNewTheory(char[][] gameState) {
        Theory result = new Theory();
        result.setCurrentState(buildTheoryState(gameState));
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

    private Theory createTheoryWithUnusedAction(char[][] gameState, List<Theory> currentStateAvailableTheories, Collection<Types.ACTIONS> availableActions) {
        Set<Types.ACTIONS> unusedActions = new HashSet<>(availableActions);
        for (Theory theory : currentStateAvailableTheories) {
            unusedActions.remove(theory.getAction());
        }
        Theory result = new Theory();
        result.setCurrentState(buildTheoryState(gameState));
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
