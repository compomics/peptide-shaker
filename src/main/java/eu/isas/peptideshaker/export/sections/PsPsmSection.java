package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
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
 * This class outputs the PSM level export features.
 *
 * @author Marc Vaudel
 */
public class PsPsmSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsPsmFeature> psmFeatures = new ArrayList<>();
    /**
     * The features to export.
     */
    private final ArrayList<PsIdentificationAlgorithmMatchesFeature> identificationAlgorithmMatchesFeatures = new ArrayList<>();
    /**
     * The fragment subsection if needed.
     */
    private PsFragmentSection fragmentSection = null;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private final boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private final boolean header;
    /**
     * The writer used to send the output to file.
     */
    private final ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsPsmSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPsmFeature) {
                psmFeatures.add((PsPsmFeature) exportFeature);
            } else if (exportFeature instanceof PsIdentificationAlgorithmMatchesFeature) {
                identificationAlgorithmMatchesFeatures.add((PsIdentificationAlgorithmMatchesFeature) exportFeature);
            } else if (exportFeature instanceof PsFragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(psmFeatures);
        Collections.sort(identificationAlgorithmMatchesFeatures);
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new PsFragmentSection(fragmentFeatures, indexes, header, writer);
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
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param nSurroundingAA the number of surrounding amino acids to export
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
     * @throws org.apache.commons.math.MathException exception thrown if a math
     * exception occurred when estimating the noise level
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, ArrayList<String> keys,
            String linePrefix, int nSurroundingAA, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }


        int line = 1;
        int totalSize = identification.getNumber(SpectrumMatch.class);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(totalSize);
        }

        PSParameter psParameter = new PSParameter();

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            String spectrumKey = spectrumMatch.getKey();
            psParameter = (PSParameter)spectrumMatch.getUrParam(psParameter);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                if (decoys || peptideAssumption == null || !peptideAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {

                    boolean first = true;

                    if (indexes) {
                        if (linePrefix != null) {
                            writer.write(linePrefix);
                        }
                        writer.write(line + "");
                        first = false;
                    }
                    for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : identificationAlgorithmMatchesFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        String feature;
                        if (peptideAssumption != null) {
                            peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                            feature = PsIdentificationAlgorithmMatchesSection.getPeptideAssumptionFeature(identification, identificationFeaturesGenerator,
                                    identificationParameters, keys, linePrefix, nSurroundingAA, peptideAssumption, spectrumMatch.getKey(),
                                    psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                            feature = PsIdentificationAlgorithmMatchesSection.getTagAssumptionFeature(identification, identificationFeaturesGenerator,
                                    identificationParameters, keys, linePrefix, tagAssumption, spectrumMatch.getKey(), psParameter,
                                    identificationAlgorithmMatchesFeature, waitingHandler);
                        } else {
                            throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                        }
                        writer.write(feature);
                    }
                    for (PsPsmFeature psmFeature : psmFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        writer.write(getFeature(identification, identificationFeaturesGenerator, identificationParameters,
                                keys, linePrefix, spectrumMatch, psParameter, psmFeature, validatedOnly, decoys, waitingHandler));
                    }
                    writer.newLine();
                    if (fragmentSection != null) {
                        String fractionPrefix = "";
                        if (linePrefix != null) {
                            fractionPrefix += linePrefix;
                        }
                        fractionPrefix += line + ".";
                        writer.increaseDepth();
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            fragmentSection.writeSection(spectrumKey, spectrumMatch.getBestPeptideAssumption(), identificationParameters, fractionPrefix, null);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            fragmentSection.writeSection(spectrumKey, spectrumMatch.getBestTagAssumption(), identificationParameters, fractionPrefix, null);
                        }
                        writer.decreseDepth();
                    }
                    line++;
                }
            }
        }
    }

    /**
     * Writes the given feature of the current section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param spectrumMatch the spectrum match inspected
     * @param psParameter the PeptideShaker parameter of the match
     * @param psmFeature the feature to export
     * @param validatedOnly indicates whether only validated hits should be
     * exported
     * @param decoys indicates whether decoys should be included in the export
     * @param waitingHandler the waiting handler
     *
     * @return the content corresponding to the given feature of the current
     * section
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
     */
    public static String getFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, ArrayList<String> keys, String linePrefix,
            SpectrumMatch spectrumMatch, PSParameter psParameter, PsPsmFeature psmFeature, boolean validatedOnly, boolean decoys,
            WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        switch (psmFeature) {
            case protein_groups:
                ArrayList<String> accessions = spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                HashSet<String> proteinGroups = new HashSet<>(accessions.size());
                for (String accession : accessions) {
                    HashSet<String> groups = identification.getProteinMap().get(accession);
                    if (groups != null) {
                        proteinGroups.addAll(groups);
                    }
                }
                StringBuilder proteins = new StringBuilder();
                ArrayList<String> proteinGroupsList = new ArrayList<>(proteinGroups);
                Collections.sort(proteinGroupsList);
                if (proteinGroupsList.size() > 1) {
                    identification.loadObjects(proteinGroupsList, waitingHandler, false);
                }
                for (String proteinGroup : proteinGroupsList) {
                    if (identification.getProteinIdentification().contains(proteinGroup)) {
                        psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(proteinGroup)).getUrParam(psParameter);
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
                accessions = spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                proteinGroups = new HashSet<>(accessions.size());
                for (String accession : accessions) {
                    HashSet<String> groups = identification.getProteinMap().get(accession);
                    if (groups != null) {
                        proteinGroups.addAll(groups);
                    }
                }
                proteinGroupsList = new ArrayList<>(proteinGroups);
                Collections.sort(proteinGroupsList);
                if (proteinGroupsList.size() > 1) {
                    identification.loadObjects(proteinGroupsList, waitingHandler, false);
                }
                for (String proteinGroup : proteinGroupsList) {
                    if (identification.getProteinIdentification().contains(proteinGroup)) {
                        psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(proteinGroup)).getUrParam(psParameter);
                        if (psParameter.getMatchValidationLevel().getIndex() > bestProteinValidationLevel.getIndex()) {
                            bestProteinValidationLevel = psParameter.getMatchValidationLevel();
                        }
                    }
                }
                return bestProteinValidationLevel.getName();
            case probabilistic_score:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        StringBuilder result = new StringBuilder();
                        ArrayList<String> modList = new ArrayList<>(ptmScores.getScoredPTMs());
                        Collections.sort(modList);
                        for (String mod : modList) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                            ArrayList<Integer> sites = new ArrayList<>(ptmScoring.getProbabilisticSites());
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
                }
                return "";
            case d_score:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    StringBuilder result = new StringBuilder();
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        ArrayList<String> modList = new ArrayList<>(ptmScores.getScoredPTMs());
                        Collections.sort(modList);
                        for (String mod : modList) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                            ArrayList<Integer> sites = new ArrayList<>(ptmScoring.getDSites());
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
                    }
                    return result.toString();
                }
                return "";
            case localization_confidence:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        StringBuilder result = new StringBuilder();
                        ArrayList<String> modList = ptmScores.getScoredPTMs();
                        Collections.sort(modList);
                        for (String mod : modList) {

                            Modification ptm = ModificationFactory.getInstance().getModification(mod);

                            if (ptm.getType() == Modification.MODAA) {

                                if (result.length() > 0) {
                                    result.append(", ");
                                }
                                result.append(mod);

                                result.append(" (");
                                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                boolean firstSite = true;

                                ArrayList<Integer> sites = ptmScoring.getOrderedPtmLocations();
                                if (sites.isEmpty()) {
                                    result.append("Not Scored");
                                } else {
                                    for (int site : ptmScoring.getOrderedPtmLocations()) {

                                        if (firstSite) {
                                            firstSite = false;
                                        } else {
                                            result.append(", ");
                                        }
                                        int ptmConfidence = ptmScoring.getLocalizationConfidence(site);

                                        switch (ptmConfidence) {
                                            case PtmScoring.NOT_FOUND:
                                                result.append(site).append(": Not Scored");
                                                break;
                                            case PtmScoring.RANDOM:
                                                result.append(site).append(": Random");
                                                break;
                                            case PtmScoring.DOUBTFUL:
                                                result.append(site).append(": Doubtfull");
                                                break;
                                            case PtmScoring.CONFIDENT:
                                                result.append(site).append(": Confident");
                                                break;
                                            case PtmScoring.VERY_CONFIDENT:
                                                result.append(site).append(": Very Confident");
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }

                                result.append(")");
                            }
                        }
                        return result.toString();
                    }
                }
                return "";
            case algorithm_score:
                HashMap<Integer, PeptideAssumption> assumptionMap = new HashMap<>();
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = spectrumMatch.getAssumptionsMap();
                    for (Integer id : assumptionsMap.keySet()) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmAssumptions = assumptionsMap.get(id);
                        for (ArrayList<SpectrumIdentificationAssumption> assumptionsAtScore : algorithmAssumptions.values()) {
                            for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : assumptionsAtScore) {
                                if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                                    if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(
                                            spectrumMatch.getBestPeptideAssumption().getPeptide(), identificationParameters.getSequenceMatchingPreferences())) {
                                        double score = peptideAssumption.getScore();
                                        PeptideAssumption currentAssumption = assumptionMap.get(id);
                                        if (currentAssumption == null || score < currentAssumption.getScore()) {
                                            assumptionMap.put(id, peptideAssumption);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ArrayList<Integer> ids = new ArrayList<>(assumptionMap.keySet());
                Collections.sort(ids);
                StringBuilder result = new StringBuilder();
                for (int id : ids) {
                    if (result.length() != 0) {
                        result.append(", ");
                    }
                    PeptideAssumption currentAssumption = assumptionMap.get(id);
                    Double score = currentAssumption.getRawScore();
                    if (score == null) {
                        score = currentAssumption.getScore();
                    }
                    result.append(Advocate.getAdvocate(id).getName()).append(" (").append(score).append(")");
                }
                return result.toString();
            case confidence:
                return psParameter.getPsmConfidence() + "";
            case score:
                return psParameter.getPsmScore() + "";
            case raw_score:
                return psParameter.getPsmProbabilityScore() + "";
            case validated:
                return psParameter.getMatchValidationLevel().toString();
            case starred:
                if (psParameter.getStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case hidden:
                if (psParameter.getHidden()) {
                    return "1";
                } else {
                    return "0";
                }
            case confident_modification_sites:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    return identificationFeaturesGenerator.getConfidentPtmSites(spectrumMatch, sequence);
                }
                return "";
            case confident_modification_sites_number:
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(spectrumMatch);
            case ambiguous_modification_sites:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    return identificationFeaturesGenerator.getAmbiguousPtmSites(spectrumMatch, sequence);
                }
                return "";
            case ambiguous_modification_sites_number:
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(spectrumMatch);
            case confident_phosphosites:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    ArrayList<String> modifications = new ArrayList<>();
                    for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                        if (ptm.toLowerCase().contains("phospho")) {
                            modifications.add(ptm);
                        }
                    }
                    return identificationFeaturesGenerator.getConfidentPtmSites(spectrumMatch, sequence, modifications);
                }
                return "";
            case confident_phosphosites_number:
                ArrayList<String> modifications = new ArrayList<>();
                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    if (ptm.toLowerCase().contains("phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(spectrumMatch, modifications);
            case ambiguous_phosphosites:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                    modifications = new ArrayList<>();
                    for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                        if (ptm.toLowerCase().contains("phospho")) {
                            modifications.add(ptm);
                        }
                    }
                    return identificationFeaturesGenerator.getAmbiguousPtmSites(spectrumMatch, sequence, modifications);
                }
                return "";
            case ambiguous_phosphosites_number:
                modifications = new ArrayList<>();
                for (String ptm : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
                    if (ptm.toLowerCase().contains("phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(spectrumMatch, modifications);
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the header of this section.
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
        for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        for (ExportFeature exportFeature : psmFeatures) {
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
