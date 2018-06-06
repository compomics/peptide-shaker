package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.IdentificationFeaturesGenerator;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidPattern;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidSequence;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.amino_acid_tags.TagComponent;
import com.compomics.util.experiment.identification.amino_acid_tags.MassGap;
import com.compomics.util.experiment.identification.utils.ModificationUtils;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDatabase;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.gui.protein_sequence.ResidueAnnotation;
import com.compomics.util.experiment.identification.peptide_shaker.PSModificationScores;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * This class creates the display features needed for the GUI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class DisplayFeaturesGenerator {

    /**
     * The display parameters.
     */
    private final DisplayParameters displayParameters;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The notSelectedRowHtmlTagFontColor.
     */
    private final String notSelectedRowHtmlTagFontColor = TableProperties.getNotSelectedRowHtmlTagFontColor();
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The protein details provider.
     */
    private final ProteinDetailsProvider proteinDetailsProvider;

    /**
     * Constructor
     *
     * @param identificationParameters the identification parameters
     * @param displayParameters the display parameters
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     */
    public DisplayFeaturesGenerator(IdentificationParameters identificationParameters, DisplayParameters displayParameters, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider) {

        this.identificationParameters = identificationParameters;
        this.displayParameters = displayParameters;
        this.sequenceProvider = sequenceProvider;
        this.proteinDetailsProvider = proteinDetailsProvider;

    }

    /**
     * Transforms the protein accession number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteinAccession the protein to get the database link for
     *
     * @return the transformed accession number
     */
    public String getDatabaseLink(String proteinAccession) {

        // No link for decoy proteins
        if (ProteinUtils.isDecoy(proteinAccession, sequenceProvider)) {

            return proteinAccession;

        }

        // Get link according to the database type
        ProteinDatabase proteinDatabase = proteinDetailsProvider.getProteinDatabase(proteinAccession);

        switch (proteinDatabase) {

            case IPI:
            case UniProt:
                return String.join("",
                        "<html><a href=\"",
                        getUniProtAccessionLink(proteinAccession),
                        "\"><font color=\"",
                        notSelectedRowHtmlTagFontColor,
                        "\">",
                        proteinAccession,
                        "</font></a></html>");

            case NextProt:
                return String.join("",
                        "<html><a href=\"",
                        getNextProtAccessionLink(proteinAccession),
                        "\"><font color=\"",
                        notSelectedRowHtmlTagFontColor,
                        "\">",
                        proteinAccession,
                        "</font></a></html>");

            case NCBI:
                return String.join("",
                        "<html><a href=\"",
                        getNcbiAccessionLink(proteinAccession),
                        "\"><font color=\"",
                        notSelectedRowHtmlTagFontColor,
                        "\">",
                        proteinAccession,
                        "</font></a></html>");

            case UniRef:
                // remove the 'UniRefXYZ_' part to get the default UniProt accession
                String uniProtProteinAccession = proteinAccession.substring(proteinAccession.indexOf("_") + 1);
                return String.join("",
                        "<html><a href=\"" + getUniProtAccessionLink(uniProtProteinAccession),
                        "\"><font color=\"" + notSelectedRowHtmlTagFontColor + "\">",
                        proteinAccession,
                        "</font></a></html>");

            default:
                // not supported
                return proteinAccession;

        }
    }

    /**
     * Transforms the protein accession number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteinAccessions the list of the accessions of proteins to get
     * the database links for
     *
     * @return the transformed accession number
     */
    public String getDatabaseLinks(String[] proteinAccessions) {

        return proteinAccessions.length == 0 ? ""
                : Arrays.stream(proteinAccessions)
                        .sorted()
                        .map(accession -> getDatabaseLink(accession))
                        .collect(Collectors.joining(", ", "<html>", "</html>"));

    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at https://srs.ebi.ac.uk.
     *
     * @param proteinAccession the protein accession number
     * @param database the protein database
     * @return the protein accession web link
     */
    public String getSrsAccessionLink(String proteinAccession, String database) {

        return String.join("",
                "https://srs.ebi.ac.uk/srsbin/cgi-bin/wgetz?-e+%5b",
                database,
                "-AccNumber:",
                proteinAccession,
                "%5d");

    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at https://www.uniprot.org/uniprot.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getUniProtAccessionLink(String proteinAccession) {

        return "https://www.uniprot.org/uniprot/" + proteinAccession;

    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at https://www.nextprot.org.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getNextProtAccessionLink(String proteinAccession) {

        proteinAccession = proteinAccession.substring(0, proteinAccession.lastIndexOf("-")); // have to remove the isoform info
        return "https://www.nextprot.org/db/entry/" + proteinAccession;

    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at https://www.ncbi.nlm.nih.gov/protein.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getNcbiAccessionLink(String proteinAccession) {

        return "https://www.ncbi.nlm.nih.gov/protein/" + proteinAccession;

    }

    /**
     * Returns the project accession number as a web link to the given project
     * in PRIDE.
     *
     * @param projectAccession the project accession number
     * @return the project accession web link
     */
    public static String getPrideAccessionLink(String projectAccession) {

        return "https://www.ebi.ac.uk/pride/directLink.do?experimentAccessionNumber=" + projectAccession;

    }

    /**
     * Returns the project accession number as a web link to the given project
     * in the PRIDE archive.
     *
     * @param projectAccession the project accession number
     * @return the project accession web link
     */
    public static String getPrideProjectArchiveLink(String projectAccession) {

        return "https://www.ebi.ac.uk/pride/archive/projects/" + projectAccession;

    }

    /**
     * Returns the assay accession number as a web link to the given assay in
     * the PRIDE archive.
     *
     * @param projectAccession the project accession number
     * @param assayAccession the assay accession number
     * @return the project accession web link
     */
    public static String getPrideAssayArchiveLink(String projectAccession, String assayAccession) {

        return String.join("",
                "https://www.ebi.ac.uk/pride/archive/projects/",
                projectAccession,
                "/assays/",
                assayAccession);

    }

    /**
     * Returns a string with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param spectrumMatch the spectrum match
     * @return a string with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(SpectrumMatch spectrumMatch) {

        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        PSModificationScores modificationScores = new PSModificationScores();
        modificationScores = (PSModificationScores) spectrumMatch.getUrParam(modificationScores);
        return getPeptideModificationTooltipAsHtml(peptide, modificationScores);

    }

    /**
     * Returns a string with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptideMatch the peptide match
     * @return a string with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(PeptideMatch peptideMatch) {

        Peptide peptide = peptideMatch.getPeptide();
        PSModificationScores modificationScores = new PSModificationScores();
        modificationScores = (PSModificationScores) peptideMatch.getUrParam(modificationScores);
        return getPeptideModificationTooltipAsHtml(peptide, modificationScores);

    }

    /**
     * Returns a string with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide the peptide
     * @param modificationScores the modification scores
     * @return a string with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide, PSModificationScores modificationScores) {

        String peptideSequence = peptide.getSequence();

        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();

        String[] fixedModifications = getDisplayedModifications(
                peptide.getFixedModifications(modificationParameters, sequenceProvider, modificationSequenceMatchingParameters), displayParameters.getDisplayedModifications());
        String[] confidentVariableModifications = modificationScores == null ? new String[peptideSequence.length() + 2]
                : getFilteredConfidentModificationsSites(modificationScores, displayParameters.getDisplayedModifications(), peptideSequence.length());
        String[] ambiguousVariableModificationsRepresentative = modificationScores == null ? new String[peptideSequence.length() + 2]
                : getFilteredAmbiguousModificationsRepresentativeSites(modificationScores, displayParameters.getDisplayedModifications(), peptideSequence.length());

        return getModificationToolTip(peptideSequence, fixedModifications, confidentVariableModifications, ambiguousVariableModificationsRepresentative);
    }

    /**
     * Returns an array containing only the modifications to display.
     *
     * @param modificationArray the original array
     * @param displayedModifications the displayed modifications
     *
     * @return an array containing only the modifications to display
     */
    public static String[] getDisplayedModifications(String[] modificationArray, HashSet<String> displayedModifications) {

        String[] result = new String[modificationArray.length];

        for (int i = 0; i < modificationArray.length; i++) {

            String modName = modificationArray[i];

            if (modName != null && displayedModifications.contains(modName)) {

                result[i] = modName;

            }
        }

        return result;

    }

    /**
     * Returns a string with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide the peptide
     * @return a string with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();

        String[] fixedModifications = getDisplayedModifications(
                peptide.getFixedModifications(modificationParameters, sequenceProvider, modificationSequenceMatchingParameters), displayParameters.getDisplayedModifications());

        String[] confidentModifications = new String[peptide.getSequence().length() + 2];
        String[] ambigousModifications = new String[peptide.getSequence().length() + 2];

        for (ModificationMatch modMatch : peptide.getVariableModifications()) {

            String modName = modMatch.getModification();

            if (displayParameters.getDisplayedModifications().contains(modName)) {

                int site = modMatch.getSite();

                if (modMatch.getConfident()) {

                    confidentModifications[site] = modName;

                } else {

                    ambigousModifications[site] = modName;

                }
            }
        }

        return getModificationToolTip(peptide.getSequence(), fixedModifications, confidentModifications, ambigousModifications);

    }

    /**
     * Returns the modification tooltip as HTML. Null if no modification.
     *
     * @param peptideSequence the peptide sequence
     * @param fixedModifications the fixed modifications
     * @param confidentLocations the confident locations
     * @param representativeAmbiguousLocations the representative locations
     *
     * @return the modification tooltip as HTML
     */
    private String getModificationToolTip(String peptideSequence,
            String[] fixedModifications,
            String[] confidentLocations,
            String[] representativeAmbiguousLocations) {

        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();

        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html>");

        ArrayList<String> alreadyAnnotated = new ArrayList<>();

        for (int aa = 1; aa <= peptideSequence.length(); aa++) {

            int aaIndex = aa - 1;
            char aminoAcid = peptideSequence.charAt(aaIndex);

            String fixedMod = fixedModifications[aa];
            String confidentMod = confidentLocations[aa];
            String ambiguousMod = representativeAmbiguousLocations[aa];

            if (fixedMod != null || confidentMod != null || ambiguousMod != null) {

                StringBuilder tempTooltip = new StringBuilder(); // @TODO: should be a way of doing this without needing this second string builder..?
                ModificationUtils.appendTaggedResidue(tempTooltip, aminoAcid, confidentMod, ambiguousMod, null, fixedMod, modificationParameters, true, true);
                String currentResidueAnnotation = tempTooltip.toString();
                
                if (!alreadyAnnotated.contains(currentResidueAnnotation)) {

                    tooltip.append(currentResidueAnnotation).append(": ");

                    if (fixedMod != null) {
                        tooltip.append(fixedMod).append(" (fixed))<br>");
                    } else if (confidentMod != null) {
                        tooltip.append(confidentMod).append(" (confident))<br>");
                    } else if (ambiguousMod != null) {
                        tooltip.append(ambiguousMod).append(" (not confident))<br>");
                    }
                    
                    alreadyAnnotated.add(tempTooltip.toString());
                }

            }
        }

        tooltip.append("</html>");

        return tooltip.length() == 13 ? null : tooltip.toString();

    }

    /**
     * Returns a string with the HTML tooltip for the tag indicating the
     * modification details.
     *
     * @param tag the tag
     *
     * @return a string with the HTML tooltip for the tag
     */
    public String getTagModificationTooltipAsHtml(Tag tag) {

        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();

        // @TODO: merge with getTagModificationTooltipAsHtml in DeNovoGUI and move to utilities
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html>");

        ArrayList<TagComponent> tagComponents = tag.getContent();

        for (int i = 0; i < tagComponents.size(); i++) {

            TagComponent tagComponent = tagComponents.get(i);

            if (tagComponent instanceof AminoAcidSequence) {

                AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;

                String[] fixedMods = aminoAcidSequence.getFixedModifications(i == 0, i == tagComponents.size() - 1, modificationParameters, modificationSequenceMatchingParameters);
                String[] variableMods = aminoAcidSequence.getIndexedVariableModifications();

                for (int aa = 1; aa <= aminoAcidSequence.length(); aa++) {

                    int aaIndex = aa - 1;
                    char aminoAcid = aminoAcidSequence.charAt(aaIndex);

                    String fixedMod = fixedMods[aa];
                    String confidentMod = variableMods[aa];

                    ModificationUtils.appendTaggedResidue(tooltip, aminoAcid, confidentMod, null, null, fixedMod, modificationParameters, true, true);

                }
            }
        }

        tooltip.append("</html>");

        return tooltip.length() == 13 ? null : tooltip.toString();

    }

    /**
     * Returns the peptide with modification sites tagged (color coded or with
     * modification tags, e.g, &lt;mox&gt;) in the sequence based on
     * PeptideShaker site inference results for the given peptide match.
     *
     * @param peptideMatch the peptide match of interest
     * @param useHtmlColorCoding if true, color coded HTML is used, otherwise
     * modification tags, e.g, &lt;mox&gt;, are used
     * @param includeHtmlStartEndTags if true, HTML start and end tags are added
     * @param useShortName if true the short names are used in the tags
     *
     * @return the tagged peptide sequence
     */
    public String getTaggedPeptideSequence(PeptideMatch peptideMatch, boolean useHtmlColorCoding, boolean includeHtmlStartEndTags, boolean useShortName) {

        Peptide peptide = peptideMatch.getPeptide();
        PSModificationScores modificationScores = new PSModificationScores();
        modificationScores = (PSModificationScores) peptideMatch.getUrParam(modificationScores);
        return getTaggedPeptideSequence(peptide, modificationScores, useHtmlColorCoding, includeHtmlStartEndTags, useShortName);

    }

    /**
     * Returns the peptide with modification sites tagged (color coded or with
     * modification tags, e.g, &lt;mox&gt;) in the sequence based on
     * PeptideShaker site inference results for the best assumption of the given
     * spectrum match.
     *
     * @param spectrumMatch the spectrum match of interest
     * @param useHtmlColorCoding if true, color coded HTML is used, otherwise
     * modification tags, e.g, &lt;mox&gt;, are used
     * @param includeHtmlStartEndTags if true, HTML start and end tags are added
     * @param useShortName if true the short names are used in the tags
     *
     * @return the tagged peptide sequence
     */
    public String getTaggedPeptideSequence(SpectrumMatch spectrumMatch, boolean useHtmlColorCoding, boolean includeHtmlStartEndTags, boolean useShortName) {

        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        PSModificationScores modificationScores = new PSModificationScores();
        modificationScores = (PSModificationScores) spectrumMatch.getUrParam(modificationScores);
        return getTaggedPeptideSequence(peptide, modificationScores, useHtmlColorCoding, includeHtmlStartEndTags, useShortName);

    }

    /**
     * Returns the peptide with modification sites tagged (color coded or with
     * modification tags, e.g, &lt;mox&gt;) in the sequence based on the
     * provided modification localization scores.
     *
     * @param peptide the spectrum match of interest
     * @param modificationScores the modification localization scores
     * @param useHtmlColorCoding if true, color coded HTML is used, otherwise
     * modification tags, e.g, &lt;mox&gt;, are used
     * @param includeHtmlStartEndTags if true, HTML start and end tags are added
     * @param useShortName if true the short names are used in the tags
     *
     * @return the tagged peptide sequence
     */
    public String getTaggedPeptideSequence(Peptide peptide, PSModificationScores modificationScores, boolean useHtmlColorCoding, boolean includeHtmlStartEndTags, boolean useShortName) {

        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();

        String peptideSequence = peptide.getSequence();

        String[] fixedModifications = getDisplayedModifications(
                peptide.getFixedModifications(modificationParameters, sequenceProvider, modificationSequenceMatchingParameters), displayParameters.getDisplayedModifications());
        String[] confidentVariableModifications = modificationScores == null ? new String[peptideSequence.length() + 2]
                : getFilteredConfidentModificationsSites(modificationScores, displayParameters.getDisplayedModifications(), peptideSequence.length());
        String[] ambiguousVariableModificationsRepresentative = modificationScores == null ? new String[peptideSequence.length() + 2]
                : getFilteredAmbiguousModificationsRepresentativeSites(modificationScores, displayParameters.getDisplayedModifications(), peptideSequence.length());

        return PeptideUtils.getTaggedModifiedSequence(peptide, modificationParameters,
                confidentVariableModifications, ambiguousVariableModificationsRepresentative, null,
                fixedModifications, useHtmlColorCoding, includeHtmlStartEndTags, useShortName);
    }

    /**
     * Exports the confidently localized modification sites in a map: site &gt;
     * mapped modifications.
     *
     * @param modificationScores the PeptideShaker modification scores
     * @param displayedModifications list of modifications to display
     * @param peptideLength the length of the peptide
     *
     * @return a map of filtered modifications based on the user display
     * preferences
     */
    public static String[] getFilteredConfidentModificationsSites(PSModificationScores modificationScores, HashSet<String> displayedModifications, int peptideLength) {

        String[] result = new String[peptideLength + 2];

        for (String modName : displayedModifications) {

            for (int confidentSite : modificationScores.getConfidentSitesForModification(modName)) {

                result[confidentSite] = modName;

            }
        }

        return result;

    }

    /**
     * Exports the ambiguously localized modification representative sites in a
     * map: site &gt; mapped modifications.
     *
     * @param modificationScores the PeptideShaker modification scores
     * @param displayedModifications list of modifications to display
     * @param peptideLength the length of the peptide
     *
     * @return a map of filtered modifications based on the user display
     * preferences
     */
    public static String[] getFilteredAmbiguousModificationsRepresentativeSites(PSModificationScores modificationScores, HashSet<String> displayedModifications, int peptideLength) {

        String[] result = new String[peptideLength + 2];

        for (int representativeSite : modificationScores.getRepresentativeSites()) {

            for (String modName : modificationScores.getModificationsAtRepresentativeSite(representativeSite)) {

                if (displayedModifications.contains(modName)) {

                    result[representativeSite] = modName;

                }
            }
        }

        return result;
    }

    /**
     * Exports the ambiguously localized modification secondary sites in a map:
     * site &gt; mapped modifications.
     *
     * @param modificationScores the PeptideShaker modification scores
     * @param displayedModifications list of modifications to display
     * @param peptideLength the length of the peptide
     *
     * @return a map of filtered modifications based on the user display
     * preferences
     */
    public static String[] getFilteredAmbiguousModificationsSecondarySites(PSModificationScores modificationScores, HashSet<String> displayedModifications, int peptideLength) {

        String[] result = new String[peptideLength + 2];

        for (int representativeSite : modificationScores.getRepresentativeSites()) {

            HashMap<Integer, HashSet<String>> modificationsAtSite = modificationScores.getAmbiguousModificationsAtRepresentativeSite(representativeSite);

            for (int secondarySite : modificationsAtSite.keySet()) {

                if (secondarySite != representativeSite) {

                    for (String modName : modificationsAtSite.get(secondarySite)) {

                        if (displayedModifications.contains(modName)) {

                            result[secondarySite] = modName;

                        }
                    }
                }
            }
        }

        return result;

    }

    /**
     * Returns the GO accession number as a web link to the given GO term at
     * QuickGO.
     *
     * @param goAccession the GO accession number
     *
     * @return the GO accession number as a web link to the given GO term at
     * QuickGO
     */
    public String addGoLink(String goAccession) { // @TODO: move method to utilities

        return String.join("",
                "<html><a href=\"",
                getGoAccessionLink(goAccession),
                "\"><font color=\"",
                notSelectedRowHtmlTagFontColor,
                "\">",
                goAccession,
                "</font></a></html>");

    }

    /**
     * Returns the GO accession number as a web link to the given GO term at
     * QuickGO.
     *
     * @param goAccession the GO accession number
     *
     * @return the GO accession web link
     */
    public String getGoAccessionLink(String goAccession) {

        return String.join("", "https://www.ebi.ac.uk/QuickGO/GTerm?id=", goAccession);

    }

    /**
     * Returns the residue annotation for a given protein in a map for enzymatic
     * or not enzymatic peptides only. Residue number &gt; annotations. 0 is the
     * first amino acid.
     *
     * @param proteinMatchKey the key of the match of interest
     * @param sequenceMatchingPreferences The sequence matching preferences
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
     */
    public HashMap<Integer, ArrayList<ResidueAnnotation>> getResidueAnnotation(long proteinMatchKey, SequenceMatchingParameters sequenceMatchingPreferences,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, Metrics metrics, Identification identification,
            boolean allPeptides, SearchParameters searchParameters, boolean enzymatic) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String sequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());

        HashMap<Integer, ArrayList<ResidueAnnotation>> residueAnnotation = new HashMap<>(sequence.length());

        double[] coverage = identificationFeaturesGenerator.getCoverableAA(proteinMatchKey);
        double lastP = coverage[0];
        int lastIndex = 0;

        for (int i = 1; i < coverage.length; i++) {

            double p = coverage[i];

            if (p != lastP) {

                StringBuilder annotation = new StringBuilder();
                annotation.append(lastIndex + 1).append('-').append(i + 1);

                if (metrics.getPeptideLengthDistribution() != null) {

                    annotation.append(", ").append(Util.roundDouble(100 * lastP, 1)).append("% chance of coverage");

                } else if (lastP > 0.01) {

                    annotation.append(", possible to cover");

                }

                ArrayList<ResidueAnnotation> annotations = new ArrayList<>(1);
                annotations.add(new ResidueAnnotation(annotation.toString(), 0l, false));

                for (int j = lastIndex; j < i; j++) {

                    residueAnnotation.put(j, new ArrayList<>(annotations));

                }

                lastP = p;
                lastIndex = i;

            }
        }

        int i = coverage.length;
        StringBuilder annotation = new StringBuilder();
        annotation.append(lastIndex + 1).append("-").append(i);

        if (metrics.getPeptideLengthDistribution() != null) {

            annotation.append(", ").append(Util.roundDouble(100 * lastP, 1)).append("% chance of coverage");

        } else if (lastP > 0.01) {

            annotation.append(", possible to cover");

        }

        ArrayList<ResidueAnnotation> annotations = new ArrayList<>(1);
        annotations.add(new ResidueAnnotation(annotation.toString(), 0l, false));

        for (int j = lastIndex; j < i; j++) {

            residueAnnotation.put(j, new ArrayList<>(annotations));

        }

        for (long peptideMatchKey : proteinMatch.getPeptideMatchesKeys()) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);
            String peptideSequence = peptideMatch.getPeptide().getSequence();
            boolean enzymaticPeptide = true;

            if (!allPeptides) {

                DigestionParameters digestionPreferences = searchParameters.getDigestionParameters();

                if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                    PeptideUtils.isEnzymatic(peptideMatch.getPeptide(), sequenceProvider, digestionPreferences.getEnzymes());

                }
            }

            if (allPeptides || (enzymatic && enzymaticPeptide) || (!enzymatic && !enzymaticPeptide)) {

                String modifiedSequence = getTaggedPeptideSequence(peptideMatch, true, false, true);
                AminoAcidPattern aminoAcidPattern = AminoAcidPattern.getAminoAcidPatternFromString(peptideSequence);
                int[] startIndexes = aminoAcidPattern.getIndexes(sequence, sequenceMatchingPreferences);

                for (int startIndex : startIndexes) {

                    int endIndex = startIndex + peptideSequence.length();
                    String peptideTempStart = Integer.toString(startIndex);
                    String peptideTempEnd = Integer.toString(endIndex);

                    ResidueAnnotation newAnnotation = new ResidueAnnotation(String.join(" - ", peptideTempStart, modifiedSequence, peptideTempEnd), peptideMatchKey, true);

                    for (int j = startIndex - 1; j < endIndex - 1; j++) {

                        annotations = residueAnnotation.get(j);

                        if (annotations == null) {

                            annotations = new ArrayList<>(1);
                            residueAnnotation.put(j, annotations);

                        } else if (annotations.size() == 1 && !annotations.get(0).clickable) {

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
