package eu.isas.peptideshaker.preferences;

import com.compomics.util.io.file.SerializationUtils;
import eu.isas.peptideshaker.PeptideShaker;
import java.io.File;

/**
 * Implementing this class will give you access to the saved user preferences.
 *
 * @author Marc Vaudel
 */
public abstract class UserPreferencesParent {

    /**
     * The user preferences.
     */
    protected UserPreferences userPreferences;

    /**
     * Loads the user preferences.
     */
    public void loadUserPreferences() {

        try {
            File file = new File(PeptideShaker.getUserPreferencesFile());
            if (!file.exists()) {
                userPreferences = new UserPreferences();
                saveUserPreferences();
            } else {
                userPreferences = (UserPreferences) SerializationUtils.readObject(file);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while loading " + PeptideShaker.getUserPreferencesFile() + " (see below). User preferences set back to default.");
            e.printStackTrace();
            userPreferences = new UserPreferences();
        }
    }

    /**
     * Saves the user preferences.
     */
    public void saveUserPreferences() {

        try {
            File file = new File(PeptideShaker.getUserPreferencesFile());
            boolean parentExists = true;

            if (!file.getParentFile().exists()) {
                parentExists = file.getParentFile().mkdir();
            }

            if (parentExists) {
                SerializationUtils.writeObject(userPreferences, file);
            } else {
                System.out.println("Parent folder does not exist: \'" + file.getParentFile() + "\'. User preferences not saved.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
