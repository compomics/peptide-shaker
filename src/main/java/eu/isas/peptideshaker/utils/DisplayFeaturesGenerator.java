package eu.isas.peptideshaker.utils;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.tags.Tag;
import com.compomics.util.experiment.identification.tags.TagComponent;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.protein.Header;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.protein_sequence.ResidueAnnotation;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class creates the display features needed for the GUI.
 *
 * @author Marc Vaudel
 */
public class DisplayFeaturesGenerator {

    /**
     * The modification profile containing the colors of the PTMs.
     */
    private ModificationProfile modificationProfile;
    /**
     * The notSelectedRowHtmlTagFontColor.
     */
    private String notSelectedRowHtmlTagFontColor = TableProperties.getNotSelectedRowHtmlTagFontColor();
    /**
     * The exception handler used to catch exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * List of PTMs to display.
     */
    private ArrayList<String> displayedPTMs;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Constructor
     *
     * @param modificationProfile the modification profile containing the colors
     * of the ptms
     * @param exceptionHandler an exception handler to catch exceptions
     */
    public DisplayFeaturesGenerator(ModificationProfile modificationProfile, ExceptionHandler exceptionHandler) {
        this.modificationProfile = modificationProfile;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Transforms the protein accession number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteinAccession the protein to get the database link for
     * @return the transformed accession number
     */
    public String addDatabaseLink(String proteinAccession) {

        String accessionNumberWithLink = proteinAccession;

        try {
            if (sequenceFactory.getHeader(proteinAccession) != null) {

                // try to find the database from the SequenceDatabase
                Header.DatabaseType databaseType = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                // create the database link
                if (databaseType != null) {

                    // @TODO: support more databases
                    if (databaseType == Header.DatabaseType.IPI || databaseType == Header.DatabaseType.UniProt) {
                        accessionNumberWithLink = "<html><a href=\"" + getUniProtAccessionLink(proteinAccession)
                                + "\"><font color=\"" + notSelectedRowHtmlTagFontColor + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else if (databaseType == Header.DatabaseType.NCBI) {
                        accessionNumberWithLink = "<html><a href=\"" + getNcbiAccessionLink(proteinAccession)
                                + "\"><font color=\"" + notSelectedRowHtmlTagFontColor + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else {
                        // unknown database!
                    }
                }
            }
        } catch (Exception e) {
            exceptionHandler.catchException(e);
        }

        return accessionNumberWithLink;
    }

    /**
     * Transforms the protein accesion number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteins the list of proteins to get the database links for
     * @return the transformed accession number
     */
    public String addDatabaseLinks(ArrayList<String> proteins) {

        if (proteins.isEmpty()) {
            return "";
        }

        StringBuilder accessionNumberWithLink = new StringBuilder();
        accessionNumberWithLink.append("<html>");

        for (int i = 0; i < proteins.size(); i++) {

            String proteinAccession = proteins.get(i);

            try {
                if (!sequenceFactory.isDecoyAccession(proteins.get(i)) && sequenceFactory.getHeader(proteinAccession) != null) {

                    // try to find the database from the SequenceDatabase
                    DatabaseType database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                    // create the database link
                    if (database != null) {

                        // @TODO: support more databases
                        if (database == DatabaseType.IPI || database == DatabaseType.UniProt) {
                            accessionNumberWithLink.append("<a href=\"");
                            accessionNumberWithLink.append(getUniProtAccessionLink(proteinAccession));
                            accessionNumberWithLink.append("\"><font color=\"");
                            accessionNumberWithLink.append(notSelectedRowHtmlTagFontColor);
                            accessionNumberWithLink.append("\">");
                            accessionNumberWithLink.append(proteinAccession);
                            accessionNumberWithLink.append("</font></a>, ");
                        } else if (database == DatabaseType.NCBI) {
                            accessionNumberWithLink.append("<a href=\"");
                            accessionNumberWithLink.append(getNcbiAccessionLink(proteinAccession));
                            accessionNumberWithLink.append("\"><font color=\"");
                            accessionNumberWithLink.append(notSelectedRowHtmlTagFontColor);
                            accessionNumberWithLink.append("\">");
                            accessionNumberWithLink.append(proteinAccession);
                            accessionNumberWithLink.append("</font></a>, ");
                        } else {
                            // unknown database!
                            accessionNumberWithLink.append(proteinAccession);
                            accessionNumberWithLink.append(", ");
                        }
                    }
                } else {
                    accessionNumberWithLink.append(proteinAccession);
                    accessionNumberWithLink.append(", ");
                }
            } catch (Exception e) {
                accessionNumberWithLink.append(proteinAccession);
                accessionNumberWithLink.append(", ");
            }
        }

        // remove the last ', '
        String accessionNumberWithLinkAsString = accessionNumberWithLink.toString();
        accessionNumberWithLinkAsString = accessionNumberWithLinkAsString.substring(0, accessionNumberWithLinkAsString.length() - 2);
        accessionNumberWithLinkAsString += "</html>";

        return accessionNumberWithLinkAsString;
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://srs.ebi.ac.uk.
     *
     * @param proteinAccession the protein accession number
     * @param database the protein database
     * @return the protein accession web link
     */
    public String getSrsAccessionLink(String proteinAccession, String database) {
        return "http://srs.ebi.ac.uk/srsbin/cgi-bin/wgetz?-e+%5b" + database + "-AccNumber:" + proteinAccession + "%5d";
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://www.uniprot.org/uniprot.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getUniProtAccessionLink(String proteinAccession) {
        return "http://www.uniprot.org/uniprot/" + proteinAccession;
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://www.ncbi.nlm.nih.gov/protein.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getNcbiAccessionLink(String proteinAccession) {
        return "http://www.ncbi.nlm.nih.gov/protein/" + proteinAccession;
    }

    /**
     * Returns the project accession number as a web link to the given project
     * in PRIDE.
     *
     * @param projectAccession the project accession number
     * @return the project accession web link
     */
    public static String getPrideAccessionLink(String projectAccession) {
        return "http://www.ebi.ac.uk/pride/directLink.do?experimentAccessionNumber=" + projectAccession;
    }

    /**
     * Returns a String with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide
     * @return a String with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        // @TODO: merge with getTagModificationTooltipAsHtml below (and in DeNovoGUI) -  and moved to utilities?
        String tooltip = "<html>";
        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            String modName = modMatch.getTheoreticPtm();
            PTM ptm = ptmFactory.getPTM(modName);

            if (ptm.getType() == PTM.MODAA && displayedPTMs.contains(modName)) {

                int modSite = modMatch.getModificationSite();

                if (modSite > 0) {
                    char affectedResidue = peptide.getSequence().charAt(modSite - 1);
                    Color ptmColor = modificationProfile.getColor(modName);

                    if (!alreadyAnnotated.contains(modName + "_" + affectedResidue)) {
                        tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                + affectedResidue
                                + "</span>"
                                + ": " + modName + "<br>";

                        alreadyAnnotated.add(modName + "_" + affectedResidue);
                    }
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
     * Returns a String with the HTML tooltip for the tag indicating the
     * modification details.
     *
     * @param tag the tag
     * @return a String with the HTML tooltip for the tag
     */
    public String getTagModificationTooltipAsHtml(Tag tag) {

        // @TODO: merge with getTagModificationTooltipAsHtml in DeNovoGUI and move to utilities
        String tooltip = "<html>";

        for (TagComponent tagComponent : tag.getContent()) {
            if (tagComponent instanceof AminoAcidPattern) {
                AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;
                for (int site = 1; site <= aminoAcidPattern.length(); site++) {
                    for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(site)) {
                        String affectedResidue = aminoAcidPattern.asSequence(site - 1);
                        String modName = modificationMatch.getTheoreticPtm();
                        Color ptmColor = modificationProfile.getColor(modName);
                        tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                + affectedResidue
                                + "</span>"
                                + ": " + modName + "<br>";
                    }
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
     * Returns the peptide with modification sites tagged (color coded or with
     * PTM tags, e.g, &lt;mox&gt;) in the sequence based on PeptideShaker site
     * inference results for the given peptide match.
     *
     * @param peptideMatch the peptide match of interest
     * @param useHtmlColorCoding if true, color coded HTML is used, otherwise
     * PTM tags, e.g, &lt;mox&gt;, are used
     * @param includeHtmlStartEndTags if true, HTML start and end tags are added
     * @param useShortName if true the short names are used in the tags
     * @return the tagged peptide sequence
     */
    public String getTaggedPeptideSequence(PeptideMatch peptideMatch, boolean useHtmlColorCoding, boolean includeHtmlStartEndTags, boolean useShortName) {
        try {

            Peptide peptide = peptideMatch.getTheoreticPeptide();

            HashMap<Integer, ArrayList<String>> fixedModifications = getFilteredModifications(peptide.getIndexedFixedModifications(), displayedPTMs);
            HashMap<Integer, ArrayList<String>> mainLocations = new HashMap<Integer, ArrayList<String>>();
            HashMap<Integer, ArrayList<String>> secondaryLocations = new HashMap<Integer, ArrayList<String>>();

            PSPtmScores ptmScores = new PSPtmScores();
            ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
            if (ptmScores != null) {
                mainLocations = getFilteredModifications(ptmScores.getMainModificationSites(), displayedPTMs);
                secondaryLocations = getFilteredModifications(ptmScores.getSecondaryModificationSites(), displayedPTMs);
            }
            return Peptide.getTaggedModifiedSequence(modificationProfile,
                    peptide, mainLocations, secondaryLocations, fixedModifications, useHtmlColorCoding, includeHtmlStartEndTags, useShortName);
        } catch (Exception e) {
            exceptionHandler.catchException(e);
            return "Error";
        }
    }

    /**
     * Returns the peptide with modification sites tagged (color coded or with
     * PTM tags, e.g, &lt;mox&gt;) in the sequence based on PeptideShaker site
     * inference results for the best assumption of the given spectrum match.
     *
     * @param spectrumMatch the spectrum match of interest
     * @param useHtmlColorCoding if true, color coded HTML is used, otherwise
     * PTM tags, e.g, &lt;mox&gt;, are used
     * @param includeHtmlStartEndTags if true, HTML start and end tags are added
     * @param useShortName if true the short names are used in the tags
     * @return the tagged peptide sequence
     */
    public String getTaggedPeptideSequence(SpectrumMatch spectrumMatch, boolean useHtmlColorCoding, boolean includeHtmlStartEndTags, boolean useShortName) {
        try {

            Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

            HashMap<Integer, ArrayList<String>> fixedModifications = getFilteredModifications(peptide.getIndexedFixedModifications(), displayedPTMs);
            HashMap<Integer, ArrayList<String>> mainLocations = new HashMap<Integer, ArrayList<String>>();
            HashMap<Integer, ArrayList<String>> secondaryLocations = new HashMap<Integer, ArrayList<String>>();

            PSPtmScores ptmScores = new PSPtmScores();
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
            if (ptmScores != null) {
                mainLocations = getFilteredModifications(ptmScores.getMainModificationSites(), displayedPTMs);
                secondaryLocations = getFilteredModifications(ptmScores.getSecondaryModificationSites(), displayedPTMs);
            }
            return Peptide.getTaggedModifiedSequence(modificationProfile,
                    peptide, mainLocations, secondaryLocations, fixedModifications, useHtmlColorCoding, includeHtmlStartEndTags, useShortName);
        } catch (Exception e) {
            exceptionHandler.catchException(e);
            return "Error";
        }
    }

    /**
     * Returns the modified sequence as an tagged string with potential
     * modification sites color coded or with PTM tags, e.g, &lt;mox&gt;. /!\
     * This method will work only if the PTM found in the peptide are in the
     * PTMFactory. /!\ This method uses the modifications as set in the
     * modification matches of this peptide and displays all of them.
     *
     * @param peptide the peptide
     * @param useHtmlColorCoding if true, color coded HTML is used, otherwise
     * PTM tags, e.g, &lt;mox&gt;, are used
     * @param includeHtmlStartEndTag if true, start and end HTML tags are added
     * @param useShortName if true the short names are used in the tags
     * @return the tagged sequence as a string
     */
    public String getTaggedPeptideSequence(Peptide peptide, boolean useHtmlColorCoding, boolean includeHtmlStartEndTag, boolean useShortName) {

        HashMap<Integer, ArrayList<String>> mainModificationSites = new HashMap<Integer, ArrayList<String>>();
        HashMap<Integer, ArrayList<String>> secondaryModificationSites = new HashMap<Integer, ArrayList<String>>();
        HashMap<Integer, ArrayList<String>> fixedModificationSites = new HashMap<Integer, ArrayList<String>>();

        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            String modName = modMatch.getTheoreticPtm();
            if (displayedPTMs.contains(modName)) {

                if (ptmFactory.getPTM(modMatch.getTheoreticPtm()).getType() == PTM.MODAA) { // exclude terminal ptms

                    int modSite = modMatch.getModificationSite();

                    if (modMatch.isVariable()) {
                        if (modMatch.isConfident()) {
                            if (!mainModificationSites.containsKey(modSite)) {
                                mainModificationSites.put(modSite, new ArrayList<String>());
                            }
                            mainModificationSites.get(modSite).add(modName);
                        } else {
                            if (!secondaryModificationSites.containsKey(modSite)) {
                                secondaryModificationSites.put(modSite, new ArrayList<String>());
                            }
                            secondaryModificationSites.get(modSite).add(modName);
                        }
                    } else {
                        if (!fixedModificationSites.containsKey(modSite)) {
                            fixedModificationSites.put(modSite, new ArrayList<String>());
                        }
                        fixedModificationSites.get(modSite).add(modName);
                    }
                }
            }
        }

        return Peptide.getTaggedModifiedSequence(modificationProfile, peptide, mainModificationSites,
                secondaryModificationSites, fixedModificationSites, useHtmlColorCoding, includeHtmlStartEndTag, useShortName);
    }

    /**
     * Filters the modification map according to the user's display preferences.
     *
     * @param modificationMap the map of modifications to filter (amino acid ->
     * list of modifications, 1 is the first amino acid)
     * @param displayedPtms list of PTMs to display
     *
     * @return a map of filtered modifications based on the user display
     * preferences
     */
    public static HashMap<Integer, ArrayList<String>> getFilteredModifications(HashMap<Integer, ArrayList<String>> modificationMap, ArrayList<String> displayedPtms) {
        HashMap<Integer, ArrayList<String>> result = new HashMap<Integer, ArrayList<String>>();
        for (int aa : modificationMap.keySet()) {
            for (String ptm : modificationMap.get(aa)) {
                if (displayedPtms.contains(ptm)) {
                    if (!result.containsKey(aa)) {
                        result.put(aa, new ArrayList<String>());
                    }
                    result.get(aa).add(ptm);
                }
            }
        }
        return result;
    }

    /**
     * Returns the GO accession number as a web link to the given GO term at
     * QuickGO.
     *
     * @param goAccession
     * @return the GO accession number as a web link to the given GO term at
     * QuickGO
     */
    public String addGoLink(String goAccession) { // @TODO: move method to utilities...
        return "<html><a href=\"" + getGoAccessionLink(goAccession)
                + "\"><font color=\"" + notSelectedRowHtmlTagFontColor + "\">"
                + goAccession + "</font></a></html>";
    }

    /**
     * Returns the GO accession number as a web link to the given GO term at
     * QuickGO.
     *
     * @param goAccession the GO accession number
     * @return the GO accession web link
     */
    public String getGoAccessionLink(String goAccession) {
        return "http://www.ebi.ac.uk/QuickGO/GTerm?id=" + goAccession;
    }

    /**
     * Sets the PTMs to display.
     *
     * @param displayedPTMs the names of the PTMs to display in a list
     */
    public void setDisplayedPTMs(ArrayList<String> displayedPTMs) {
        this.displayedPTMs = displayedPTMs;
    }

    /**
     * Returns the residue annotation for a given protein in a map for enzymatic
     * or not enzymatic peptides only. Residue number -> annotations. 0 is the
     * first amino acid.
     *
     * @param proteinMatchKey the key of the match of interest
     * @param matchingType the type of sequence matching to use
     * @param massTolerance the MS2 mass tolerance
     * @param identificationFeaturesGenerator the identification feature
     * generator
     * @param metrics the metrics
     * @param identification the identification
     * @param allPeptides if true, all peptides are considered
     * @param searchParameters the search parameters
     * @param enzymatic whether enzymatic only or not enzymatic only peptides
     * should be considered
     *
     * @return the residue annotation for a given protein
     *
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public HashMap<Integer, ArrayList<ResidueAnnotation>> getResidueAnnotation(String proteinMatchKey, AminoAcidPattern.MatchingType matchingType,
            Double massTolerance, IdentificationFeaturesGenerator identificationFeaturesGenerator, Metrics metrics, Identification identification,
            boolean allPeptides, SearchParameters searchParameters, boolean enzymatic)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
        String sequence = currentProtein.getSequence();

        HashMap<Integer, ArrayList<ResidueAnnotation>> residueAnnotation = new HashMap<Integer, ArrayList<ResidueAnnotation>>(sequence.length());

        double[] coverage = identificationFeaturesGenerator.getCoverableAA(proteinMatchKey);
        double lastP = coverage[0];
        int lastIndex = 0;
        for (int i = 1; i < coverage.length; i++) {
            double p = coverage[i];
            if (p != lastP) {
                String annotation = (lastIndex + 1) + "-" + (i + 1);
                if (metrics.getPeptideLengthDistribution() != null) {
                    annotation += ", " + Util.roundDouble(100 * lastP, 1) + "% chances of coverage";
                } else if (lastP > 0.01) {
                    annotation += ", possible to cover";
                }
                ArrayList<ResidueAnnotation> annotations = new ArrayList<ResidueAnnotation>(1);
                annotations.add(new ResidueAnnotation(annotation, null, false));
                for (int j = lastIndex; j < i; j++) {
                    residueAnnotation.put(j, new ArrayList<ResidueAnnotation>(annotations));
                }
                lastP = p;
                lastIndex = i;
            }
        }
        int i = coverage.length;
        String annotation = (lastIndex + 1) + "-" + (i);
        if (metrics.getPeptideLengthDistribution() != null) {
            annotation += ", " + Util.roundDouble(100 * lastP, 1) + "% chances of coverage";
        } else if (lastP > 0.01) {
            annotation += ", possible to cover";
        }
        ArrayList<ResidueAnnotation> annotations = new ArrayList<ResidueAnnotation>(1);
        annotations.add(new ResidueAnnotation(annotation, null, false));
        for (int j = lastIndex; j < i; j++) {
            residueAnnotation.put(j, new ArrayList<ResidueAnnotation>(annotations));
        }

        // batch load the required data
        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);

        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            String peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();
            boolean enzymaticPeptide = true;
            if (!allPeptides) {
                enzymaticPeptide = currentProtein.isEnzymaticPeptide(peptideSequence, searchParameters.getEnzyme(),
                        PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
            }
            if (allPeptides || (enzymatic && enzymaticPeptide) || (!enzymatic && !enzymatic)) {
                String modifiedSequence = getTaggedPeptideSequence(peptideMatch, true, false, true);
                AminoAcidPattern aminoAcidPattern = new AminoAcidPattern(peptideSequence);
                ArrayList<Integer> startIndexes = aminoAcidPattern.getIndexes(sequence, matchingType, massTolerance);
                for (int index : startIndexes) {
                    int peptideTempStart = index;
                    int peptideTempEnd = peptideTempStart + peptideSequence.length();
                    ResidueAnnotation newAnnotation = new ResidueAnnotation(peptideTempStart + " - " + modifiedSequence + " - " + peptideTempEnd, peptideKey, true);
                    for (int j = peptideTempStart - 1; j < peptideTempEnd - 1; j++) {
                        annotations = residueAnnotation.get(j);
                        if (annotations == null) {
                            annotations = new ArrayList<ResidueAnnotation>();
                            residueAnnotation.put(j, annotations);
                        } else if (annotations.size() == 1 && !annotations.get(0).isClickable()) {
                            annotations.clear();
                        }
                        annotations.add(newAnnotation);
                    }
                }
            }
        }

        return residueAnnotation;
    }
}
