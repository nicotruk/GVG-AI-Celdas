import random


class ReplayMemory(object):

    def __init__(self, capacity):
        self.capacity = capacity
        self.memory = []

    def push(self, experience):
        if len(self.memory) < self.capacity:
            self.memory.append(experience)

    def sample(self, batch_size):
        if batch_size < len(self.memory):
            return random.sample(self.memory, batch_size)
        else:
            return random.sample(self.memory, len(self.memory))

    def __len__(self):
        return len(self.memory)
