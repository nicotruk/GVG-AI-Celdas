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
AVAILABLE_ACTIONS_QUANTITY = 5  # Atacar - Izquierda - Derecha - Abajo - Arriba
AGENT_MATRIX_WIDTH = 7  # siempre impar! para que la matriz sea centrada en el agente
AGENT_MATRIX_HEIGHT = 7  # siempre impar! para que la matriz sea centrada en el agente
STATE_SIZE = AGENT_MATRIX_WIDTH * AGENT_MATRIX_HEIGHT + 3  # 117


class Agent(AbstractPlayer):
    def __init__(self):
        AbstractPlayer.__init__(self)
        self.lastGameState = None

        self.replayMemory = ReplayMemory(REPLAY_MEMORY_CAPACITY)
        self.exploreExploitDecision = ExploreExploitDecision()

        # self.model = keras.Sequential()
        # self.model.add(keras.layers.Dense(STATE_SIZE, input_dim=STATE_SIZE, activation="relu"))  # fully-connected layer
        # self.model.add(keras.layers.Dense(AVAILABLE_ACTIONS_QUANTITY))  # salida
        # self.model.compile(optimizer="adam", learning_rate=1e-2, loss="mean_square")

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
        perception = self.get_perception(sso)
        print(perception)
        print("*******************************************************************************************")
        print(self.buildState(perception, sso.avatarOrientation))

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

    # obtenido del archivo Types.py - Actions to int
    actions = ['ACTION_USE', 'ACTION_LEFT', 'ACTION_RIGHT', 'ACTION_DOWN', 'ACTION_UP']

    def calculateDistanceToExit(self, playerPositionX, playerPositionY, exitPositionX, exitPositionY):
        return math.sqrt(pow(exitPositionX - playerPositionX, 2) + pow(exitPositionY - playerPositionY, 2))

    def buildState(self, gameState, avatarOrientation):
        # Buscamos la posicion del agente
        player_x = 0
        player_y = 0
        exit_x = 0
        exit_y = 0
        for i in range(0, len(gameState) - 1):
            for j in range(0, len(gameState[i]) - 1):
                if gameState[i][j] == 'A':
                    player_x = i
                    player_y = j
                if gameState[i][j] == 'S':
                    exit_x = i
                    exit_y = j

        # Inicializacion del resultado vacio
        result = []
        for i in range(0, STATE_SIZE):
            result.append(0)

        # Tomamos una matriz de AGENT_MATRIX_WIDTH x AGENT_MATRIX_HEIGHT  con centro en el agente
        i = 0
        for x in range(player_x - math.trunc((AGENT_MATRIX_WIDTH - 1) / 2),
                       player_x + math.trunc((AGENT_MATRIX_WIDTH - 1) / 2) + 1):
            for y in range(player_y - math.trunc((AGENT_MATRIX_HEIGHT - 1) / 2),
                           player_y + math.trunc((AGENT_MATRIX_HEIGHT - 1) / 2) + 1):
                if x >= 0 and y >= 0:
                    try:
                        result[i] = gameState[x][y]
                    except IndexError:
                        result[i] = 0
                else:
                    result[i] = 0
                i += 1

        # Agregamos la distancia del agente a la salida
        result[i] = self.calculateDistanceToExit(player_x, player_y, exit_x, exit_y)

        # Agregamos la posicion del agente
        i += 1
        result[i] = avatarOrientation[0]
        i += 1
        result[i] = avatarOrientation[1]
        return result
