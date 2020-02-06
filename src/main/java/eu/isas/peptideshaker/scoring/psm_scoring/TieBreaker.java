package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class breaks ties between equally scoring peptides.
 *
 * @author Marc Vaudel
 */
public class TieBreaker {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    private final HashMap<String, Integer> proteinCount;
    private final SearchParameters searchParameters;
    private final AnnotationParameters annotationParameters;
    private final ModificationParameters modificationParameters;
    private final SequenceMatchingParameters modificationSequenceMatchingParameters;
    /**
     * The sequence provider
     */
    private final SequenceProvider sequenceProvider;
    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator;

    public TieBreaker(
            HashMap<String, Integer> proteinCount,
            IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            SequenceProvider sequenceProvider
    ) {
        this.proteinCount = proteinCount;
        this.annotationParameters = identificationParameters.getAnnotationParameters();
        this.searchParameters = identificationParameters.getSearchParameters();
        this.modificationParameters = searchParameters.getModificationParameters();
        this.modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        this.peptideSpectrumAnnotator = peptideSpectrumAnnotator;
        this.sequenceProvider = sequenceProvider;
    }

    public PeptideAssumption getBestPeptideAssumption(
            String spectrumKey,
            PeptideAssumption peptideAssumption1,
            PeptideAssumption peptideAssumption2
    ) {

        int proteinMaxOccurrence1 = peptideAssumption1.getPeptide().getProteinMapping().navigableKeySet().stream()
                .filter(accession -> proteinCount.containsKey(accession))
                .mapToInt(accession -> proteinCount.get(accession))
                .max()
                .orElse(1);
        int proteinMaxOccurrence2 = peptideAssumption1.getPeptide().getProteinMapping().navigableKeySet().stream()
                .filter(accession -> proteinCount.containsKey(accession))
                .mapToInt(accession -> proteinCount.get(accession))
                .max()
                .orElse(1);

        if (proteinMaxOccurrence1 > proteinMaxOccurrence2) {

            return peptideAssumption1;

        } else if (proteinMaxOccurrence1 < proteinMaxOccurrence2) {

            return peptideAssumption2;

        }

        Spectrum spectrum = (Spectrum) spectrumFactory.getSpectrum(spectrumKey);

        int nCoveredAminoAcids1 = nCoveredAminoAcids(peptideAssumption1, spectrum);
        int nCoveredAminoAcids2 = nCoveredAminoAcids(peptideAssumption2, spectrum);

        if (nCoveredAminoAcids1 > nCoveredAminoAcids2) {

            return peptideAssumption1;

        } else if (nCoveredAminoAcids1 < nCoveredAminoAcids2) {

            return peptideAssumption2;

        }

        double massError1 = Math.abs(
                peptideAssumption1.getDeltaMass(
                        spectrum.getPrecursor().getMz(),
                        searchParameters.isPrecursorAccuracyTypePpm(),
                        searchParameters.getMinIsotopicCorrection(),
                        searchParameters.getMaxIsotopicCorrection()
                ));

        double massError2 = Math.abs(
                peptideAssumption2.getDeltaMass(
                        spectrum.getPrecursor().getMz(),
                        searchParameters.isPrecursorAccuracyTypePpm(),
                        searchParameters.getMinIsotopicCorrection(),
                        searchParameters.getMaxIsotopicCorrection()
                ));

        if (massError1 < massError2) {

            return peptideAssumption1;

        } else if (massError1 > massError2) {

            return peptideAssumption2;

        }

        throw new IllegalArgumentException("Tie during best match selection in spectrum " + spectrumKey + "(" + peptideAssumption1.getPeptide().getSequence() + " vs. " + peptideAssumption2.getPeptide().getSequence() + ".");

    }

    private int nCoveredAminoAcids(
            PeptideAssumption peptideAssumption,
            Spectrum spectrum
    ) {

        Peptide peptide1 = peptideAssumption.getPeptide();
        SpecificAnnotationParameters specificAnnotationPreferences = annotationParameters.getSpecificAnnotationParameters(
                spectrum.getSpectrumKey(),
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                peptideSpectrumAnnotator
        );
        Map<Integer, ArrayList<IonMatch>> coveredAminoAcids = peptideSpectrumAnnotator.getCoveredAminoAcids(
                annotationParameters,
                specificAnnotationPreferences,
                spectrum,
                peptideAssumption.getPeptide(),
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                true
        );
        return coveredAminoAcids.size();

    }

}
