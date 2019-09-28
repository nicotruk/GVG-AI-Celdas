package ar.uba.fi.celdas;

import java.io.*;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class TheoryPersistant {

    public static final String FILEANME = "theories.json";
    private static Theories theories;

    public static void save(Theories theories) {
        TheoryPersistant.theories = theories;
    }

    public static void write() {
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
