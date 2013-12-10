package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeatures;
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
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
     * The protein features to export.
     */
    private ArrayList<ExportFeature> proteinFeatures = new ArrayList<ExportFeature>();
    /**
     * The peptide subsection if any.
     */
    private PeptideSection peptideSection = null;
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
     * @param exportFeatures the features to export in this section.
     * ProteinFeatures as main features. If Peptide or protein features are
     * selected, they will be added as sub-sections.
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public ProteinSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> peptideFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof ProteinFeatures) {
                proteinFeatures.add(exportFeature);
            } else if (exportFeature instanceof PeptideFeatures || exportFeature instanceof PsmFeatures) {
                peptideFeatures.add(exportFeature);
            }
        }
        if (!peptideFeatures.isEmpty()) {
            peptideSection = new PeptideSection(peptideFeatures, separator, indexes, header, writer);
        }
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
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the protein matches to output. if null all
     * proteins will be exported.
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino-acids, the number of surrounding amino acids to use
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, int nSurroundingAas, WaitingHandler waitingHandler) 
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        if (keys == null) {
            keys = identification.getProteinIdentification();
        }

        PSParameter psParameter = new PSParameter();
        ProteinMatch proteinMatch = null;
        String matchKey = "", parameterKey = "";
        int line = 1;

        if (peptideSection != null) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Loading Peptides. Please Wait...");
                waitingHandler.resetSecondaryProgressCounter();
            }
            identification.loadPeptideMatches(waitingHandler);
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Loading Peptide Details. Please Wait...");
                waitingHandler.resetSecondaryProgressCounter();
            }
            identification.loadPeptideMatchParameters(psParameter, waitingHandler);
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Proteins. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadProteinMatches(keys, waitingHandler);
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Protein Details. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadProteinMatchParameters(keys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        for (String proteinKey : keys) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            if (indexes) {
                writer.write(line + separator); // @TODO: there is something off with the indexes in the Default PSM Report
            }
            for (ExportFeature exportFeature : proteinFeatures) {
                ProteinFeatures tempProteinFeatures = (ProteinFeatures) exportFeature;
                switch (tempProteinFeatures) {
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
                    case ensembl_gene_id:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        if (!identification.getProteinMatch(proteinKey).isDecoy()) {
                            GeneFactory geneFactory = GeneFactory.getInstance();
                            String geneName = geneFactory.getGeneNameForUniProtProtein(proteinMatch.getMainMatch());
                            if (geneName != null) {
                                String ensemblId = geneFactory.getGeneEnsemblId(geneName);
                                if (ensemblId != null) {
                                    writer.write(ensemblId);
                                }
                            }
                        }
                        writer.write(separator);
                        break;
                    case gene_name:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        if (!identification.getProteinMatch(proteinKey).isDecoy()) {
                            GeneFactory geneFactory = GeneFactory.getInstance();
                            String geneName = geneFactory.getGeneNameForUniProtProtein(proteinMatch.getMainMatch());
                            if (geneName != null) {
                                writer.write(geneName);
                            }
                        }
                        writer.write(separator);
                        break;
                    case chromosome:
                        if (!matchKey.equals(proteinKey)) {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            matchKey = proteinKey;
                        }
                        if (!identification.getProteinMatch(proteinKey).isDecoy()) {
                            GeneFactory geneFactory = GeneFactory.getInstance();
                            String geneName = geneFactory.getGeneNameForUniProtProtein(proteinMatch.getMainMatch());
                            if (geneName != null) {
                                String chromosome = geneFactory.getChromosomeForGeneName(geneName);
                                if (chromosome != null) {
                                    writer.write(chromosome);
                                }
                            }
                        }
                        writer.write(separator);
                        break;
                    case go_accession:
                        if (!identification.getProteinMatch(proteinKey).isDecoy()) {
                            ArrayList<String> goTermaccessions = GOFactory.getInstance().getProteinGoAccessions(proteinKey);
                            if (goTermaccessions != null) {
                                boolean first = true;
                                for (String accession : goTermaccessions) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(accession);
                                }
                            }
                        }
                        writer.write(separator);
                        break;
                    case go_description:
                        if (!identification.getProteinMatch(proteinKey).isDecoy()) {
                            ArrayList<String> goTermDescriptions = GOFactory.getInstance().getProteinGoDescriptions(proteinKey);
                            if (goTermDescriptions != null) {
                                boolean first = true;
                                for (String description : goTermDescriptions) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(description);
                                }
                            }
                        }
                        writer.write(separator);
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
                        writer.write(identificationFeaturesGenerator.getPrimaryPTMSummary(proteinKey, separator));
                        break;
                    case other_PTMs:
                        writer.write(identificationFeaturesGenerator.getSecondaryPTMSummary(proteinKey, separator));
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
                        Double result = 100 * identificationFeaturesGenerator.getSequenceCoverage(proteinKey, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
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
                            writer.write(psParameter.getMatchValidationLevel() + separator);
                        break;
                    default:
                        writer.write("Not implemented");
                }
            }
            writer.newLine();
            if (peptideSection != null) {
                peptideSection.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, proteinMatch.getPeptideMatches(), nSurroundingAas, line + ".", null);
            }
            line++;
        }
    }

    /**
     * Writes the header of the protein section.
     *
     * @throws IOException
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.write(separator);
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : proteinFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.write(separator);
            }
            writer.write(exportFeature.getTitle());
        }
        writer.newLine();
    }
}
