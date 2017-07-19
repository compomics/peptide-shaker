package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.genes.GeneFactory;
import com.compomics.util.experiment.biology.genes.go.GoMapping;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.units.MetricsPrefix;
import com.compomics.util.experiment.units.StandardUnit;
import com.compomics.util.experiment.units.UnitOfMeasurement;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsProteinFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
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
import org.apache.commons.math.util.FastMath;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the protein related export features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsProteinSection {

    /**
     * The protein features to export.
     */
    private ArrayList<PsProteinFeature> proteinFeatures = new ArrayList<PsProteinFeature>();
    /**
     * The peptide subsection if any.
     */
    private PsPeptideSection peptideSection = null;
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
     * @param exportFeatures the features to export in this section.
     * ProteinFeatures as main features. If Peptide or protein features are
     * selected, they will be added as sub-sections.
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsProteinSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> peptideFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsProteinFeature) {
                proteinFeatures.add((PsProteinFeature) exportFeature);
            } else if (exportFeature instanceof PsPeptideFeature || exportFeature instanceof PsPsmFeature || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature || exportFeature instanceof PsFragmentFeature) {
                peptideFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(proteinFeatures);
        if (!peptideFeatures.isEmpty()) {
            peptideSection = new PsPeptideSection(peptideFeatures, indexes, header, writer);
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
     * @param geneMaps the gene maps
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output. if null all
     * proteins will be exported.
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino-acids, the number of surrounding amino acids to use
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
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error is encountered while calculating the observable coverage
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, ArrayList<String> keys,
            int nSurroundingAas, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        if (keys == null) {
            keys = new ArrayList<String>(identification.getProteinIdentification());
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
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(keys, waitingHandler); // @TODO: find a better way to know if we need psms

        while (proteinMatchesIterator.hasNext()) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();

            if (decoys || !ProteinMatch.isDecoy(proteinKey)) {
                psParameter = (PSParameter)proteinMatch.getParameters();

                if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                    boolean first = true;

                    if (indexes) {
                        writer.write(line + "");
                        first = false;
                    }

                    for (ExportFeature exportFeature : proteinFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        PsProteinFeature tempProteinFeatures = (PsProteinFeature) exportFeature;
                        writer.write(getFeature(identificationFeaturesGenerator, geneMaps, identificationParameters, keys, nSurroundingAas, proteinKey, proteinMatch, psParameter, tempProteinFeatures, waitingHandler));
                    }
                    writer.newLine();
                    if (peptideSection != null) {
                        writer.increaseDepth();
                        if (waitingHandler != null) {
                            waitingHandler.setDisplayProgress(false);
                        }
                        peptideSection.writeSection(identification, identificationFeaturesGenerator, identificationParameters, proteinMatch.getPeptideMatchesKeys(), nSurroundingAas, line + ".", validatedOnly, decoys, waitingHandler);
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
     * Returns the part of the desired section.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param geneMaps the gene maps
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output. if null all
     * proteins will be exported.
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino-acids, the number of surrounding amino acids to use
     * @param proteinKey the key of the protein match being written
     * @param proteinMatch the protein match, can be null if not needed
     * @param psParameter the protein match parameter containing the
     * PeptideShaker parameters, can be null if not needed
     * @param tempProteinFeatures the protein feature to write
     * @param waitingHandler the waiting handler
     *
     * @return the string to write
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
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error is encountered while calculating the observable coverage
     */
    public static String getFeature(IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, ArrayList<String> keys, int nSurroundingAas, String proteinKey, ProteinMatch proteinMatch, PSParameter psParameter, PsProteinFeature tempProteinFeatures, WaitingHandler waitingHandler)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        switch (tempProteinFeatures) {
            case accession:
                return proteinMatch.getMainMatch();
            case protein_description:
                return SequenceFactory.getInstance().getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription();
            case ensembl_gene_id:
                if (!proteinMatch.isDecoy()) {
                    String geneName = geneMaps.getGeneNameForProtein(proteinMatch.getMainMatch());
                    if (geneName != null) {
                        String ensemblId = geneMaps.getEnsemblId(geneName);
                        if (ensemblId != null) {
                            return ensemblId;
                        }
                    }
                }
                return "";
            case gene_name:
                if (!proteinMatch.isDecoy()) {
                    String geneName = geneMaps.getGeneNameForProtein(proteinMatch.getMainMatch());
                    if (geneName != null) {
                        return geneName;
                    }
                }
                return "";
            case chromosome:
                if (!proteinMatch.isDecoy()) {
                    String geneName = geneMaps.getGeneNameForProtein(proteinMatch.getMainMatch());
                    if (geneName != null) {
                        String chromosome = geneMaps.getChromosome(geneName);
                        if (chromosome != null) {
                            return chromosome;
                        }
                    }
                }
                return "";
            case go_accession:
                StringBuilder result = new StringBuilder();
                if (!proteinMatch.isDecoy()) {
                    HashSet<String> goTermaccessions = geneMaps.getGoTermsForProtein(proteinKey);
                    if (goTermaccessions != null) {
                        for (String accession : goTermaccessions) {
                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(accession);
                        }
                    }
                }
                return result.toString();
            case go_description:
                result = new StringBuilder();
                if (!proteinMatch.isDecoy()) {
                    HashSet<String> goTermDescriptions = geneMaps.getGoNamesForProtein(proteinKey);
                    if (goTermDescriptions != null) {
                        for (String description : goTermDescriptions) {
                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(description);
                        }
                    }
                }
                return result.toString();
            case other_proteins:
                String mainAccession = proteinMatch.getMainMatch();
                result = new StringBuilder();
                List<String> otherAccessions = Arrays.asList(ProteinMatch.getAccessions(proteinKey));
                Collections.sort(otherAccessions);
                for (String accession : otherAccessions) {
                    if (!accession.equals(mainAccession)) {
                        if (result.length() > 0) {
                            result.append(", ");
                        }
                        result.append(accession);
                    }
                }
                return result.toString();
            case protein_group:
                StringBuilder completeProteinGroup = new StringBuilder();
                List<String> allAccessions = Arrays.asList(ProteinMatch.getAccessions(proteinKey));
                Collections.sort(allAccessions);
                for (String accession : allAccessions) {
                    if (completeProteinGroup.length() > 0) {
                        completeProteinGroup.append(", ");
                    }
                    completeProteinGroup.append(accession);
                }
                return completeProteinGroup.toString();
            case descriptions:
                StringBuilder descriptions = new StringBuilder();
                allAccessions = Arrays.asList(ProteinMatch.getAccessions(proteinKey));
                Collections.sort(allAccessions);
                for (String accession : allAccessions) {
                    if (descriptions.length() > 0) {
                        descriptions.append(", ");
                    }
                    descriptions.append(SequenceFactory.getInstance().getHeader(accession).getSimpleProteinDescription());
                }
                return descriptions.toString();
            case confidence:
                return psParameter.getProteinConfidence() + "";
            case confident_modification_sites:
                mainAccession = proteinMatch.getMainMatch();
                Protein protein = SequenceFactory.getInstance().getProtein(mainAccession);
                String sequence = protein.getSequence();
                return identificationFeaturesGenerator.getConfidentPtmSites(proteinMatch, sequence);
            case confident_modification_sites_number:
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(proteinMatch);
            case ambiguous_modification_sites:
                mainAccession = proteinMatch.getMainMatch();
                protein = SequenceFactory.getInstance().getProtein(mainAccession);
                sequence = protein.getSequence();
                return identificationFeaturesGenerator.getAmbiguousPtmSites(proteinMatch, sequence);
            case ambiguous_modification_sites_number:
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(proteinMatch);
            case confident_phosphosites:
                ArrayList<String> modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                mainAccession = proteinMatch.getMainMatch();
                protein = SequenceFactory.getInstance().getProtein(mainAccession);
                sequence = protein.getSequence();
                return identificationFeaturesGenerator.getConfidentPtmSites(proteinMatch, sequence, modifications);
            case confident_phosphosites_number:
                modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getConfidentPtmSitesNumber(proteinMatch, modifications);
            case ambiguous_phosphosites:
                mainAccession = proteinMatch.getMainMatch();
                protein = SequenceFactory.getInstance().getProtein(mainAccession);
                sequence = protein.getSequence();
                modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSites(proteinMatch, sequence, modifications);
            case ambiguous_phosphosites_number:
                modifications = new ArrayList<String>();
                for (String ptm : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
                    if (ptm.contains("Phospho")) {
                        modifications.add(ptm);
                    }
                }
                return identificationFeaturesGenerator.getAmbiguousPtmSiteNumber(proteinMatch, modifications);
            case possible_coverage:
                Double value = 100 * identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                return Util.roundDouble(value, 2) + "";
            case coverage:
                value = 100 * identificationFeaturesGenerator.getValidatedSequenceCoverage(proteinKey);
                return Util.roundDouble(value, 2) + "";
            case confident_coverage:
                HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                value = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return Util.roundDouble(value, 2) + "";
            case decoy:
                if (ProteinMatch.isDecoy(proteinKey)) {
                    return 1 + "";
                } else {
                    return 0 + "";
                }
            case hidden:
                if (psParameter.getHidden()) {
                    return 1 + "";
                } else {
                    return 0 + "";
                }
            case mw:
                Double proteinMW = SequenceFactory.getInstance().computeMolecularWeight(proteinMatch.getMainMatch());
                return proteinMW.toString();
            case non_enzymatic:
                ArrayList<String> nonEnzymatic = identificationFeaturesGenerator.getNonEnzymatic(proteinKey, identificationParameters.getSearchParameters().getDigestionPreferences());
                return nonEnzymatic.size() + "";
            case pi:
                return psParameter.getProteinInferenceClassAsString();
            case peptides:
                return proteinMatch.getPeptideCount() + "";
            case psms:
                int nHits = identificationFeaturesGenerator.getNSpectra(proteinKey);
                return nHits + "";
            case validated_peptides:
                nHits = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
                return nHits + "";
            case unique_peptides:
                nHits = identificationFeaturesGenerator.getNUniquePeptides(proteinKey);
                return nHits + "";
            case unique_validated_peptides:
                nHits = identificationFeaturesGenerator.getNUniqueValidatedPeptides(proteinKey);
                return nHits + "";
            case unique_peptides_group:
                nHits = identificationFeaturesGenerator.getNUniquePeptidesGroup(proteinKey);
                return nHits + "";
            case unique_validated_peptides_group:
                nHits = identificationFeaturesGenerator.getNUniqueValidatedPeptidesGroup(proteinKey);
                return nHits + "";
            case validated_psms:
                nHits = identificationFeaturesGenerator.getNValidatedSpectra(proteinKey);
                return nHits + "";
            case score:
                return -10 * FastMath.log10(psParameter.getProteinProbabilityScore()) + "";
            case raw_score:
                return psParameter.getProteinProbabilityScore() + "";
            case spectrum_counting:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey) + "";
            case spectrum_counting_nsaf:
                return identificationFeaturesGenerator.getSpectrumCounting(proteinKey,
                        SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + "";
            case spectrum_counting_empai:
                return identificationFeaturesGenerator.getSpectrumCounting(proteinKey,
                        SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + "";
            case spectrum_counting_empai_percent:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey, new UnitOfMeasurement(StandardUnit.percentage), SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + "";
            case spectrum_counting_nsaf_percent:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey, new UnitOfMeasurement(StandardUnit.percentage), SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + "";
            case spectrum_counting_empai_ppm:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey, new UnitOfMeasurement(StandardUnit.ppm), SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + "";
            case spectrum_counting_nsaf_ppm:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey, new UnitOfMeasurement(StandardUnit.ppm), SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + "";
            case spectrum_counting_empai_fmol:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey, new UnitOfMeasurement(StandardUnit.mol, MetricsPrefix.femto), SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + "";
            case spectrum_counting_nsaf_fmol:
                return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey, new UnitOfMeasurement(StandardUnit.mol, MetricsPrefix.femto), SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + "";
            case starred:
                if (psParameter.getStarred()) {
                    return 1 + "";
                } else {
                    return 0 + "";
                }
            case validated:
                result = new StringBuilder();
                result.append(psParameter.getMatchValidationLevel().toString());
                return result.toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the header of the protein section.
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
        for (ExportFeature exportFeature : proteinFeatures) {
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
