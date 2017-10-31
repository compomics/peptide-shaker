package eu.isas.peptideshaker.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

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
         * The non-tryptic peptides of a protein.
         */
        tryptic_protein(true),
        /**
         * The likelihood to cover amino acids.
         */
        coverable_AA_p(true),
        /**
         * The amino acid coverage of a given protein.
         */
        AA_coverage(true),
        /**
         * The number of validated peptides of a given protein.
         */
        number_of_validated_peptides(false),
        /**
         * The number of confident peptides of a given protein.
         */
        number_of_confident_peptides(false),
        /**
         * The number of spectra of a given protein.
         */
        number_of_spectra(false),
        /**
         * The number of validated spectra.
         */
        number_of_validated_spectra(false),
        /**
         * The number of confident spectra.
         */
        number_of_confident_spectra(false),
        /**
         * The number of unique peptides.
         */
        unique_peptides(false),
        /**
         * The number of unique validated peptides.
         */
        unique_validated_peptides(false),
        /**
         * The number of validated protein groups for a peptide.
         */
        protein_groups_for_peptide(false),
        /**
         * If a given protein accession contains enzymatic peptides: true or
         * false.
         */
        containsEnzymaticPeptides(false),
        /**
         * The max mz value for all the PSMs for a given peptide.
         */
        max_psm_mz_for_peptides(false),
        /**
         * The sequence coverage of a given protein using validated peptides.
         */
        sequence_coverage(false),
        /**
         * The sequence coverage of a given protein.
         */
        sequence_validation_coverage(false),
        /**
         * The expected sequence coverage of a given protein.
         */
        expected_coverage(false),
        /**
         * The spectrum counting index of a given protein.
         */
        spectrum_counting(false);
        /**
         * Boolean indicating whether this category contains large objects.
         */
        public final boolean large;

        /**
         * Constructor.
         *
         * @param large boolean indicating whether this category contains large
         * objects
         */
        private ObjectType(boolean large) {
            this.large = large;
        }
    }
    /**
     * The number of values kept in memory for small objects.
     */
    private static final int smallObjectsCacheSize = 1000000;
    /**
     * The number of values kept in memory for large objects.
     */
    private static final int largeObjectsCacheSize = 1000;
    /**
     * The number of small objects in cache.
     */
    private int smallObjectsInCache = 0;
    /**
     * The number of large objects in cache.
     */
    private int largeObjectsInCache = 0;
    /**
     * Cache for the large objects.
     */
    private final HashMap<ObjectType, HashMap<Long, Object>> largeObjectsCache = new HashMap<>();
    /**
     * Cache for the small objects.
     */
    private final HashMap<ObjectType, HashMap<Long, Object>> smallObjectsCache = new HashMap<>();
    /**
     * The protein list.
     */
    private long[] proteinListAfterHiding = null;
    /**
     * Back-up list for when proteins are hidden.
     */
    private long[] proteinList = null;
    /**
     * List of the validated proteins.
     */
    private long[] validatedProteinList = null;
    /**
     * The peptide list.
     */
    private long[] peptideList;
    /**
     * The PSM list.
     */
    private long[] psmList;
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
    private long currentProteinKey;
    /**
     * The current peptide key.
     */
    private long currentPeptideKey;
    /**
     * Indicates whether the cache is read only.
     */
    private boolean readOnly = false;
    /**
     * Semaphore for the different object types.
     */
    private final HashMap<ObjectType, Semaphore> mutexMap = new HashMap<ObjectType, Semaphore>(ObjectType.values().length);

    /**
     * Constructor.
     */
    public IdentificationFeaturesCache() {

        for (ObjectType type : ObjectType.values()) {

            if (!type.large) {

                smallObjectsCache.put(type, new HashMap<>());

            } else {

                largeObjectsCache.put(type, new HashMap<>());

            }

            mutexMap.put(type, new Semaphore(1));

        }
    }

    /**
     * Clears all objects of the given type.
     *
     * @param type the object type
     */
    public void removeObjects(ObjectType type) {

        if (!type.large) {

            HashMap<Long, Object> subMap = smallObjectsCache.get(type);
            smallObjectsCache.put(type, new HashMap<>());
            smallObjectsInCache -= subMap.size();

        } else {

            HashMap<Long, Object> subMap = largeObjectsCache.get(type);
            largeObjectsCache.put(type, new HashMap<>());
            largeObjectsInCache -= subMap.size();

        }
    }

    /**
     * Adds an object in the cache. If a thread gets interrupted, the
     * corresponding exception is thrown as runtime exception.
     *
     * @param type the type of the object
     * @param objectKey the object key
     * @param object the object to store
     */
    public void addObject(ObjectType type, long objectKey, Object object) {

        if (!readOnly) {

            try {

                if (!type.large) {

                    Semaphore mutex = mutexMap.get(type);
                    mutex.acquire();

                    HashMap<Long, Object> subMap = smallObjectsCache.get(type);

                    if (subMap.containsKey(objectKey)) {

                        mutex.release();

                    } else {

                        smallObjectsCache.get(type).put(objectKey, object);
                        smallObjectsInCache++;

                        mutex.release();

                        int i = 0;
                        ObjectType[] types = ObjectType.values();

                        while (smallObjectsInCache > smallObjectsCacheSize && i++ < types.length) {

                            ObjectType cacheType = types[i];

                            if (!cacheType.large && cacheType != type) {

                                mutex = mutexMap.get(cacheType);
                                mutex.acquire();

                                removeObjects(type);

                                mutex.release();

                            }

                        }
                    }

                } else {

                    Semaphore mutex = mutexMap.get(type);
                    mutex.acquire();

                    HashMap<Long, Object> subMap = largeObjectsCache.get(type);

                    if (subMap.containsKey(objectKey)) {

                        mutex.release();

                    } else {

                        largeObjectsCache.get(type).put(objectKey, object);
                        largeObjectsInCache++;

                        mutex.release();

                        int i = 0;
                        ObjectType[] types = ObjectType.values();

                        while (largeObjectsInCache > largeObjectsCacheSize && i++ < types.length) {

                            ObjectType cacheType = types[i];

                            if (cacheType.large && cacheType != type) {

                                mutex = mutexMap.get(cacheType);
                                mutex.acquire();

                                removeObjects(type);

                                mutex.release();

                            }
                        }
                    }
                }

            } catch (InterruptedException e) {

                throw new RuntimeException();

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
    public Object getObject(ObjectType type, long objectKey) {
        
        return type.large ? largeObjectsCache.get(type).get(objectKey)
                : smallObjectsCache.get(type).get(objectKey);
        
    }

    /**
     * Returns the current peptide key.
     *
     * @return the current peptide key
     */
    public long getCurrentPeptideKey() {
        return currentPeptideKey;
    }

    /**
     * Sets the current peptide key.
     *
     * @param currentPeptideKey the current peptide key
     */
    public void setCurrentPeptideKey(long currentPeptideKey) {
        this.currentPeptideKey = currentPeptideKey;
    }

    /**
     * Returns the current protein key.
     *
     * @return the current protein key
     */
    public long getCurrentProteinKey() {
        return currentProteinKey;
    }

    /**
     * Sets the current protein key.
     *
     * @param currentProteinKey the current protein key
     */
    public void setCurrentProteinKey(long currentProteinKey) {
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
    public long[] getPeptideList() {
        return peptideList;
    }

    /**
     * Sets the current peptide list.
     *
     * @param peptideList the current peptide list
     */
    public void setPeptideList(long[] peptideList) {
        this.peptideList = peptideList;
    }

    /**
     * Returns the protein list.
     *
     * @return the protein list
     */
    public long[] getProteinList() {
        return proteinList;
    }

    /**
     * Sets the protein list.
     *
     * @param proteinList the protein list
     */
    public void setProteinList(long[] proteinList) {
        this.proteinList = proteinList;
    }

    /**
     * Returns the protein list after all hiding filters have been used.
     *
     * @return the protein list after all hiding filters have been used
     */
    public long[] getProteinListAfterHiding() {
        return proteinListAfterHiding;
    }

    /**
     * Sets the protein list after all hiding filters have been used.
     *
     * @param proteinListAfterHiding the protein list after all hiding filters
     * have been used
     */
    public void setProteinListAfterHiding(long[] proteinListAfterHiding) {
        this.proteinListAfterHiding = proteinListAfterHiding;
    }

    /**
     * Returns the PSM list.
     *
     * @return the PSM list
     */
    public long[] getPsmList() {
        return psmList;
    }

    /**
     * Sets the PSM list.
     *
     * @param psmList the PSM list
     */
    public void setPsmList(long[] psmList) {
        this.psmList = psmList;
    }

    /**
     * Returns a list of validated proteins.
     *
     * @return a list of validated proteins
     */
    public long[] getValidatedProteinList() {
        return validatedProteinList;
    }

    /**
     * Sets the list of validated proteins.
     *
     * @param validatedProteinList a list of validated proteins
     */
    public void setValidatedProteinList(long[] validatedProteinList) {
        this.validatedProteinList = validatedProteinList;
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
