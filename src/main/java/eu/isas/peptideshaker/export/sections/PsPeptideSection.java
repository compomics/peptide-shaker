package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the peptide related export features.
 *
 * @author Marc Vaudel
 */
public class PsPeptideSection {

    /**
     * The peptide features to export.
     */
    private ArrayList<PsPeptideFeature> peptideFeatures = new ArrayList<PsPeptideFeature>();
    /**
     * The PSM subsection if needed.
     */
    private PsPsmSection psmSection = null;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsPeptideSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> psmFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPeptideFeature) {
                peptideFeatures.add((PsPeptideFeature) exportFeature);
            } else if (exportFeature instanceof PsPsmFeature || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature || exportFeature instanceof PsFragmentFeature) {
                psmFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(peptideFeatures);
        if (!psmFeatures.isEmpty()) {
            psmSection = new PsPsmSection(psmFeatures, indexes, header, writer);
        }
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown if a math exception occurred when estimating the noise level in spectra
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, ArrayList<String> keys, int nSurroundingAA,
            String linePrefix, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        if (keys == null) {
            keys = new ArrayList<String>(identification.getPeptideIdentification());
        }

        int line = 1;

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(keys, parameters, psmSection != null, parameters, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                if (decoys || !peptideMatch.getTheoreticPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {

                    boolean first = true;

                    if (indexes) {
                        if (linePrefix != null) {
                            writer.write(linePrefix);
                        }
                        writer.write(line + "");
                        first = false;
                    }

                    for (ExportFeature exportFeature : peptideFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        PsPeptideFeature peptideFeature = (PsPeptideFeature) exportFeature;
                        writer.write(getfeature(identification, identificationFeaturesGenerator, identificationParameters, keys, nSurroundingAA, linePrefix, peptideMatch, psParameter, peptideFeature, validatedOnly, decoys, waitingHandler));
                    }
                    writer.newLine();
                    if (psmSection != null) {
                        String psmSectionPrefix = "";
                        if (linePrefix != null) {
                            psmSectionPrefix += linePrefix;
                        }
                        psmSectionPrefix += line + ".";
                        writer.increaseDepth();
                        if (waitingHandler != null) {
                            waitingHandler.setDisplayProgress(false);
                        }
                        psmSection.writeSection(identification, identificationFeaturesGenerator, identificationParameters, peptideMatch.getSpectrumMatchesKeys(), psmSectionPrefix, nSurroundingAA, validatedOnly, decoys, waitingHandler);
                        if (waitingHandler != null) {
                            waitingHandler.setDisplayProgress(true);
                        }
                        writer.decreseDepth();
                    }
                    line++;
                }
            }
        }
    }

    /**
     * Returns the component of the section corresponding to the given feature.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
     * @param peptideMatch the peptide match
     * @param psParameter the PeptideShaker parameters of the match
     * @param peptideFeature the peptide feature to export
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @return the component of the section corresponding to the given feature
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file while mapping potential modification sites
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping potential modification sites
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the ProteinTree
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the ProteinTree
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     */
    public static String getfeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, ArrayList<String> keys, int nSurroundingAA, String linePrefix, PeptideMatch peptideMatch, PSParameter psParameter, PsPeptideFeature peptideFeature, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (peptideFeature) {
            case accessions:
                StringBuilder proteins = new StringBuilder();
                ArrayList<String> accessions = peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                for (String accession : accessions) {
                    if (proteins.length() > 0) {
                        proteins.append("; ");
                    }
                    proteins.append(accession);
                }
                return proteins.toString();
            case protein_description:
                SequenceFactory sequenceFactory = SequenceFactory.getInstance();
                StringBuilder descriptions = new StringBuilder();
                accessions = peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                for (String accession : accessions) {
                    if (descriptions.length() > 0) {
                        descriptions.append("; ");
                    }
                    descriptions.append(sequenceFactory.getHeader(accession).getDescription());
                }
                return descriptions.toString();
            case protein_groups:
                HashSet<String> proteinGroups = identification.getProteinMatches(peptideMatch.getTheoreticPeptide());
                proteins = new StringBuilder();
                ArrayList<String> proteinGroupsList = new ArrayList<String>(proteinGroups);
                Collections.sort(proteinGroupsList);
                if (proteinGroupsList.size() > 1) {
                    identification.loadProteinMatchParameters(proteinGroupsList, psParameter, waitingHandler, false);
                }
                psParameter = new PSParameter();
                for (String proteinGroup : proteinGroupsList) {
                    if (identification.getProteinIdentification().contains(proteinGroup)) {
                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinGroup, psParameter);
                        if (proteins.length() > 0) {
                            proteins.append("; ");
                        }
                        List<String> groupAccessions = Arrays.asList(ProteinMatch.getAccessions(proteinGroup));
                        Collections.sort(groupAccessions);
                        boolean first = true;
                        for (String accession : groupAccessions) {
                            if (first) {
                                first = false;
                            } else {
                                proteins.append(", ");
                            }
                            proteins.append(accession);
                        }
                        proteins.append(" (");
                        proteins.append(psParameter.getMatchValidationLevel().getName());
                        proteins.append(")");
                    }
                }
                return proteins.toString();
            case best_protein_group_validation:
                MatchValidationLevel bestProteinValidationLevel = MatchValidationLevel.none;
                proteinGroups = identification.getProteinMatches(peptideMatch.getTheoreticPeptide());
                proteinGroupsList = new ArrayList<String>(proteinGroups);
                Collections.sort(proteinGroupsList);
                if (proteinGroupsList.size() > 1) {
                    identification.loadProteinMatchParameters(proteinGroupsList, psParameter, waitingHandler, false);
                }
                psParameter = new PSParameter();
                for (String proteinGroup : proteinGroupsList) {
                    if (identification.getProteinIdentification().contains(proteinGroup)) {
                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinGroup, psParameter);
                        if (psParameter.getMatchValidationLevel().getIndex() > bestProteinValidationLevel.getIndex()) {
                            bestProteinValidationLevel = psParameter.getMatchValidationLevel();
                        }
                    }
                }
                return bestProteinValidationLevel.getName();
            case confidence:
                return psParameter.getPeptideConfidence() + "";
            case decoy:
                if (peptideMatch.getTheoreticPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {
                    return "1";
                } else {
                    return "0";
                }
            case hidden:
                if (psParameter.isHidden()) {
                    return "1";
                } else {
                    return "0";
                }
            case localization_confidence:
                return getPeptideModificationLocations(peptideMatch, identificationParameters.getSearchParameters().getPtmSettings()) + "";
            case pi:
                return psParameter.getProteinInferenceClassAsString();
            case position:
                accessions = peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                Peptide peptide = peptideMatch.getTheoreticPeptide();
                String start = "";
                for (String proteinAccession : accessions) {
                    if (!start.equals("")) {
                        start += "; ";
                    }
                    Protein protein = SequenceFactory.getInstance().getProtein(proteinAccession);
                    ArrayList<Integer> starts = protein.getPeptideStart(peptide.getSequence(),
                            identificationParameters.getSequenceMatchingPreferences());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            start += ", ";
                        }
                        start += startAa;
                    }
                }
                return start;
            case psms:
                return peptideMatch.getSpectrumCount() + "";
            case variable_ptms:
                return Peptide.getPeptideModificationsAsString(peptideMatch.getTheoreticPeptide(), true);
            case fixed_ptms:
                return Peptide.getPeptideModificationsAsString(peptideMatch.getTheoreticPeptide(), false);
            case score:
                return psParameter.getPeptideScore() + "";
            case raw_score:
                return psParameter.getPeptideProbabilityScore() + "";
            case sequence:
                return peptideMatch.getTheoreticPeptide().getSequence();
            case missed_cleavages:
                peptide = peptideMatch.getTheoreticPeptide();
                Integer nMissedCleavages = peptide.getNMissedCleavages(identificationParameters.getSearchParameters().getDigestionPreferences());
                if (nMissedCleavages == null) {
                    nMissedCleavages = 0;
                }
                return nMissedCleavages + "";
            case modified_sequence:
                return peptideMatch.getTheoreticPeptide().getTaggedModifiedSequence(identificationParameters.getSearchParameters().getPtmSettings(), false, false, true);
            case starred:
                if (psParameter.isStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case aaBefore:
                peptide = peptideMatch.getTheoreticPeptide();
                accessions = peptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                String subSequence = "";
                for (String proteinAccession : accessions) {
                    if (!subSequence.equals("")) {
                        subSequence += "; ";
                    }
                    HashMap<Integer, String[]> surroundingAAs = SequenceFactory.getInstance().getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                            nSurroundingAA, identificationParameters.getSequenceMatchingPreferences());
                    ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            subSequence += ", ";
                        }
                        subSequence += surroundingAAs.get(startAa)[0];
                    }
                }
                return subSequence;
            case aaAfter:
                peptide = peptideMatch.getTheoreticPeptide();
                accessions = peptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                subSequence = "";
                for (String proteinAccession : accessions) {
                    if (!subSequence.equals("")) {
                        subSequence += "; ";
                    }
                    HashMap<Integer, String[]> surroundingAAs
                            = SequenceFactory.getInstance().getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                    nSurroundingAA, identificationParameters.getSequenceMatchingPreferences());
                    ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            subSequence += ", ";
                        }
                        subSequence += surroundingAAs.get(startAa)[1];
                    }
                }
                return subSequence;
            case nValidatedProteinGroups:
                peptide = peptideMatch.getTheoreticPeptide();
                return identificationFeaturesGenerator.getNValidatedProteinGroups(peptide, waitingHandler) + "";
            case unique_database:
                peptide = peptideMatch.getTheoreticPeptide();
                if (identification.isUniqueInDatabase(peptide)) {
                    return "1";
                } else {
                    return "0";
                }
            case validated:
                return psParameter.getMatchValidationLevel().toString();
            case validated_psms:
                return identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideMatch.getKey()) + "";
            case probabilistic_score:
                PSPtmScores ptmScores = new PSPtmScores();
                ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
                if (ptmScores != null) {
                    StringBuilder result = new StringBuilder();
                    ArrayList<String> modList = new ArrayList<String>(ptmScores.getScoredPTMs());
                    Collections.sort(modList);
                    for (String mod : modList) {
                        PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                        ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getProbabilisticSites());
                        if (!sites.isEmpty()) {
                            Collections.sort(sites);
                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(mod).append(" (");
                            boolean firstSite = true;
                            for (int site : sites) {
                                if (firstSite) {
                                    firstSite = false;
                                } else {
                                    result.append(", ");
                                }
                                result.append(site).append(": ").append(ptmScoring.getProbabilisticScore(site));
                            }
                            result.append(")");
                        }
                    }
                    return result.toString();
                }
                return "";
            case d_score:
                StringBuilder result = new StringBuilder();
                ptmScores = new PSPtmScores();
                ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
                if (ptmScores != null) {
                    ArrayList<String> modList = new ArrayList<String>(ptmScores.getScoredPTMs());
                    Collections.sort(modList);
                    for (String mod : modList) {
                        PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                        ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getDSites());
                        if (!sites.isEmpty()) {
                            Collections.sort(sites);
                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(mod).append(" (");
                            boolean firstSite = true;
                            for (int site : sites) {
                                if (firstSite) {
                                    firstSite = false;
                                } else {
                                    result.append(", ");
                                }
                                result.append(site).append(": ").append(ptmScoring.getDeltaScore(site));
                            }
                            result.append(")");
                        }
                    }
                    return result.toString();
                }
                return "";
            case confident_modification_sites:
                String sequence = peptideMatch.getTheoreticPeptide().getSequence();
                return identificationFeaturesGenerator.getConfidentPtmSites(peptideMatch, sequence);
            case confident_modification_sites_number:
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(peptideMatch);
            case ambiguous_modification_sites:
                sequence = peptideMatch.getTheoreticPeptide().getSequence();
                return identificationFeaturesGenerator.getAmbiguousPtmSites(peptideMatch, sequence);
            case ambiguous_modification_sites_number:
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(peptideMatch);
            case confident_phosphosites:
                ArrayList<String> modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getConfidentPtmSites(peptideMatch, peptideMatch.getTheoreticPeptide().getSequence(), modifications);
            case confident_phosphosites_number:
                modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(peptideMatch, modifications);
            case ambiguous_phosphosites:
                modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSites(peptideMatch, peptideMatch.getTheoreticPeptide().getSequence(), modifications);
            case ambiguous_phosphosites_number:
                modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(peptideMatch, modifications);
            default:
                return "Not implemented";
        }
    }

    /**
     * Returns the peptide modification location confidence as a string.
     *
     * @param peptideMatch the peptide match
     * @param ptmProfile the PTM profile
     *
     * @return the peptide modification location confidence as a string
     */
    public static String getPeptideModificationLocations(PeptideMatch peptideMatch, PtmSettings ptmProfile) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) peptideMatch.getUrParam(psPtmScores);

        if (psPtmScores != null) {
            ArrayList<String> modList = psPtmScores.getScoredPTMs();

            StringBuilder result = new StringBuilder();
            Collections.sort(modList);

            for (String mod : modList) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                PSPtmScores ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
                result.append(mod).append(" (");
                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                boolean firstSite = true;
                for (int site : ptmScoring.getOrderedPtmLocations()) {
                    if (firstSite) {
                        firstSite = false;
                    } else {
                        result.append(", ");
                    }
                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                    if (ptmConfidence == PtmScoring.NOT_FOUND) {
                        result.append(site).append(": Not Scored");
                    } else if (ptmConfidence == PtmScoring.RANDOM) {
                        result.append(site).append(": Random");
                    } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                        result.append(site).append(": Doubtfull");
                    } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                        result.append(site).append(": Confident");
                    } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                        result.append(site).append(": Very Confident");
                    }
                }
                result.append(")");
            }

            return result.toString();
        }
        return "";
    }

    /**
     * Writes the title of the section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : peptideFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        writer.newLine();
    }
}
