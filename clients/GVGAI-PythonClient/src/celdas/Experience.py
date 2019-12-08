class Experience:
    def __init__(self, actual_state, action, reward, next_state):
        self.actualState = actual_state
        self.action = action
        self.reward = reward
        self.nextState = next_state

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
