package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneFactory;
import com.compomics.util.experiment.biology.genes.go.GoMapping;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.protein.Header;
import eu.isas.peptideshaker.filtering.items.ProteinFilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Protein filter.
 *
 * @author Marc Vaudel
 */
public class ProteinFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 5753850468907866679L;

    /**
     * Constructor.
     */
    public ProteinFilter() {

    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     */
    public ProteinFilter(String name) {
        this.name = name;
        this.filterType = FilterType.PROTEIN;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     */
    public ProteinFilter(String name, String description) {
        this.name = name;
        this.description = description;
        this.filterType = FilterType.PROTEIN;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     * @param condition a description of the condition to be met to pass the
     * filter
     * @param reportPassed a report for when the filter is passed
     * @param reportFailed a report for when the filter is not passed
     */
    public ProteinFilter(String name, String description, String condition, String reportPassed, String reportFailed) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.reportPassed = reportPassed;
        this.reportFailed = reportFailed;
        this.filterType = FilterType.PROTEIN;
    }

    @Override
    protected MatchFilter getNew() {
        return new ProteinFilter();
    }

    @Override
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String matchKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        ProteinFilterItem filterItem = ProteinFilterItem.getItem(itemName);
        if (filterItem == null) {
            throw new IllegalArgumentException("Filter item " + itemName + "not recognized as protein filter item.");
        }
        String input = value.toString();
        switch (filterItem) {
            case proteinAccession:
                return filterItemComparator.passes(input, ProteinMatch.getAccessions(matchKey));
            case proteinDescription:
                String[] accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> descriptions = new ArrayList<>();
                for (String accession : accessions) {
                    Header proteinHeader = SequenceFactory.getInstance().getHeader(accession);
                    descriptions.add(proteinHeader.getDescription());
                }
                return filterItemComparator.passes(input, descriptions);
            case sequence:
                accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> sequences = new ArrayList<>();
                for (String accession : accessions) {
                    Protein protein = SequenceFactory.getInstance().getProtein(accession);
                    sequences.add(protein.getSequence());
                }
                return filterItemComparator.passes(input, sequences);
            case chromosome:
                accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> chromosomes = new ArrayList<>();
                for (String accession : accessions) {
                    String geneName = SequenceFactory.getInstance().getHeader(accession).getGeneName();
                    String chromosomeNumber = geneMaps.getChromosome(geneName);
                    chromosomes.add(chromosomeNumber);
                }
                return filterItemComparator.passes(input, chromosomes);
            case gene:
                accessions = ProteinMatch.getAccessions(matchKey);
                ArrayList<String> genes = new ArrayList<>();
                for (String accession : accessions) {
                    String geneName = SequenceFactory.getInstance().getHeader(accession).getGeneName();
                    genes.add(geneName);
                }
                return filterItemComparator.passes(input, genes);
            case GO:
                return filterItemComparator.passes(input, new ArrayList<>(geneMaps.getGoNamesForProtein(matchKey)));
            case expectedCoverage:
                Double coverage = 100 * identificationFeaturesGenerator.getObservableCoverage(matchKey);
                return filterItemComparator.passes(input, coverage.toString());
            case validatedCoverage:
                coverage = 100 * identificationFeaturesGenerator.getValidatedSequenceCoverage(matchKey);
                return filterItemComparator.passes(input, coverage.toString());
            case confidentCoverage:
                HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(matchKey);
                coverage = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return filterItemComparator.passes(input, coverage.toString());
            case spectrumCounting:
                sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(matchKey);
                coverage = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                return filterItemComparator.passes(input, coverage.toString());
            case ptm:
                ProteinMatch proteinMatch = (ProteinMatch)identification.retrieveObject(matchKey);
                ArrayList<String> ptms;
                PSPtmScores psPtmScores = new PSPtmScores();
                psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
                if (psPtmScores != null) {
                    ptms = psPtmScores.getScoredPTMs();
                } else {
                    ptms = new ArrayList<>(0);
                }
                return filterItemComparator.passes(input, ptms);
            case nPeptides:
                proteinMatch = (ProteinMatch)identification.retrieveObject(matchKey);
                Integer nPeptides = proteinMatch.getPeptideCount();
                return filterItemComparator.passes(input, nPeptides.toString());
            case nValidatedPeptides:
                nPeptides = identificationFeaturesGenerator.getNValidatedPeptides(matchKey);
                return filterItemComparator.passes(input, nPeptides.toString());
            case nConfidentPeptides:
                nPeptides = identificationFeaturesGenerator.getNConfidentPeptides(matchKey);
                return filterItemComparator.passes(input, nPeptides.toString());
            case nPSMs:
                Integer nPsms = identificationFeaturesGenerator.getNSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case nValidatedPSMs:
                nPsms = identificationFeaturesGenerator.getNValidatedSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case nConfidentPSMs:
                nPsms = identificationFeaturesGenerator.getNConfidentSpectra(matchKey);
                return filterItemComparator.passes(input, nPsms.toString());
            case confidence:
                PSParameter psParameter = new PSParameter();
                psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(matchKey)).getUrParam(psParameter);
                Double confidence = psParameter.getProteinConfidence();
                return filterItemComparator.passes(input, confidence.toString());
            case proteinInference:
                psParameter = new PSParameter();
                psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(matchKey)).getUrParam(psParameter);
                Integer pi = psParameter.getProteinInferenceGroupClass();
                return filterItemComparator.passes(input, pi.toString());
            case validationStatus:
                psParameter = new PSParameter();
                psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(matchKey)).getUrParam(psParameter);
                Integer validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation.toString());
            case stared:
                psParameter = new PSParameter();
                psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(matchKey)).getUrParam(psParameter);
                String starred;
                if (psParameter.getStarred()) {
                    starred = FilterItemComparator.trueFalse[0];
                } else {
                    starred = FilterItemComparator.trueFalse[1];
                }
                return filterItemComparator.passes(input, starred);
            default:
                throw new IllegalArgumentException("Protein filter not implemented for item " + filterItem.name + ".");
        }
    }

    @Override
    public FilterItem[] getPossibleFilterItems() {
        return ProteinFilterItem.values();
    }

    @Override
    public FilterItem getFilterItem(String itemName) {
        return ProteinFilterItem.getItem(itemName);
    }
}
