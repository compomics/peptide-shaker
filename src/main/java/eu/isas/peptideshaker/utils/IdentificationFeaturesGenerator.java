package eu.isas.peptideshaker.utils;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class provides identification features at the protein level
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class IdentificationFeaturesGenerator {

    /**
     * Instance of the main GUI class
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The number of values kept in memory for small objects
     */
    private final int smallObjectsCacheSize = 1000;
    /**
     * The number of values kept in memory for big objects
     */
    private final int bigObjectsCacheSize = 3;
    /**
     * The cached protein matches for small objects
     */
    private ArrayList<String> smallObjectsCache = new ArrayList<String>();
    /**
     * The cached protein matches for big objects
     */
    private ArrayList<String> bigObjectsCache = new ArrayList<String>();
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The sequence coverage of the main match of the loaded protein match
     */
    private HashMap<String, Double> sequenceCoverage = new HashMap<String, Double>();
    /**
     * The possible sequence coverage of the main match of the loaded protein match
     */
    private HashMap<String, Double> possibleCoverage = new HashMap<String, Double>();
    /**
     * The spectrum counting metric of the loaded protein match
     */
    private HashMap<String, Double> spectrumCounting = new HashMap<String, Double>();
    /**
     * The number of spectra
     */
    private HashMap<String, Integer> nSpectra = new HashMap<String, Integer>();
    /**
     * The compomics PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * a map containing the list of coverable amino acids for each protein in the big object cache
     */
    private HashMap<String, boolean[]> coverableAA = new HashMap<String, boolean[]>();

    /**
     * Constructor
     * @param peptideShakerGUI instance of the main GUI class
     */
    public IdentificationFeaturesGenerator(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
    }

    /**
     * Returns an array of boolean indicating whether the amino acids of given peptides can generate peptides.
     * 
     * @param proteinMatchKey the key of the protein of interest
     * @return an array of boolean indicating whether the amino acids of given peptides can generate peptides
     */
    public boolean[] getCoverableAA(String proteinMatchKey) {
        boolean[] result = coverableAA.get(proteinMatchKey);
        if (result == null) {
            if (bigObjectsCache.size() >= bigObjectsCacheSize) {
                int nRemove = bigObjectsCache.size() - bigObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();
                for (String tempKey : bigObjectsCache) {
                    if (coverableAA.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }
                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }
            result = estimateCoverableAA(proteinMatchKey);
            coverableAA.put(proteinMatchKey, result);
            bigObjectsCache.remove(proteinMatchKey);
            bigObjectsCache.add(proteinMatchKey);
        }
        return result;
    }

    /**
     * Returns an array of boolean indicating whether the amino acids of given peptides can generate peptides.
     * 
     * @param proteinMatchKey the key of the protein of interest
     * @return an array of boolean indicating whether the amino acids of given peptides can generate peptides
     */
    private boolean[] estimateCoverableAA(String proteinMatchKey) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            boolean[] result = new boolean[sequence.length()];
            int pepMax = peptideShakerGUI.getIdFilter().getMaxPepLength();
            Enzyme enzyme = peptideShakerGUI.getSearchParameters().getEnzyme();
            int cleavageAA = 0;
            int lastCleavage = 0;

            while (++cleavageAA < sequence.length() - 2) {
                if (enzyme.getAminoAcidAfter().contains(sequence.charAt(cleavageAA + 1)) && !enzyme.getRestrictionBefore().contains(sequence.charAt(cleavageAA))
                        || enzyme.getAminoAcidBefore().contains(sequence.charAt(cleavageAA)) && !enzyme.getRestrictionAfter().contains(sequence.charAt(cleavageAA + 1))) {
                    if (cleavageAA - lastCleavage <= pepMax) {
                        for (int i = lastCleavage; i <= cleavageAA; i++) {
                            result[i] = true;
                        }
                    }
                    lastCleavage = cleavageAA;
                }
            }

            result[sequence.length() - 1] = result[sequence.length() - 2];

            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return new boolean[0];
        }
    }

    /**
     * Returns the sequence coverage of the protein of interest
     * @param proteinMatchKey the key of the protein of interest
     * @return the sequence coverage
     */
    public Double getSequenceCoverage(String proteinMatchKey) {

        Double result = sequenceCoverage.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (sequenceCoverage.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }

            result = estimateSequenceCoverage(proteinMatchKey);
            sequenceCoverage.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }
        return result;
    }

    /**
     * Removes a key from the cache.
     * 
     * @param key the key to remove
     */
    public void removeFromSmallCache(String key) {
        smallObjectsCache.remove(key);
        sequenceCoverage.remove(key);
        possibleCoverage.remove(key);
        spectrumCounting.remove(key);
    }

    /**
     * Removes a key from the cache.
     * 
     * @param key the key to remove
     */
    public void removeFromBigCache(String key) {
        bigObjectsCache.remove(key);
        coverableAA.remove(key);
    }

    /**
     * Estimates the sequence coverage for the given protein match.
     * 
     * @param proteinMatchKey   the key of the protein match
     * @return the sequence coverage
     */
    private double estimateSequenceCoverage(String proteinMatchKey) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            // an array containing the coverage index for each residue
            int[] coverage = new int[sequence.length() + 1];
            int peptideTempStart, peptideTempEnd;
            String tempSequence, peptideSequence;
            PSParameter pSParameter = new PSParameter();

            // iterate the peptide table and store the coverage for each peptide
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
                if (pSParameter.isValidated()) {
                    tempSequence = sequence;
                    peptideSequence = Peptide.getSequence(peptideKey);
                    peptideTempStart = 0;
                    while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                        peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                        peptideTempEnd = peptideTempStart + peptideSequence.length();
                        for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                            coverage[j] = 1;
                        }
                        tempSequence = sequence.substring(0, peptideTempStart);
                    }
                }
            }

            double covered = 0.0;

            for (int aa : coverage) {
                covered += aa;
            }

            return covered / ((double) sequence.length());
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest.
     * 
     * @param proteinMatchKey the key of the protein match of interest
     * @return the corresponding spectrum counting metric
     */
    public Double getSpectrumCounting(String proteinMatchKey) {

        Double result = spectrumCounting.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (spectrumCounting.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }

            result = estimateSpectrumCounting(proteinMatchKey);
            spectrumCounting.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }
        return result;
    }

    /**
     * Returns the spectrum counting score based on the user's settings.
     * 
     * @param proteinMatch  the inspected protein match
     * @return the spectrum counting score
     */
    private double estimateSpectrumCounting(String proteinMatchKey) {

        double result;
        Enzyme enyzme = peptideShakerGUI.getSearchParameters().getEnzyme();
        PSParameter pSParameter = new PSParameter();
        Identification identification = peptideShakerGUI.getIdentification();
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        try {
            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
            if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.NSAF) {
                if (currentProtein == null) {
                    return 0.0;
                }
                result = 0;
                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    for (String spectrumMatchKey : peptideMatch.getSpectrumMatches()) {
                        pSParameter = (PSParameter) identification.getMatchParameter(spectrumMatchKey, pSParameter);
                        if (!peptideShakerGUI.getSpectrumCountingPreferences().isValidatedHits() || pSParameter.isValidated()) {
                            result++;
                        }
                    }
                }

                return result / currentProtein.getObservableLength(enyzme, peptideShakerGUI.getIdFilter().getMaxPepLength());

            } else { // emPAI

                if (peptideShakerGUI.getSpectrumCountingPreferences().isValidatedHits()) {
                    result = 0;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
                        if (pSParameter.isValidated()) {
                            result++;
                        }
                    }
                } else {
                    result = proteinMatch.getPeptideCount();
                }

                return Math.pow(10, result / currentProtein.getNPossiblePeptides(enyzme)) - 1;
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Returns the best protein coverage possible according to the given cleavage settings.
     * 
     * @param proteinMatchKey the key of the protein match of interest
     * @return the best protein coverage possible according to the given cleavage settings
     */
    public Double getObservableCoverage(String proteinMatchKey) {

        Double result = possibleCoverage.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (possibleCoverage.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }

            result = estimateObservableCoverage(proteinMatchKey);
            possibleCoverage.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }
        return result;
    }

    /**
     * Returns the best protein coverage possible according to the given cleavage settings.
     * 
     * @param proteinMatchKey the key of the protein match of interest
     * @return the best protein coverage possible according to the given cleavage settings
     */
    private double estimateObservableCoverage(String proteinMatchKey) {
        try {
            Enzyme enyzme = peptideShakerGUI.getSearchParameters().getEnzyme();
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
            return ((double) currentProtein.getObservableLength(enyzme, peptideShakerGUI.getIdFilter().getMaxPepLength())) / currentProtein.getLength();
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Estimates the number of spectra for the given protein match.
     * 
     * @param proteinMatchKey the key of the given protein match
     * @return the number of spectra for the given protein match
     */
    public Integer getNSpectra(String proteinMatchKey) {
        Integer result = nSpectra.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (nSpectra.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }
            result = estimateNSpectra(proteinMatchKey);
            nSpectra.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }

        return result;
    }

    /**
     * Returns the number of spectra where this protein was found independantly from the validation process.
     * 
     * @param proteinMatch the protein match of interest
     * @return the number of spectra where this protein was found
     */
    private int estimateNSpectra(String proteinMatchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int result = 0;

        try {
            PeptideMatch peptideMatch;
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                peptideMatch = identification.getPeptideMatch(peptideKey);
                result += peptideMatch.getSpectrumCount();
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return result;
    }

    /**
     * Returns the protein sequence annotated with modifications.
     * 
     * @param proteinKey the key of the protein match
     * @return the protein sequence annotated with modifications
     */
    public String getModifiedSequence(String proteinKey) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            String result = "";
            PSPtmScores psPtmScores = new PSPtmScores();
            psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);

            for (int aa = 0; aa < sequence.length(); aa++) {
                result += sequence.charAt(aa);
                if (!psPtmScores.getMainModificationsAt(aa).isEmpty()) {
                    boolean first = true;
                    result += "<";
                    for (String ptm : psPtmScores.getMainModificationsAt(aa)) {
                        if (first) {
                            first = false;
                        } else {
                            result += ", ";
                        }
                        result += ptmFactory.getPTM(ptm).getShortName();
                    }
                    result += ">";
                }
            }

            return result;
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            return "IO exception";
        }
    }

    /**
     * Transforms the protein accession number into an HTML link to the 
     * corresponding database. Note that this is a complete HTML with 
     * HTML and a href tags, where the main use is to include it in the 
     * protein tables.
     * 
     * @param proteinAccession   the protein to get the database link for
     * @return                   the transformed accession number
     */
    public String addDatabaseLink(String proteinAccession) {

        String accessionNumberWithLink = proteinAccession;

        try {
            if (sequenceFactory.getHeader(proteinAccession) != null) {

                // try to find the database from the SequenceDatabase
                DatabaseType databaseType = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                // create the database link
                if (databaseType != null) {

                    // @TODO: support more databases

                    if (databaseType == DatabaseType.IPI || databaseType == DatabaseType.UniProt) {
                        accessionNumberWithLink = "<html><a href=\"" + getUniProtAccessionLink(proteinAccession)
                                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else if (databaseType == DatabaseType.NCBI) {
                        accessionNumberWithLink = "<html><a href=\"" + getNcbiAccessionLink(proteinAccession)
                                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else {
                        // unknown database!
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return accessionNumberWithLink;
    }

    /**
     * Transforms the protein accesion number into an HTML link to the 
     * corresponding database. Note that this is a complete HTML with 
     * HTML and a href tags, where the main use is to include it in the 
     * protein tables.
     * 
     * @param proteins  the list of proteins to get the database links for
     * @return          the transformed accession number
     */
    public String addDatabaseLinks(ArrayList<String> proteins) {

        if (proteins.isEmpty()) {
            return "";
        }

        String accessionNumberWithLink = "<html>";

        for (int i = 0; i < proteins.size(); i++) {

            String proteinAccession = proteins.get(i);
            try {
                if (!SequenceFactory.isDecoy(proteins.get(i)) && sequenceFactory.getHeader(proteinAccession) != null) {

                    // try to find the database from the SequenceDatabase
                    DatabaseType database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                    // create the database link
                    if (database != null) {

                        // @TODO: support more databases

                        if (database == DatabaseType.IPI || database == DatabaseType.UniProt) {
                            accessionNumberWithLink += "<a href=\"" + getUniProtAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else if (database == DatabaseType.NCBI) {
                            accessionNumberWithLink += "<a href=\"" + getNcbiAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else {
                            // unknown database!
                            accessionNumberWithLink += proteinAccession + ", ";
                        }
                    }
                } else {
                    accessionNumberWithLink += proteinAccession + ", ";
                }
            } catch (Exception e) {
                accessionNumberWithLink += proteinAccession + ", ";
            }
        }

        // remove the last ', '
        accessionNumberWithLink = accessionNumberWithLink.substring(0, accessionNumberWithLink.length() - 2);
        accessionNumberWithLink += "</html>";

        return accessionNumberWithLink;
    }

    /**
     * Returns the protein accession number as a web link to the given 
     * protein at http://srs.ebi.ac.uk.
     * 
     * @param proteinAccession  the protein accession number
     * @param database          the protein database
     * @return                  the protein accession web link
     */
    public String getSrsAccessionLink(String proteinAccession, String database) {
        return "http://srs.ebi.ac.uk/srsbin/cgi-bin/wgetz?-e+%5b" + database + "-AccNumber:" + proteinAccession + "%5d";
    }

    /**
     * Returns the protein accession number as a web link to the given 
     * protein at http://www.uniprot.org/uniprot.
     * 
     * @param proteinAccession  the protein accession number
     * @return                  the protein accession web link
     */
    public String getUniProtAccessionLink(String proteinAccession) {
        return "http://www.uniprot.org/uniprot/" + proteinAccession;
    }

    /**
     * Returns the protein accession number as a web link to the given 
     * protein at http://www.ncbi.nlm.nih.gov/protein.
     * 
     * @param proteinAccession  the protein accession number
     * @return                  the protein accession web link
     */
    public String getNcbiAccessionLink(String proteinAccession) {
        return "http://www.ncbi.nlm.nih.gov/protein/" + proteinAccession;
    }

    /**
     * Returns a String with the HTML tooltip for the peptide indicating the 
     * modification details.
     * 
     * @param peptide
     * @return a String with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        String tooltip = "<html>";
        ArrayList<ModificationMatch> modifications = peptide.getModificationMatches();
        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        for (int i = 0; i < modifications.size(); i++) {

            PTM ptm = ptmFactory.getPTM(modifications.get(i).getTheoreticPtm());

            if (ptm.getType() == PTM.MODAA && modifications.get(i).isVariable()) {

                int modSite = modifications.get(i).getModificationSite();
                String modName = modifications.get(i).getTheoreticPtm();
                char affectedResidue = peptide.getSequence().charAt(modSite - 1);
                Color ptmColor = peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(modifications.get(i).getTheoreticPtm());

                if (!alreadyAnnotated.contains(modName + "_" + affectedResidue)) {
                    tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                            + affectedResidue
                            + "</span>"
                            + ": " + modName + "<br>";

                    alreadyAnnotated.add(modName + "_" + affectedResidue);
                }
            }
        }

        if (!tooltip.equalsIgnoreCase("<html>")) {
            tooltip += "</html>";
        } else {
            tooltip = null;
        }

        return tooltip;
    }

    /**
     * Returns the peptide with modification sites colored on the sequence. Shall be used for peptides, not PSMs.
     * 
     * @param peptideKey the peptide key
     * @param includeHtmlStartEndTag if true, html start and end tags are added
     * @return the colored peptide sequence
     */
    public String getColoredPeptideSequence(String peptideKey, boolean includeHtmlStartEndTag) {

        Identification identification = peptideShakerGUI.getIdentification();
        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
        PSPtmScores ptmScores = new PSPtmScores();
        ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
        if (ptmScores != null) {
            HashMap<Integer, ArrayList<String>> mainLocations = ptmScores.getMainModificationSites();
            HashMap<Integer, ArrayList<String>> secondaryLocations = ptmScores.getSecondaryModificationSites();
            return Peptide.getModifiedSequenceAsHtml(peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(),
                    includeHtmlStartEndTag, peptideMatch.getTheoreticPeptide(),
                    mainLocations, secondaryLocations);
        } else {
            return peptideMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                    peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), includeHtmlStartEndTag);
        }
    }
}
