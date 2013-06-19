package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class outputs the protein related export features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinSection {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The features to export.
     */
    private ArrayList<ExportFeature> exportFeatures;
    /**
     * The separator used to separate columns.
     */
    private String separator;
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
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public ProteinSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
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
     * @param searchParameters the search parameters of the project
     * @param keys the keys of the protein matches to output
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, ArrayList<String> keys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressDialogIndeterminate(true);
        }

        if (header) {
            if (indexes) {
                writer.write(separator);
            }
            boolean firstColumn = true;
            for (ExportFeature exportFeature : exportFeatures) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(exportFeature.getTitle());
            }
            writer.newLine();
        }

        if (keys == null) {
            keys = identification.getProteinIdentification();
        }

        PSParameter psParameter = new PSParameter();
        ProteinMatch proteinMatch = null;
        String matchKey = "", parameterKey = "";
        int line = 1;

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Proteins. Please Wait...");
            waitingHandler.resetSecondaryProgressBar();
        }
        identification.loadProteinMatches(keys, waitingHandler);
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Protein Details. Please Wait...");
            waitingHandler.resetSecondaryProgressBar();
        }
        identification.loadProteinMatchParameters(keys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressBar();
            waitingHandler.setMaxSecondaryProgressValue(keys.size());
        }

        for (String proteinKey : keys) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressValue();
            }

            if (indexes) {
                writer.write(line + separator);
            }
            for (ExportFeature exportFeature : exportFeatures) {
                ProteinFeatures proteinFeatures = (ProteinFeatures) exportFeature;
                switch (proteinFeatures) {
                    case accession:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        writer.write(proteinMatch.getMainMatch() + separator);
                        break;
                    case protein_description:
                        writer.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription() + separator);
                        break;
                    case other_proteins:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        String mainAccession = proteinMatch.getMainMatch();
                        StringBuilder otherProteins = new StringBuilder();
                        List<String> otherAccessions = Arrays.asList(ProteinMatch.getAccessions(matchKey));
                        Collections.sort(otherAccessions);
                        for (String accession : otherAccessions) {
                            if (!accession.equals(mainAccession)) {
                                if (otherProteins.length() > 0) {
                                    otherProteins.append(", ");
                                }
                                otherProteins.append(accession);
                            }
                        }

                        writer.write(otherProteins.toString() + separator);
                        break;
                    case protein_group:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        StringBuilder completeProteinGroup = new StringBuilder();
                        List<String> allAccessions = Arrays.asList(ProteinMatch.getAccessions(matchKey));
                        Collections.sort(allAccessions);
                        for (String accession : allAccessions) {
                            if (completeProteinGroup.length() > 0) {
                                completeProteinGroup.append(", ");
                            }
                            completeProteinGroup.append(accession);
                        }

                        writer.write(completeProteinGroup.toString() + separator);
                        break;
                    case confidence:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        writer.write(psParameter.getProteinConfidence() + separator);
                        break;
                    case confident_PTMs:
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey, separator) + separator);
                        break;
                    case other_PTMs:
                        writer.write(identificationFeaturesGenerator.getSecondaryPTMSummary(proteinKey, separator) + separator);
                        break;
                    case confident_phosphosites:
                        ArrayList<String> modifications = new ArrayList<String>();
                        for (String ptm : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                            if (ptm.contains("phospho")) {
                                modifications.add(ptm);
                            }
                        }
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey, modifications, separator) + separator);
                        break;
                    case other_phosphosites:
                        modifications = new ArrayList<String>();
                        for (String ptm : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                            if (ptm.contains("phospho")) {
                                modifications.add(ptm);
                            }
                        }
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey, modifications, separator) + separator);
                        break;
                    case coverage:
                        Double result = 100 * identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                        writer.write(Util.roundDouble(result, 2) + separator);
                        break;
                    case possible_coverage:
                        result = 100 * identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                        writer.write(Util.roundDouble(result, 2) + separator);
                        break;
                    case decoy:
                        if (ProteinMatch.isDecoy(proteinKey)) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                        break;
                    case hidden:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        if (psParameter.isHidden()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                        break;
                    case mw:
                        Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                        writer.write(proteinMW + separator);
                        break;
                    case non_enzymatic:
                        ArrayList<String> nonEnzymatic = identificationFeaturesGenerator.getNonEnzymatic(proteinKey, searchParameters.getEnzyme());
                        writer.write(nonEnzymatic.size() + separator);
                        break;
                    case pi:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        writer.write(psParameter.getProteinInferenceClassAsString() + separator);
                        break;
                    case peptides:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        writer.write(proteinMatch.getPeptideCount() + separator);
                        break;
                    case psms:
                        int nHits = identificationFeaturesGenerator.getNSpectra(proteinKey);
                        writer.write(nHits + separator);
                        break;
                    case validated_peptides:
                        nHits = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
                        writer.write(nHits + separator);
                        break;
                    case unique_peptides:
                        nHits = identificationFeaturesGenerator.getNUniquePeptides(proteinKey);
                        writer.write(nHits + separator);
                        break;
                    case validated_psms:
                        nHits = identificationFeaturesGenerator.getNValidatedSpectra(proteinKey);
                        writer.write(nHits + separator);
                        break;
                    case score:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        writer.write(psParameter.getProteinProbabilityScore() + separator);
                        break;
                    case spectrum_counting_nsaf:
                        try {
                            writer.write(identificationFeaturesGenerator.getSpectrumCounting(proteinKey,
                                    SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + separator);
                        } catch (Exception e) {
                            writer.write("error: " + e.getLocalizedMessage() + separator);
                        }
                        break;
                    case spectrum_counting_empai:
                        try {
                            writer.write(identificationFeaturesGenerator.getSpectrumCounting(proteinKey,
                                    SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + separator);
                        } catch (Exception e) {
                            writer.write("error: " + e.getLocalizedMessage() + separator);
                        }
                        break;
                    case starred:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        if (psParameter.isStarred()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                        break;
                    case validated:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        if (psParameter.isValidated()) {
                            writer.write(1 + separator);
                        } else {
                            writer.write(0 + separator);
                        }
                        break;
                    default:
                        writer.write("Not implemented");
                }
            }
            writer.newLine();
            line++;
        }
    }
}
