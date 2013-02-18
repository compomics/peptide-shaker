/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.export.section_generators;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures;
import eu.isas.peptideshaker.export.exportfeatures.SpectrumCountingFeatures;
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
 * This class outputs the protein related export features
 *
 * @author Marc
 */
public class ProteinSection {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The features to export
     */
    private ArrayList<ExportFeature> exportFeatures;
    /**
     * The separator used to separate columns
     */
    private String separator;
    /**
     * Boolean indicating whether the line shall be indexed
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;

    /**
     * constructor
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
     * Writes the desired section
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param keys the keys of the protein matches to output
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, SearchParameters searchParameters, ArrayList<String> keys) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {
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
        PSParameter psParameter = new PSParameter();
        ProteinMatch proteinMatch = null;
        String matchKey = "", parameterKey = "";
        int line = 1;
        for (String proteinKey : keys) {
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
                        writer.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription() + separator);
                        break;
                    case other_proteins:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        String mainAccession = proteinMatch.getMainMatch();
                        String proteins = "";
                        List<String> accessions = Arrays.asList(ProteinMatch.getAccessions(matchKey));
                        Collections.sort(accessions);
                        for (String accession : accessions) {
                            if (!accession.equals(mainAccession)) {
                                if (!proteins.equals("")) {
                                    proteins += ", ";
                                }
                                proteins += accession;
                            }
                        }
                        writer.write(proteins + separator);
                        break;
                    case confidence:
                        if (!parameterKey.equals(proteinKey)) {
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                            parameterKey = proteinKey;
                        }
                        writer.write(psParameter.getProteinConfidence() + separator);
                        break;
                    case confident_PTMs:
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey) + separator);
                        break;
                    case other_PTMs:
                        writer.write(identificationFeaturesGenerator.getSecondaryPTMSummary(proteinKey) + separator);
                        break;
                    case confident_phosphosites:
                        ArrayList<String> modifications = new ArrayList<String>();
                        for (String ptm : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                            if (ptm.contains("phospho")) {
                                modifications.add(ptm);
                            }
                        }
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey, modifications) + separator);
                        break;
                    case other_phosphosites:
                        modifications = new ArrayList<String>();
                        for (String ptm : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                            if (ptm.contains("phospho")) {
                                modifications.add(ptm);
                            }
                        }
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey, modifications) + separator);
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
                        writer.write(psParameter.getProteinInferenceClassAsString());
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
                    case spectrum_counting:
                        result = identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                        writer.write(result + separator);
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
