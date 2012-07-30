/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class caches the identification features calculated by the
 * IdentificationFeaturesGenerator for later reuse
 *
 * @author Marc
 */
public class IdentificationFeaturesCache implements Serializable {

    /**
     * Serial number for backward compatibility
     */
    static final long serialVersionUID = -7291018247377919040L;

    /**
     * An enumerator of the supported object types.
     */
    public enum ObjectType {

        /**
         * the coverable amino acids stored as big object
         */
        coverable_AA,
        /**
         * the sequence coverage of a given protein stored as small object
         */
        sequence_coverage,
        /**
         * the expected sequence coverage of a given protein stored as small
         * object
         */
        expected_coverage,
        /**
         * the spectrum counting index of a given protein stored as small object
         */
        spectrum_counting,
        /**
         * the number of spectra of a given protein stored as small object
         */
        number_of_spectra,
        /**
         * the number of validated spectra of a given protein stored as small
         * object
         */
        number_of_validated_spectra,
    }
    /**
     * The number of values kept in memory for small objects.
     */
    private final int smallObjectsCacheSize = 10000;
    /**
     * The number of values kept in memory for big objects.
     */
    private final int bigObjectsCacheSize = 10;
    /**
     * Separator used to concatenate strings.
     */
    private static final String cacheSeparator = "_ccs_";
    /**
     * The cached protein matches for small objects.
     */
    private ArrayList<String> smallObjectsInCache = new ArrayList<String>();
    /**
     * The cached protein matches for big objects.
     */
    private ArrayList<String> bigObjectsInCache = new ArrayList<String>();
    /**
     * Mapping of the stored big objects
     */
    private HashMap<ObjectType, HashMap<String, Object>> bigObjectsCache = new HashMap<ObjectType, HashMap<String, Object>>();
    /**
     * Mapping of the stored small objects
     */
    private HashMap<ObjectType, HashMap<String, Object>> smallObjectsCache = new HashMap<ObjectType, HashMap<String, Object>>();
    /**
     * The protein list.
     */
    private ArrayList<String> proteinListAfterHiding = null;
    /**
     * Back-up list for when proteins are hidden.
     */
    private ArrayList<String> proteinList = null;
    /**
     * List of the validated proteins.
     */
    private ArrayList<String> validatedProteinList = null;
    /**
     * The peptide list.
     */
    private ArrayList<String> peptideList;
    /**
     * The psm list.
     */
    private ArrayList<String> psmList;
    /**
     * Boolean indicating whether a filtering was already used. If yes, proteins
     * might need to be unhiden.
     */
    private boolean filtered = false;
    /**
     * The max m/z value across the selected spectra.
     */
    private double maxPsmMzValue;
    /**
     * The maximum number of psms across all peptides of the last selected
     * protein.
     */
    private int maxSpectrumCount;
    /**
     * The number of validated psms in the currently selected peptide.
     */
    private int nValidatedPsms;
    /**
     * The current protein key.
     */
    private String currentProteinKey = "";
    /**
     * The current peptide key.
     */
    private String currentPeptideKey = "";

    /**
     * Clears all objects of the given type
     * @param type the object type
     */
    public void removeObjects(ObjectType type) {

        String typeKey = getTypeAsString(type);
        ArrayList<String> toRemove = new ArrayList<String>();
        switch (type) {
            case coverable_AA:
                bigObjectsCache.remove(type);
                for (String key : bigObjectsInCache) {
                    if (key.contains(typeKey)) {
                        toRemove.add(key);
                    }
                }
                for (String key : toRemove) {
                    bigObjectsInCache.remove(key);
                }
            case sequence_coverage:
            case expected_coverage:
            case spectrum_counting:
            case number_of_spectra:
            case number_of_validated_spectra:
                smallObjectsCache.remove(type);
                for (String key : smallObjectsInCache) {
                    if (key.contains(typeKey)) {
                        toRemove.add(key);
                    }
                }
                for (String key : toRemove) {
                    bigObjectsInCache.remove(key);
                }
        }
    }

    /**
     * Adds an object in the cache
     *
     * @param type the type of the object
     * @param objectKey the object key
     * @param object the object to store
     */
    public void addObject(ObjectType type, String objectKey, Object object) {
        switch (type) {
            case coverable_AA:
                if (!bigObjectsCache.containsKey(type)) {
                    bigObjectsCache.put(type, new HashMap<String, Object>());
                }
                bigObjectsCache.get(type).put(objectKey, object);
                bigObjectsInCache.add(getCacheKey(type, objectKey));
                while (bigObjectsInCache.size() >= bigObjectsCacheSize) {
                    ObjectType oldType = getType(bigObjectsInCache.get(0));
                    String oldKey = getObjectKey(bigObjectsInCache.get(0));
                    if (bigObjectsCache.containsKey(oldType)) { //Should always be true. Should...
                        bigObjectsCache.get(oldType).remove(oldKey);
                        if (bigObjectsCache.get(oldType).isEmpty()) {
                            bigObjectsCache.remove(oldType);
                        }
                    }
                    bigObjectsInCache.remove(0);
                    if (bigObjectsInCache.isEmpty()) {
                        break;
                    }
                }
            case sequence_coverage:
            case expected_coverage:
            case spectrum_counting:
            case number_of_spectra:
            case number_of_validated_spectra:
                if (!smallObjectsCache.containsKey(type)) {
                    smallObjectsCache.put(type, new HashMap<String, Object>());
                }
                smallObjectsCache.get(type).put(objectKey, object);
                smallObjectsInCache.add(getCacheKey(type, objectKey));
                while (smallObjectsInCache.size() >= smallObjectsCacheSize) {
                    ObjectType oldType = getType(smallObjectsInCache.get(0));
                    String oldKey = getObjectKey(smallObjectsInCache.get(0));
                    if (smallObjectsCache.containsKey(oldType)) { //Should always be true. Should...
                        smallObjectsCache.get(oldType).remove(oldKey);
                        if (smallObjectsCache.get(oldType).isEmpty()) {
                            smallObjectsCache.remove(oldType);
                        }
                    }
                    smallObjectsInCache.remove(0);
                    if (smallObjectsInCache.isEmpty()) {
                        break;
                    }
                }
        }
    }

    /**
     * Returns an object if present in the cache. Null if not.
     *
     * @param type the type of the object
     * @param objectKey the key of the object
     * @return the desired object
     */
    public Object getObject(ObjectType type, String objectKey) {
        Object result = null;
        switch (type) {
            case coverable_AA:
                if (bigObjectsCache.containsKey(type)) {
                    result = bigObjectsCache.get(type).get(objectKey);
                }
                if (result != null && bigObjectsInCache.size() >= bigObjectsCacheSize) {
                    String cacheKey = getCacheKey(type, objectKey);
                    for (int i = 0; i < Math.min(bigObjectsInCache.size(), 100); i++) {
                        if (bigObjectsInCache.get(i).equals(cacheKey)) {
                            bigObjectsInCache.remove(i);
                            bigObjectsInCache.add(cacheKey);
                            break;
                        }
                    }
                }
                return result;
            case sequence_coverage:
            case expected_coverage:
            case spectrum_counting:
            case number_of_spectra:
            case number_of_validated_spectra:
                if (smallObjectsCache.containsKey(type)) {
                    result = smallObjectsCache.get(type).get(objectKey);
                }
                if (result != null && smallObjectsInCache.size() >= smallObjectsCacheSize) {
                    String cacheKey = getCacheKey(type, objectKey);
                    for (int i = 0; i < Math.min(smallObjectsInCache.size(), 100); i++) {
                        if (smallObjectsInCache.get(i).equals(cacheKey)) {
                            smallObjectsInCache.remove(i);
                            smallObjectsInCache.add(cacheKey);
                            break;
                        }
                    }
                }
                return result;
            default:
                return result;
        }
    }

    /**
     * Returns the current peptide key
     *
     * @return the current peptide key
     */
    public String getCurrentPeptideKey() {
        return currentPeptideKey;
    }

    /**
     * Sets the current peptide key
     *
     * @param currentPeptideKey the current peptide key
     */
    public void setCurrentPeptideKey(String currentPeptideKey) {
        this.currentPeptideKey = currentPeptideKey;
    }

    /**
     * Returns the current protein key
     *
     * @return the current protein key
     */
    public String getCurrentProteinKey() {
        return currentProteinKey;
    }

    /**
     * Sets the current protein key
     *
     * @param currentProteinKey the current protein key
     */
    public void setCurrentProteinKey(String currentProteinKey) {
        this.currentProteinKey = currentProteinKey;
    }

    /**
     * Indicates whether the protein list is filtered
     *
     * @return a boolean indicating whether the protein list is filtered
     */
    public boolean isFiltered() {
        return filtered;
    }

    /**
     * Sets whether the protein list is filtered
     *
     * @param filtered a boolean indicating whether the protein list is filtered
     */
    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    /**
     * Returns the max m/z value in the selected PSMs
     *
     * @return the max m/z value in the selected PSMs
     */
    public double getMaxPsmMzValue() {
        return maxPsmMzValue;
    }

    /**
     * Sets the max m/z value in the selected PSMs
     *
     * @param maxPsmMzValue the max m/z value in the selected PSMs
     */
    public void setMaxPsmMzValue(double maxPsmMzValue) {
        this.maxPsmMzValue = maxPsmMzValue;
    }

    /**
     * Returns the maximal amount of PSMs for the peptides in the current
     * peptide list
     *
     * @return the maximal amount of PSMs for the peptides in the current
     * peptide list
     */
    public int getMaxSpectrumCount() {
        return maxSpectrumCount;
    }

    /**
     * Sets the maximal amount of PSMs for the peptides in the current peptide
     * list
     *
     * @param maxSpectrumCount the maximal amount of PSMs for the peptides in
     * the current peptide list
     */
    public void setMaxSpectrumCount(int maxSpectrumCount) {
        this.maxSpectrumCount = maxSpectrumCount;
    }

    /**
     * Returns the number of validated PSMs for the currently selected peptide
     *
     * @return the number of validated PSMs
     */
    public int getnValidatedPsms() {
        return nValidatedPsms;
    }

    /**
     * Sets the number of validated PSMs for the currently selected peptide
     *
     * @param nValidatedPsms the number of validated PSMs
     */
    public void setnValidatedPsms(int nValidatedPsms) {
        this.nValidatedPsms = nValidatedPsms;
    }

    /**
     * Returns the current peptide list
     *
     * @return the current peptide list
     */
    public ArrayList<String> getPeptideList() {
        return peptideList;
    }

    /**
     * Sets the current peptide list
     *
     * @param peptideList the current peptide list
     */
    public void setPeptideList(ArrayList<String> peptideList) {
        this.peptideList = peptideList;
    }

    /**
     * Returns the protein list
     *
     * @return the protein list
     */
    public ArrayList<String> getProteinList() {
        return proteinList;
    }

    /**
     * Sets the protein list
     *
     * @param proteinList the protein list
     */
    public void setProteinList(ArrayList<String> proteinList) {
        this.proteinList = proteinList;
    }

    /**
     * Returns the protein list after all hiding filters have been used
     *
     * @return the protein list after all hiding filters have been used
     */
    public ArrayList<String> getProteinListAfterHiding() {
        return proteinListAfterHiding;
    }

    /**
     * Sets the protein list after all hiding filters have been used
     *
     * @param proteinListAfterHiding the protein list after all hiding filters
     * have been used
     */
    public void setProteinListAfterHiding(ArrayList<String> proteinListAfterHiding) {
        this.proteinListAfterHiding = proteinListAfterHiding;
    }

    /**
     * Returns the PSM list
     *
     * @return the PSM list
     */
    public ArrayList<String> getPsmList() {
        return psmList;
    }

    /**
     * Sets the PSM list
     *
     * @param psmList the PSM list
     */
    public void setPsmList(ArrayList<String> psmList) {
        this.psmList = psmList;
    }

    /**
     * Returns a list of validated proteins
     *
     * @return a list of validated proteins
     */
    public ArrayList<String> getValidatedProteinList() {
        return validatedProteinList;
    }

    /**
     * Sets the list of validated proteins
     *
     * @param validatedProteinList a list of validated proteins
     */
    public void setValidatedProteinList(ArrayList<String> validatedProteinList) {
        this.validatedProteinList = validatedProteinList;
    }

    /**
     * Convenience method returning a string as key for the object of the given
     * type identified by the given key
     *
     * @param type the type of object
     * @param objectKey the key of the object
     * @return the key to be used in cache
     */
    private String getCacheKey(ObjectType type, String objectKey) {
        return getTypeAsString(type) + cacheSeparator + objectKey;
    }

    /**
     * Returns the object type as string
     *
     * @param type the type of object
     * @return the corresponding key
     */
    private String getTypeAsString(ObjectType type) {
        switch (type) {
            case coverable_AA:
                return "coverable_AA";
            case sequence_coverage:
                return "sequence_coverage";
            case expected_coverage:
                return "expected_coverage";
            case spectrum_counting:
                return "spectrum_counting";
            case number_of_spectra:
                return "#spectra";
            case number_of_validated_spectra:
                return "#validated_spectra";
            default:
                return "default";
        }
    }

    /**
     * Convenience method returning the type of object base on the objects cache
     * key
     *
     * @param cacheKey the object cache key
     * @return the type of object
     */
    private ObjectType getType(String cacheKey) {
        String objectTypeAsString = cacheKey.split(cacheSeparator)[0];
        if (objectTypeAsString.equals("coverable_AA")) {
            return ObjectType.coverable_AA;
        } else if (objectTypeAsString.equals("sequence_coverage")) {
            return ObjectType.sequence_coverage;
        } else if (objectTypeAsString.equals("expected_coverage")) {
            return ObjectType.expected_coverage;
        } else if (objectTypeAsString.equals("spectrum_counting")) {
            return ObjectType.sequence_coverage;
        } else if (objectTypeAsString.equals("#spectra")) {
            return ObjectType.number_of_spectra;
        } else if (objectTypeAsString.equals("#validated_spectra")) {
            return ObjectType.number_of_validated_spectra;
        } else {
            return null;
        }
    }

    /**
     * Convenience method returning the object key based on the cache key
     *
     * @param cacheKey the cache key
     * @return the object key
     */
    private String getObjectKey(String cacheKey) {
        String result = "";
        String[] splittedKey = cacheKey.split(cacheKey);
        for (int i = 1; i < splittedKey.length; i++) {
            result += splittedKey[i];
        }
        return result;
    }
}
