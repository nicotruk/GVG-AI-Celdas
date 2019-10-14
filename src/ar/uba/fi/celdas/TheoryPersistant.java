package ar.uba.fi.celdas;

import java.io.*;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import ontology.Types;

public class TheoryPersistant {

    public static final String FILEANME = "theories.json";
    private static Theories theories;
    private static Theory lastUsedTheory;

    public static void save(Theories theories, Theory lastUsedTheory) {
        TheoryPersistant.theories = theories;
        TheoryPersistant.lastUsedTheory = lastUsedTheory;
    }

    private static void updateLastUsedTheory(Types.WINNER playerWinState) {
        if (lastUsedTheory != null) {
            for (Theory theory : theories.getTheories().get(lastUsedTheory.hashCodeOnlyCurrentState())) {
                if (theory.hashCode() == lastUsedTheory.hashCode()) {
                    char[][] result;
                    switch(playerWinState) {
                        case PLAYER_LOSES:
                            if(theory.getSuccessCount() > 0) {
                                theory.setSuccessCount(theory.getSuccessCount() - 1);
                            }
                            theory.setUtility(0);
                            result = new char[][]{{'D','I','E'}};
                            theory.setPredictedState(result);
                            break;
                        case PLAYER_WINS:
                            theory.setSuccessCount(theory.getSuccessCount() + 1);
                            theory.setUtility(10);
                            result = new char[][]{{'W','I','N'}};
                            theory.setPredictedState(result);
                            break;
                    }
                }
            }
        }
    }

    public static void write(Types.WINNER playerWinState) {
        updateLastUsedTheory(playerWinState);
        try {
            try (Writer writer = new FileWriter(FILEANME)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(theories, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Theories load() {
        try {
            Gson gson = new Gson();
            File file = new File(FILEANME);
            if (file.exists()) {
                JsonReader reader = new JsonReader(new FileReader(FILEANME));

                Type listType = new TypeToken<Theories>() {
                }.getType();

                Theories theories = gson.fromJson(reader, listType); // contains the whole reviews list
                return theories;
            } else {
                return new Theories();
            }
        } catch (FileNotFoundException ex) {
            return new Theories();
        }
    }

}
