package eu.isas.peptideshaker.scoring;

import java.util.HashMap;
import javax.swing.ImageIcon;

/**
 * Enum listing the different levels of match validation.
 *
 * @author Marc Vaudel
 */
public enum MatchValidationLevel {

    none(-1, "No Validation"),
    not_validated(0, "Not Validated"),
    doubtful(1, "Doubtful"),
    confident(2, "Confident");
    /**
     * The index associated to this possibility. The higher the id, the better
     * the confidence.
     */
    private final int index;
    /**
     * The name of this possibility.
     */
    private final String name;

    /**
     * Constructor.
     *
     * @param index the index associated to this possibility
     * @param name the name of this possibility
     */
    private MatchValidationLevel(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * Returns the index associated to this possibility.
     *
     * @return the index associated to this possibility
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the name of this possibility.
     *
     * @return the name of this possibility
     */
    public String getName() {
        return name;
    }

    /**
     * Indicates whether this level is considered as validated.
     *
     * @return a boolean indicating whether this level is considered as
     * validated.
     */
    public boolean isValidated() {
        return this == doubtful || this == confident;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the default map of icons for the validation levels.
     *
     * @param tempClass reference to the class needed to use getResource (use
     * the getClass method)
     * @return he default map of icons
     */
    public static HashMap<Integer, ImageIcon> getIconMap(Class tempClass) {
        HashMap<Integer, ImageIcon> icons = new HashMap<>();
        icons.put(MatchValidationLevel.none.getIndex(), new ImageIcon(tempClass.getResource("/icons/warning.png"))); // @TODO: come up with a separate icon for this one?
        icons.put(MatchValidationLevel.confident.getIndex(), new ImageIcon(tempClass.getResource("/icons/accept.png")));
        icons.put(MatchValidationLevel.not_validated.getIndex(), new ImageIcon(tempClass.getResource("/icons/Error_3.png")));
        icons.put(MatchValidationLevel.doubtful.getIndex(), new ImageIcon(tempClass.getResource("/icons/warning.png")));
        return icons;
    }

    /**
     * Returns the implemented validation levels.
     *
     * @return the implemented validation levels
     */
    public static MatchValidationLevel[] getValidationLevels() {
        MatchValidationLevel[] result = new MatchValidationLevel[4];
        result[0] = MatchValidationLevel.confident;
        result[1] = MatchValidationLevel.doubtful;
        result[2] = MatchValidationLevel.not_validated;
        result[3] = MatchValidationLevel.none;
        return result;
    }

    /**
     * Returns an array of the validation level indexes in ascending order of
     * confidence.
     *
     * @return an array of the validation level indexes
     */
    public static int[] getValidationLevelIndexes() {
        int[] indexes = new int[4];
        indexes[0] = MatchValidationLevel.none.getIndex();
        indexes[1] = MatchValidationLevel.not_validated.getIndex();
        indexes[2] = MatchValidationLevel.doubtful.getIndex();
        indexes[3] = MatchValidationLevel.confident.getIndex();
        return indexes;
    }

    /**
     * Returns the default map of icons for the validation levels.
     *
     * @return he default map of icons
     */
    public static HashMap<Integer, String> getTooltipMap() {
        HashMap<Integer, String> tooltips = new HashMap<>();
        for (MatchValidationLevel matchValidationLevel : getValidationLevels()) {
            tooltips.put(matchValidationLevel.getIndex(), matchValidationLevel.getName());
        }
        return tooltips;
    }

    /**
     * Returns a list of the names of the implemented validation levels.
     *
     * @return a list of the names of the implemented validation levels
     */
    public static String[] getValidationLevelsNames() {
        MatchValidationLevel[] possibilities = getValidationLevels();
        String[] result = new String[possibilities.length];
        for (int i = 0; i < possibilities.length; i++) {
            result[i] = possibilities[i].getName();
        }
        return result;
    }

    /**
     * Returns the match validation level associated to a given name.
     *
     * @param name the name
     *
     * @return the match validation level associated to a given name
     */
    public static MatchValidationLevel getMatchValidationLevel(String name) {
        for (MatchValidationLevel matchValidationLevel : getValidationLevels()) {
            if (matchValidationLevel.getName().equals(name)) {
                return matchValidationLevel;
            }
        }
        throw new IllegalArgumentException("No match validation level found for name " + name + ".");
    }

    /**
     * Returns the match validation level associated to a given index.
     *
     * @param index the index
     *
     * @return the match validation level associated to a given index
     */
    public static MatchValidationLevel getMatchValidationLevel(int index) {
        for (MatchValidationLevel matchValidationLevel : getValidationLevels()) {
            if (matchValidationLevel.getIndex() == index) {
                return matchValidationLevel;
            }
        }
        throw new IllegalArgumentException("No match validation level found for index " + index + ".");
    }
}
