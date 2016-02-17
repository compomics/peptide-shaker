package eu.isas.peptideshaker.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class caches the identification features calculated by the
 * IdentificationFeaturesGenerator for later reuse.
 *
 * @author Marc Vaudel
 */
public class IdentificationFeaturesCache implements Serializable {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = -7291018247377919040L;

    /**
     * An enumerator of the supported object types.
     */
    public enum ObjectType {

        /**
         * The likelihood to cover amino acids stored as big object.
         */
        coverable_AA_p,
        /**
         * The amino acid coverage of a given protein.
         */
        AA_coverage,
        /**
         * The sequence coverage of a given protein using validated peptides
         * stored as small object.
         */
        sequence_coverage,
        /**
         * The sequence coverage of a given protein stored as small object.
         */
        sequence_validation_coverage,
        /**
         * The expected sequence coverage of a given protein stored as small
         * object.
         */
        expected_coverage,
        /**
         * The spectrum counting index of a given protein stored as small
         * object.
         */
        spectrum_counting,
        /**
         * The number of spectra of a given protein stored as small object.
         */
        number_of_spectra,
        /**
         * The number of validated spectra of a given peptide or protein stored
         * as small object.
         */
        number_of_validated_spectra,
        /**
         * The number of validated spectra of a given peptide or protein stored
         * as small object.
         */
        number_of_confident_spectra,
        /**
         * The number of validated peptides of a given protein stored as small
         * object.
         */
        number_of_validated_peptides,
        /**
         * The number of confident peptides of a given protein stored as small
         * object.
         */
        number_of_confident_peptides,
        /**
         * The max mz value for all the PSMs for a given peptide stored as small
         * object.
         */
        max_psm_mz_for_peptides,
        /**
         * The non-tryptic peptides. Stored as a big object.
         */
        tryptic_protein,
        /**
         * The number of unique peptides. Stored as a small object.
         */
        unique_peptides,
        /**
         * The number of validated protein groups for a peptide. Stored as a small object.
         */
        protein_groups_for_peptide,
        /**
         * The number of unique validated peptides. Stored as a small object.
         */
        unique_validated_peptides,
        /**
         * The number of unique peptides. Stored as a small object.
         */
        unique_peptides_group,
        /**
         * The number of unique validated peptides. Stored as a small object.
         */
        unique_validated_peptides_group,
        /**
         * Contains if a given protein accession contains enzymatic peptides:
         * true or false. Stored as a small object.
         */
        containsEnzymaticPeptides;
    }
    /**
     * The number of values kept in memory for small objects.
     */
    private final int smallObjectsCacheSize = 1000000;
    /**
     * The number of values kept in memory for big objects.
     */
    private final int bigObjectsCacheSize = 1000;
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
     * Mapping of the stored big objects.
     */
    private HashMap<ObjectType, HashMap<String, Object>> bigObjectsCache = new HashMap<ObjectType, HashMap<String, Object>>();
    /**
     * Mapping of the stored small objects.
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
     * The PSM list.
     */
    private ArrayList<String> psmList;
    /**
     * Boolean indicating whether a filtering was already used. If yes, proteins
     * might need to be unhidden.
     */
    private boolean filtered = false;
    /**
     * The maximum number of PSMs across all peptides of the last selected
     * protein.
     */
    private int maxSpectrumCount;
    /**
     * The number of validated PSMs in the currently selected peptide.
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
     * Indicates whether the cache is read only.
     */
    private boolean readOnly = false;

    /**
     * Clears all objects of the given type.
     *
     * @param type the object type
     */
    public synchronized void removeObjects(ObjectType type) {
        if (!readOnly) {
            String typeKey = getTypeAsString(type);
            ArrayList<String> toRemove = new ArrayList<String>();

            switch (type) {
                case coverable_AA_p:
                case AA_coverage:
                case tryptic_protein:
                    bigObjectsCache.remove(type);
                    for (String key : bigObjectsInCache) {
                        if (key.contains(typeKey)) {
                            toRemove.add(key);
                        }
                    }
                    for (String key : toRemove) {
                        bigObjectsInCache.remove(key);
                    }
                    break;
                case sequence_coverage:
                case sequence_validation_coverage:
                case expected_coverage:
                case spectrum_counting:
                case number_of_spectra:
                case number_of_validated_spectra:
                case number_of_validated_peptides:
                case number_of_confident_spectra:
                case number_of_confident_peptides:
                case max_psm_mz_for_peptides:
                case unique_peptides:
                case containsEnzymaticPeptides:
                    smallObjectsCache.remove(type);
                    for (String key : smallObjectsInCache) {
                        if (key.contains(typeKey)) {
                            toRemove.add(key);
                        }
                    }
                    for (String key : toRemove) {
                        smallObjectsInCache.remove(key);
                    }
                    break;
            }
        }
    }

    /**
     * Adds an object in the cache.
     *
     * @param type the type of the object
     * @param objectKey the object key
     * @param object the object to store
     */
    public synchronized void addObject(ObjectType type, String objectKey, Object object) {
        if (!readOnly) {
            switch (type) {
                case coverable_AA_p:
                case AA_coverage:
                case tryptic_protein:
                    if (!bigObjectsCache.containsKey(type)) {
                        bigObjectsCache.put(type, new HashMap<String, Object>());
                    }

                    Object oldValue = bigObjectsCache.get(type).put(objectKey, object);

                    if (oldValue == null) { // don't add if the object was already in the cache
                        bigObjectsInCache.add(getCacheKey(type, objectKey));
                    }

                    while (bigObjectsInCache.size() >= bigObjectsCacheSize && !bigObjectsInCache.isEmpty()) {
                        String firstObjectKey = bigObjectsInCache.get(0);
                        ObjectType oldType = getType(firstObjectKey);
                        String oldKey = getObjectKey(firstObjectKey);
                        HashMap<String, Object> cacheForType = bigObjectsCache.get(oldType);
                        if (cacheForType != null) {
                            cacheForType.remove(oldKey);
                            if (cacheForType.isEmpty()) {
                                bigObjectsCache.remove(oldType);
                            }
                        }
                        bigObjectsInCache.remove(0);
                    }
                    break;
                case sequence_coverage:
                case sequence_validation_coverage:
                case expected_coverage:
                case spectrum_counting:
                case number_of_spectra:
                case number_of_validated_spectra:
                case number_of_validated_peptides:
                case number_of_confident_spectra:
                case number_of_confident_peptides:
                case max_psm_mz_for_peptides:
                case unique_peptides:
                case containsEnzymaticPeptides:
                    if (!smallObjectsCache.containsKey(type)) {
                        smallObjectsCache.put(type, new HashMap<String, Object>());
                    }

                    oldValue = smallObjectsCache.get(type).put(objectKey, object);

                    if (oldValue == null) {
                        smallObjectsInCache.add(getCacheKey(type, objectKey));
                    }

                    while (smallObjectsInCache.size() >= smallObjectsCacheSize && !smallObjectsInCache.isEmpty()) {
                        String firstObjectKey = smallObjectsInCache.get(0);
                        ObjectType oldType = getType(firstObjectKey);
                        String oldKey = getObjectKey(firstObjectKey);
                        HashMap<String, Object> cacheForType = smallObjectsCache.get(oldType);
                        if (cacheForType != null) {
                            cacheForType.remove(oldKey);
                            if (cacheForType.isEmpty()) {
                                smallObjectsCache.remove(oldType);
                            }
                        }
                        smallObjectsInCache.remove(0);
                    }
                    break;
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
        switch (type) {
            case coverable_AA_p:
            case AA_coverage:
            case tryptic_protein:
                HashMap<String, Object> cacheForType = bigObjectsCache.get(type);
                if (cacheForType != null) {
                    return cacheForType.get(objectKey);
                }
                return null;
            case sequence_coverage:
            case sequence_validation_coverage:
            case expected_coverage:
            case spectrum_counting:
            case number_of_spectra:
            case number_of_validated_spectra:
            case number_of_validated_peptides:
            case number_of_confident_spectra:
            case number_of_confident_peptides:
            case max_psm_mz_for_peptides:
            case unique_peptides:
            case containsEnzymaticPeptides:
                cacheForType = smallObjectsCache.get(type);
                if (cacheForType != null) {
                    return cacheForType.get(objectKey);
                }
                return null;
            default:
                return null;
        }
    }

    /**
     * Returns the current peptide key.
     *
     * @return the current peptide key
     */
    public String getCurrentPeptideKey() {
        return currentPeptideKey;
    }

    /**
     * Sets the current peptide key.
     *
     * @param currentPeptideKey the current peptide key
     */
    public void setCurrentPeptideKey(String currentPeptideKey) {
        this.currentPeptideKey = currentPeptideKey;
    }

    /**
     * Returns the current protein key.
     *
     * @return the current protein key
     */
    public String getCurrentProteinKey() {
        return currentProteinKey;
    }

    /**
     * Sets the current protein key.
     *
     * @param currentProteinKey the current protein key
     */
    public void setCurrentProteinKey(String currentProteinKey) {
        this.currentProteinKey = currentProteinKey;
    }

    /**
     * Indicates whether the protein list is filtered.
     *
     * @return a boolean indicating whether the protein list is filtered
     */
    public boolean isFiltered() {
        return filtered;
    }

    /**
     * Sets whether the protein list is filtered.
     *
     * @param filtered a boolean indicating whether the protein list is filtered
     */
    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    /**
     * Returns the maximal amount of PSMs for the peptides in the current
     * peptide list.
     *
     * @return the maximal amount of PSMs for the peptides in the current
     * peptide list
     */
    public int getMaxSpectrumCount() {
        return maxSpectrumCount;
    }

    /**
     * Sets the maximal amount of PSMs for the peptides in the current peptide
     * list.
     *
     * @param maxSpectrumCount the maximal amount of PSMs for the peptides in
     * the current peptide list
     */
    public void setMaxSpectrumCount(int maxSpectrumCount) {
        this.maxSpectrumCount = maxSpectrumCount;
    }

    /**
     * Returns the number of validated PSMs for the currently selected peptide.
     *
     * @return the number of validated PSMs
     */
    public int getnValidatedPsms() {
        return nValidatedPsms;
    }

    /**
     * Sets the number of validated PSMs for the currently selected peptide.
     *
     * @param nValidatedPsms the number of validated PSMs
     */
    public void setnValidatedPsms(int nValidatedPsms) {
        this.nValidatedPsms = nValidatedPsms;
    }

    /**
     * Returns the current peptide list.
     *
     * @return the current peptide list
     */
    public ArrayList<String> getPeptideList() {
        return peptideList;
    }

    /**
     * Sets the current peptide list.
     *
     * @param peptideList the current peptide list
     */
    public void setPeptideList(ArrayList<String> peptideList) {
        this.peptideList = peptideList;
    }

    /**
     * Returns the protein list.
     *
     * @return the protein list
     */
    public ArrayList<String> getProteinList() {
        return proteinList;
    }

    /**
     * Sets the protein list.
     *
     * @param proteinList the protein list
     */
    public void setProteinList(ArrayList<String> proteinList) {
        this.proteinList = proteinList;
    }

    /**
     * Returns the protein list after all hiding filters have been used.
     *
     * @return the protein list after all hiding filters have been used
     */
    public ArrayList<String> getProteinListAfterHiding() {
        return proteinListAfterHiding;
    }

    /**
     * Sets the protein list after all hiding filters have been used.
     *
     * @param proteinListAfterHiding the protein list after all hiding filters
     * have been used
     */
    public void setProteinListAfterHiding(ArrayList<String> proteinListAfterHiding) {
        this.proteinListAfterHiding = proteinListAfterHiding;
    }

    /**
     * Returns the PSM list.
     *
     * @return the PSM list
     */
    public ArrayList<String> getPsmList() {
        return psmList;
    }

    /**
     * Sets the PSM list.
     *
     * @param psmList the PSM list
     */
    public void setPsmList(ArrayList<String> psmList) {
        this.psmList = psmList;
    }

    /**
     * Returns a list of validated proteins.
     *
     * @return a list of validated proteins
     */
    public ArrayList<String> getValidatedProteinList() {
        return validatedProteinList;
    }

    /**
     * Sets the list of validated proteins.
     *
     * @param validatedProteinList a list of validated proteins
     */
    public void setValidatedProteinList(ArrayList<String> validatedProteinList) {
        this.validatedProteinList = validatedProteinList;
    }

    /**
     * Convenience method returning a string as key for the object of the given
     * type identified by the given key.
     *
     * @param type the type of object
     * @param objectKey the key of the object
     * @return the key to be used in cache
     */
    private String getCacheKey(ObjectType type, String objectKey) {
        return getTypeAsString(type) + cacheSeparator + objectKey;
    }

    /**
     * Returns the object type as string.
     *
     * @param type the type of object
     * @return the corresponding key
     */
    private String getTypeAsString(ObjectType type) {
        switch (type) {
            case coverable_AA_p:
                return "coverable_AA_p";
            case AA_coverage:
                return "AA_coverage";
            case sequence_coverage:
                return "sequence_coverage";
            case sequence_validation_coverage:
                return "sequence_validation_coverage";
            case expected_coverage:
                return "expected_coverage";
            case spectrum_counting:
                return "spectrum_counting";
            case number_of_spectra:
                return "#spectra";
            case number_of_validated_spectra:
                return "#validated_spectra";
            case number_of_validated_peptides:
                return "#validated_peptides";
            case number_of_confident_spectra:
                return "#confident_spectra";
            case number_of_confident_peptides:
                return "#confident_peptides";
            case max_psm_mz_for_peptides:
                return "max_psm_mz_for_peptides";
            case tryptic_protein:
                return "tryptic_protein";
            case unique_peptides:
                return "unique_peptides";
            case containsEnzymaticPeptides:
                return "contains_enzymatic_peptides";
            default:
                return "default";
        }
    }

    /**
     * Convenience method returning the type of object base on the objects cache
     * key.
     *
     * @param cacheKey the object cache key
     * @return the type of object
     */
    private ObjectType getType(String cacheKey) {
        String objectTypeAsString = cacheKey.split(cacheSeparator)[0];
        if (objectTypeAsString.equals("coverable_AA_p")) {
            return ObjectType.coverable_AA_p;
        } else if (objectTypeAsString.equals("AA_coverage")) {
            return ObjectType.AA_coverage;
        } else if (objectTypeAsString.equals("sequence_coverage")) {
            return ObjectType.sequence_coverage;
        } else if (objectTypeAsString.equals("sequence_validation_coverage")) {
            return ObjectType.sequence_validation_coverage;
        } else if (objectTypeAsString.equals("expected_coverage")) {
            return ObjectType.expected_coverage;
        } else if (objectTypeAsString.equals("spectrum_counting")) {
            return ObjectType.spectrum_counting;
        } else if (objectTypeAsString.equals("#spectra")) {
            return ObjectType.number_of_spectra;
        } else if (objectTypeAsString.equals("#validated_spectra")) {
            return ObjectType.number_of_validated_spectra;
        } else if (objectTypeAsString.equals("#validated_peptides")) {
            return ObjectType.number_of_validated_peptides;
        } else if (objectTypeAsString.equals("#confident_spectra")) {
            return ObjectType.number_of_confident_spectra;
        } else if (objectTypeAsString.equals("#confident_peptides")) {
            return ObjectType.number_of_confident_peptides;
        } else if (objectTypeAsString.equals("max_psm_mz_for_peptides")) {
            return ObjectType.max_psm_mz_for_peptides;
        } else if (objectTypeAsString.equals("tryptic_protein")) {
            return ObjectType.tryptic_protein;
        } else if (objectTypeAsString.equals("unique_peptides")) {
            return ObjectType.unique_peptides;
        } else if (objectTypeAsString.equals("contains_enzymatic_peptides")) { 
            return ObjectType.containsEnzymaticPeptides;
        } else {
            return null;
        }
    }

    /**
     * Convenience method returning the object key based on the cache key.
     *
     * @param cacheKey the cache key
     * @return the object key
     */
    private String getObjectKey(String cacheKey) {
        StringBuilder buf = new StringBuilder();
        String escapedString = java.util.regex.Pattern.quote(cacheKey);
        String[] splittedKey = cacheKey.split(escapedString);
        for (int i = 1; i < splittedKey.length; i++) {
            buf.append(splittedKey[i]);
        }
        return buf.toString();
    }

    /**
     * Sets the cache in read only.
     *
     * @param readOnly boolean indicating whether the cache should be in read
     * only
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
