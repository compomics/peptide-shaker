package eu.isas.peptideshaker.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class provides the tips of the day.
 *
 * @author Marc Vaudel
 */
public class Tips {

    /**
     * Empty constructor for instantiation purposes.
     */
    private Tips() {

    }

    /**
     * Returns the tips of the day.
     *
     * @return the tips of the day in an ArrayList
     * @throws IOException thrown if an IOException occurs
     */
    public static ArrayList<String> getTips() throws IOException {

        InputStream stream = (new Tips()).getClass().getResource("/tips.txt").openStream();
        InputStreamReader streamReader = new InputStreamReader(stream);
        BufferedReader b = new BufferedReader(streamReader);
        ArrayList<String> tips = new ArrayList<>();
        String line;

        while ((line = b.readLine()) != null) {
            tips.add(line);
        }

        b.close();

        return tips;
    }
}
