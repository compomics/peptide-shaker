/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.preferences;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * This class stores the information about the modification preferences (colors,
 * Names) used for the selected project.
 *
 * @deprecated Class kept for backward compatibility. Please use the utilities
 * version instead.
 * @author Marc Vaudel
 */
public class ModificationProfile implements Serializable {

    /**
     * Serial version number for serialization compatibility
     */
    static final long serialVersionUID = 342611308111304721L;
    /**
     * Mapping of the utilities modification names to the PeptideShaker names
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     */
    private HashMap<String, String> modificationNames = new HashMap<String, String>();
    /**
     * Mapping of the PeptideShaker names to the short names
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     */
    private HashMap<String, String> shortNames = new HashMap<String, String>();
    /**
     * Mapping of the PeptideShaker names to the color used
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     */
    private HashMap<String, Color> colors = new HashMap<String, Color>();

    /**
     * Constructor
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     */
    public ModificationProfile() {
    }

    /**
     * Returns the set of the utilities modification names included in this
     * profile
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @return the set of the utilities modification names included in this
     * profile
     */
    public Set<String> getUtilitiesNames() {
        return modificationNames.keySet();
    }

    /**
     * Returns the modification family names included in this profile
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @return the modification family names included in this profile
     */
    public Set<String> getFamilyNames() {
        return shortNames.keySet();
    }

    /**
     * Returns the modification family name corresponding to the given utilities
     * name
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param utilitiesName the given utilities name
     * @return the corresponding modification family name
     */
    public String getFamilyName(String utilitiesName) {
        return modificationNames.get(utilitiesName);
    }

    /**
     * Returns the short name of the given modification
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param familyName the PeptideShaker name of the modification
     * @return the corresponding short name
     */
    public String getShortName(String familyName) {
        return shortNames.get(familyName);
    }

    /**
     * Returns the color used to code the given modification
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param familyName the PeptideShaker name of the given modification
     * @return the corresponding color
     */
    public Color getColor(String familyName) {
        return colors.get(familyName);
    }

    /**
     * Sets a new family name for the given modification
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param utilitiesName the utilities name of the modification
     * @param familyName the new family name
     */
    public void setPeptideShakerName(String utilitiesName, String familyName) {
        String oldKey = modificationNames.get(utilitiesName);
        if (modificationNames.containsKey(utilitiesName)) {
            if (!oldKey.equals(familyName) && !shortNames.containsKey(familyName)) {
                shortNames.put(familyName, shortNames.get(oldKey));
                colors.put(familyName, colors.get(oldKey));
            }

        }
        modificationNames.put(utilitiesName, familyName);
        if (!modificationNames.containsValue(oldKey)) {
            shortNames.remove(oldKey);
            colors.remove(oldKey);
        }
    }

    /**
     * sets a new short name for the given modification
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param familyName the PeptideShaker name of the modification
     * @param shortName the new short name
     */
    public void setShortName(String familyName, String shortName) {
        shortNames.put(familyName, shortName);
    }

    /**
     * Sets a new color for the modification
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param familyName the family name of the modification
     * @param color the new color
     */
    public void setColor(String familyName, Color color) {
        colors.put(familyName, color);
    }

    /**
     * Removes a modification from the profile.
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     * @param utilitiesName the utilities name of the modification
     */
    public void remove(String utilitiesName) {
        String psName = modificationNames.get(utilitiesName);
        modificationNames.remove(utilitiesName);
        if (!modificationNames.values().contains(psName)) {
            shortNames.remove(psName);
            colors.remove(psName);
        }
    }

    /**
     * Returns a mapping of the PeptideShaker names to the colors used.
     *
     * @deprecated Class kept for backward compatibility. Please use the
     * utilities version instead.
     *
     * @return a mapping of the PeptideShaker names to the colors used
     */
    public HashMap<String, Color> getPtmColors() {
        return colors;
    }
}