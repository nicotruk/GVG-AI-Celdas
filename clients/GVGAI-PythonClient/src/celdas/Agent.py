import random

from AbstractPlayer import AbstractPlayer
from Types import *
from celdas.ExploreExploitDecision import ExploreExploitDecision
from celdas.ReplayMemory import ReplayMemory

from utils.Types import LEARNING_SSO_TYPE
from utils.SerializableStateObservation import Observation
import math
import numpy as np
from pprint import pprint
import tensorflow as tf
from tensorflow import keras

tf.compat.v1.enable_v2_behavior()

REPLAY_MEMORY_CAPACITY = 2000
BATCH_SIZE = 100  # Entrena la red con muestras de 100 movimientos
AVAILABLE_ACTIONS_QUANTITY = 5  # Arriba - Abajo - Izquierda - Derecha - Atacar


class Agent(AbstractPlayer):
    def __init__(self):
        AbstractPlayer.__init__(self)
        self.lastGameState = None

        self.replayMemory = ReplayMemory(REPLAY_MEMORY_CAPACITY)
        self.exploreExploitDecision = ExploreExploitDecision()

    """
    * Public method to be called at the start of every level of a game.
    * Perform any level-entry initialization here.
    * @param sso Phase Observation of the current game.
    * @param elapsedTimer Timer (1s)
    """

    def init(self, sso, elapsedTimer):
        self.lastGameState = None

    """
     * Method used to determine the next move to be performed by the agent.
     * This method can be used to identify the current state of the game and all
     * relevant details, then to choose the desired course of action.
     *
     * @param sso Observation of the current state of the game to be used in deciding
     *            the next action to be taken by the agent.
     * @param elapsedTimer Timer (40ms)
     * @return The action to be performed by the agent.
     """

    def act(self, sso, elapsedTimer):

        # pprint(vars(sso))
        print(self.get_perception(sso))

        if self.lastGameState is not None:
            pass  # TODO: Crear experiencia con el estado actual + accion + recompensa + estado siguiente

        index = self.get_next_action()
        return sso.availableActions[index]

    def get_next_action(self):
        if self.exploreExploitDecision.decide_to_explore():
            # TODO: Explorar! Cambiar linea de abajo
            return random.randint(0, AVAILABLE_ACTIONS_QUANTITY - 1)
        else:
            # Explotar!
            return random.randint(0, AVAILABLE_ACTIONS_QUANTITY - 1)

    def optimize_model(self):
        if len(self.replayMemory) < BATCH_SIZE * 2:
            # No entrenar
            return
        # Empieza el entrenamiento...
        batch = self.replayMemory.sample(BATCH_SIZE)  # Tomar muestra
        # TODO: Continuar...

    """
    * Method used to perform actions in case of a game end.
    * This is the last thing called when a level is played (the game is already in a terminal state).
    * Use this for actions such as teardown or process data.
    *
    * @param sso The current state observation of the game.
    * @param elapsedTimer Timer (up to CompetitionParameters.TOTAL_LEARNING_TIME
    * or CompetitionParameters.EXTRA_LEARNING_TIME if current global time is beyond TOTAL_LEARNING_TIME)
    * @return The next level of the current game to be played.
    * The level is bound in the range of [0,2]. If the input is any different, then the level
    * chosen will be ignored, and the game will play a random one instead.
    """

    def result(self, sso, elapsedTimer):
        return random.randint(0, 2)

    def get_perception(self, sso):
        sizeWorldWidthInPixels = sso.worldDimension[0]
        sizeWorldHeightInPixels = sso.worldDimension[1]
        levelWidth = len(sso.observationGrid)
        levelHeight = len(sso.observationGrid[0])

        spriteSizeWidthInPixels = sizeWorldWidthInPixels / levelWidth
        spriteSizeHeightInPixels = sizeWorldHeightInPixels / levelHeight
        level = np.chararray((levelHeight, levelWidth))
        level[:] = '.'
        avatar_observation = Observation()
        for ii in range(levelWidth):
            for jj in range(levelHeight):
                listObservation = sso.observationGrid[ii][jj]
                if len(listObservation) != 0:
                    aux = listObservation[len(listObservation) - 1]
                    if aux is None: continue
                    level[jj][ii] = self.detectElement(aux)

        return level

    def detectElement(self, o):
        if o.category == 4:
            if o.itype == 3:
                return '0'
            elif o.itype == 0:  # This is a WALL
                return 'w'
            elif o.itype == 4:  # This is a KEY
                return 'L'
            else:  # This is the AGENT
                return 'A'

        elif o.category == 0:
            if o.itype == 5:  # AGENT???
                return 'A'
            elif o.itype == 6:
                return 'B'
            elif o.itype == 1:  # AGENT???
                return 'A'
            else:  # AGENT???
                return 'A'

        elif o.category == 6:  # This is an ENEMY
            return 'e'
        elif o.category == 2:  # This is the EXIT
            return 'S'
        elif o.category == 3:
            if o.itype == 1:  # ENEMY???
                return 'e'
            else:  # ENEMY???
                return 'e'
        elif o.category == 5:
            if o.itype == 5:
                return 'x'
            else:  # ENEMY???
                return 'e'
        else:
            return '?'
