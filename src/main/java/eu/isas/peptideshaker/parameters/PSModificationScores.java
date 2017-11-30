package eu.isas.peptideshaker.parameters;

import com.compomics.util.db.object.DbObject;
import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.ModificationScoring;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * This class contains the scores for the locations of the possible
 * modifications.
 *
 * @author Marc Vaudel
 */
public class PSModificationScores extends DbObject implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 7450340838299319636L;
    /**
     * A map containing all scores indexed by the modification of interest for a
     * peptide or a PSM.
     */
    private final HashMap<String, ModificationScoring> modificationMap = new HashMap<>();
    /**
     * A list of all modification sites confidently localized on a sequence in a
     * map: site &gt; modification names.
     */
    private HashMap<Integer, HashSet<String>> mainModificationSites = null;
    /**
     * A map of all confident modifications in a sequence indexed by
     * modification name.
     */
    private HashMap<String, HashSet<Integer>> confidentModificationsByModName = null;
    /**
     * A list of all ambiguous modifications in a sequence: representative site
     * &gt; secondary site &gt; modification names.
     */
    private HashMap<Integer, HashMap<Integer, HashSet<String>>> ambiguousModificationsByRepresentativeSite = null;
    /**
     * A map of all ambiguous modifications in a sequence indexed by
     * modification name.
     */
    private HashMap<String, HashMap<Integer, HashSet<Integer>>> ambiguousModificationsByModName = null;

    /**
     * Constructor.
     */
    public PSModificationScores() {
    }

    /**
     * Adds a scoring result for the modification of interest.
     *
     * @param modName the modification of interest
     * @param modificationScoring the corresponding scoring
     */
    public void addModificationScoring(String modName, ModificationScoring modificationScoring) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        modificationMap.put(modName, modificationScoring);

    }

    /**
     * Returns the modification scoring for the desired modification (null if
     * none found).
     *
     * @param modName the modification of interest
     *
     * @return the scoring
     */
    public ModificationScoring getModificationScoring(String modName) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return modificationMap.get(modName);

    }

    /**
     * Indicates whether a modification has been already scored.
     *
     * @param modName the modification of interest
     *
     * @return a boolean indicating whether the modification is in the map
     */
    public boolean containsModification(String modName) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return modificationMap.containsKey(modName);

    }

    /**
     * Returns a list of scored modifications.
     *
     * @return a list of scored modifications
     */
    public ArrayList<String> getScoredModifications() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return new ArrayList<>(modificationMap.keySet());

    }

    /**
     * Adds a confident modification site.
     *
     * @param modName the modification name
     * @param modificationSite the modification site
     */
    public void addConfidentModificationSite(String modName, int modificationSite) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        // add the modification to the site map
        if (mainModificationSites == null) {

            mainModificationSites = new HashMap<>(1);

        }

        HashSet<String> modifications = mainModificationSites.get(modificationSite);

        if (modifications == null) {

            modifications = new HashSet<>(1);
            mainModificationSites.put(modificationSite, modifications);

        }

        modifications.add(modName);

        // add the site to the modification map
        if (confidentModificationsByModName == null) {

            confidentModificationsByModName = new HashMap<>(1);

        }

        HashSet<Integer> modificationSites = confidentModificationsByModName.get(modName);

        if (modificationSites == null) {

            modificationSites = new HashSet<>(1);
            confidentModificationsByModName.put(modName, modificationSites);

        }

        modificationSites.add(modificationSite);

    }

    /**
     * Removes a site from the ambiguous sites maps if found.
     *
     * @param modName the name of the modification of interest
     * @param modificationSite the site of interest
     */
    private void removeFromAmbiguousSitesMaps(String modName, int modificationSite) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (ambiguousModificationsByModName != null) {

            HashMap<Integer, HashSet<Integer>> modificationSites = ambiguousModificationsByModName.get(modName);

            if (modificationSites != null) {

                HashSet<Integer> representativeSites = new HashSet<>(modificationSites.keySet());

                for (int representativeSite : representativeSites) {

                    HashSet<Integer> secondarySites = modificationSites.get(representativeSite);

                    if (representativeSite == modificationSite || secondarySites.contains(modificationSite)) {

                        modificationSites.remove(representativeSite);
                        HashMap<Integer, HashSet<String>> secondarySitesAtAa = ambiguousModificationsByRepresentativeSite.get(representativeSite);
                        HashSet<Integer> secondarySiteList = new HashSet<>(secondarySitesAtAa.keySet());

                        for (int site : secondarySiteList) {

                            HashSet<String> modifications = secondarySitesAtAa.get(site);
                            modifications.remove(modName);

                            if (modifications.isEmpty()) {

                                secondarySitesAtAa.remove(site);

                            }
                        }

                        if (secondarySitesAtAa.isEmpty()) {

                            ambiguousModificationsByRepresentativeSite.remove(representativeSite);

                        }
                    }
                }

                if (modificationSites.isEmpty()) {

                    ambiguousModificationsByModName.remove(modName);

                }
            }
        }
    }

    /**
     * Adds a group of modifications to the mapping of ambiguous sites.
     *
     * @param representativeSite the representative site of this modification
     * group
     * @param possibleModifications the possible modifications in a map: site
     * &gt; modification name
     */
    public void addAmbiguousModificationSites(int representativeSite, HashMap<Integer, ArrayList<String>> possibleModifications) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (ambiguousModificationsByRepresentativeSite == null) {

            ambiguousModificationsByRepresentativeSite = new HashMap<>(possibleModifications.size());
            ambiguousModificationsByModName = new HashMap<>(possibleModifications.size());

        }

        HashMap<Integer, HashSet<String>> modificationGroupsAtSite = ambiguousModificationsByRepresentativeSite.get(representativeSite);

        if (modificationGroupsAtSite == null) {

            modificationGroupsAtSite = new HashMap<>(1);
            ambiguousModificationsByRepresentativeSite.put(representativeSite, modificationGroupsAtSite);

        }

        for (int site : possibleModifications.keySet()) {

            for (String modName : possibleModifications.get(site)) {

                HashSet<String> modifications = modificationGroupsAtSite.get(site);

                if (modifications == null) {

                    modifications = new HashSet<>(1);
                    modificationGroupsAtSite.put(site, modifications);

                }

                if (!modifications.contains(modName)) {

                    modifications.add(modName);

                }
            }
        }

        ArrayList<String> modifications = possibleModifications.get(representativeSite);

        for (String modification : modifications) {

            HashMap<Integer, HashSet<Integer>> modSites = ambiguousModificationsByModName.get(modification);

            if (modSites == null) {

                modSites = new HashMap<>();
                ambiguousModificationsByModName.put(modification, modSites);

            }

            HashSet<Integer> secondarySites = modSites.get(representativeSite);

            if (secondarySites == null) {

                secondarySites = new HashSet<>(1);
                modSites.put(representativeSite, secondarySites);

            }

            for (int site : possibleModifications.keySet()) {

                secondarySites.add(site);

            }
        }
    }

    /**
     * Changes the representative site for a given ambiguously localized
     * modification in all maps.
     *
     * @param modName the name of the modification of interest
     * @param originalRepresentativeSite the original representative site
     * @param newRepresentativeSite the new representative site
     */
    public void changeRepresentativeSite(String modName, int originalRepresentativeSite, int newRepresentativeSite) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        HashMap<Integer, HashSet<String>> ambiguousSites = ambiguousModificationsByRepresentativeSite.get(originalRepresentativeSite);

        if (ambiguousSites != null) {

            HashMap<Integer, HashSet<String>> newSites = new HashMap<>(1);
            HashSet<Integer> sites = new HashSet<>(ambiguousSites.keySet());

            for (Integer site : sites) {

                HashSet<String> modifications = ambiguousSites.get(site);

                if (modifications.contains(modName)) {

                    HashSet<String> newModifications = new HashSet<>(1);
                    newModifications.add(modName);
                    newSites.put(site, newModifications);
                    modifications.remove(modName);

                    if (modifications.isEmpty()) {

                        ambiguousSites.remove(site);

                    }
                }
            }

            if (ambiguousSites.isEmpty()) {

                ambiguousModificationsByRepresentativeSite.remove(originalRepresentativeSite);

            }

            ambiguousSites = ambiguousModificationsByRepresentativeSite.get(newRepresentativeSite);

            if (ambiguousSites == null) {

                ambiguousModificationsByRepresentativeSite.put(newRepresentativeSite, newSites);

            } else {

                for (int site : newSites.keySet()) {

                    HashSet<String> modifications = ambiguousSites.get(site);

                    if (modifications == null) {

                        modifications = new HashSet<>(2);
                        ambiguousSites.put(site, modifications);

                    }

                    if (!modifications.contains(modName)) {

                        modifications.add(modName);

                    }
                }
            }
        }

        for (String originalModificationName : ambiguousModificationsByModName.keySet()) {

            HashMap<Integer, HashSet<Integer>> modificationSiteMap = ambiguousModificationsByModName.get(originalModificationName);
            HashSet<Integer> secondarySites = modificationSiteMap.get(originalRepresentativeSite);

            if (secondarySites != null) {

                modificationSiteMap.remove(originalRepresentativeSite);
                modificationSiteMap.put(newRepresentativeSite, secondarySites);

            }
        }
    }

    /**
     * Indicates whether a site is already registered as confident modification
     * site.
     *
     * @param site the site of interest
     * @param modificationName the name of the modification
     *
     * @return a boolean indicating whether a site is already registered as
     * confident modification site
     */
    public boolean isConfidentModificationSite(int site, String modificationName) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return mainModificationSites != null
                && mainModificationSites.containsKey(site)
                && mainModificationSites.get(site).contains(modificationName);

    }

    /**
     * Returns the main potential modifications at the given amino acid index.
     *
     * @param site the index in the sequence (0 is first amino acid) //@TODO:
     * check that it is 0 and not 1
     * @return a list containing all potential modifications as main match, an
     * empty list if none found
     */
    public HashSet<String> getConfidentModificationsAt(int site) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return mainModificationSites == null || !mainModificationSites.containsKey(site) ? new HashSet<>(0)
                : mainModificationSites.get(site);

    }

    /**
     * Returns the modifications which have a representative ambiguous site at
     * the given site.
     *
     * @param site the index in the sequence (0 is first amino acid)
     *
     * @return a list of modifications which have a representative ambiguous
     * site at the given site
     */
    public HashSet<String> getModificationsAtRepresentativeSite(int site) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        HashMap<Integer, HashSet<String>> modificationsAtSite = getAmbiguousModificationsAtRepresentativeSite(site);

        return modificationsAtSite == null ? new HashSet<>(0)
                : modificationsAtSite.get(site);

    }

    /**
     * Returns the confident sites for the given modification. An empty list if
     * none found.
     *
     * @param modName the name of the modification of interest
     *
     * @return the confident sites for the given modification
     */
    public HashSet<Integer> getConfidentSitesForModification(String modName) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return confidentModificationsByModName == null || !confidentModificationsByModName.containsKey(modName) ? new HashSet<>(0)
                : confidentModificationsByModName.get(modName);

    }

    /**
     * Returns the ambiguous modification assignments registered at the given
     * representative site in a map: secondary site &gt; modifications.
     *
     * @param representativeSite the representative site of interest
     *
     * @return the ambiguous modification assignments registered at the given
     * representative site
     */
    public HashMap<Integer, HashSet<String>> getAmbiguousModificationsAtRepresentativeSite(int representativeSite) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return ambiguousModificationsByRepresentativeSite == null || ambiguousModificationsByRepresentativeSite.containsKey(representativeSite) ? new HashMap<>(0)
                : ambiguousModificationsByRepresentativeSite.get(representativeSite);

    }

    /**
     * Returns the ambiguous modification sites registered for the given modification.
     *
     * @param modName the name of the modification of interest
     *
     * @return the ambiguous modification sites registered for the given modification
     */
    public HashMap<Integer, HashSet<Integer>> getAmbiguousModificationsSites(String modName) {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return ambiguousModificationsByModName == null ? new HashMap<>(0)
                : ambiguousModificationsByModName.get(modName);
        
    }

    /**
     * Returns a list of all confident modification sites.
     *
     * @return a list of all confident modification sites
     */
    public TreeSet<Integer> getConfidentSites() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return mainModificationSites == null ? new TreeSet<>() :
                new TreeSet<>(mainModificationSites.keySet());
        
    }

    /**
     * Returns a list of all representative sites of ambiguously localized modifications.
     *
     * @return a list of all representative sites of ambiguously localized modifications
     */
    public TreeSet<Integer> getRepresentativeSites() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return ambiguousModificationsByRepresentativeSite == null ? new TreeSet<>()
                : new TreeSet<>(ambiguousModificationsByRepresentativeSite.keySet());
        
    }

    /**
     * Returns a list of modifications presenting at least a confident site.
     *
     * @return a list of modifications presenting at least a confident site
     */
    public TreeSet<String> getConfidentlyLocalizedModifications() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return confidentModificationsByModName == null ? new TreeSet<>()
                : new TreeSet<>(confidentModificationsByModName.keySet());
        
    }

    /**
     * Returns a list of modifications presenting at least an ambiguous site.
     *
     * @return a list of modifications presenting at least an ambiguous site
     */
    public TreeSet<String> getAmbiguouslyLocalizedModifications() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return ambiguousModificationsByModName == null ? new TreeSet<>()
                : new TreeSet<>(ambiguousModificationsByModName.keySet());
        
    }

    @Override
    public long getParameterKey() {
        return getId();
    }
}
