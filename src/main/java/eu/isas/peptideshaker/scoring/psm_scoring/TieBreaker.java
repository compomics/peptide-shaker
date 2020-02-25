package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
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
     * Map of the occurrence of the protein accessions.
     */
    private final HashMap<String, Integer> proteinCount;
    /**
     * The search parameters.
     */
    private final SearchParameters searchParameters;
    /**
     * The spectrum annotation parameters.
     */
    private final AnnotationParameters annotationParameters;
    /**
     * The modification parameters.
     */
    private final ModificationParameters modificationParameters;
    /**
     * The sequence matching parameters for modifications.
     */
    private final SequenceMatchingParameters modificationSequenceMatchingParameters;
    /**
     * The sequence provider
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The spectrum provider.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * The peptide spectrum annotator.
     */
    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator;

    /**
     * Constructor.
     *
     * @param proteinCount Map of the occurrence of the protein accessions.
     * @param identificationParameters The identification parameters.
     * @param peptideSpectrumAnnotator The peptide spectrum annotator.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     */
    public TieBreaker(
            HashMap<String, Integer> proteinCount,
            IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider
    ) {
        this.proteinCount = proteinCount;
        this.annotationParameters = identificationParameters.getAnnotationParameters();
        this.searchParameters = identificationParameters.getSearchParameters();
        this.modificationParameters = searchParameters.getModificationParameters();
        this.modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        this.peptideSpectrumAnnotator = peptideSpectrumAnnotator;
        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;
    }

    /**
     * Returns the best assumption between the two given possibilities for the
     * given spectrum.
     *
     * @param spectrumFile The spectrum file name.
     * @param spectrumTitle The spectrum title.
     * @param peptideAssumption1 The first possible peptide.
     * @param peptideAssumption2 The second possible peptide.
     * @param silentFail If false an exception will be thrown if tie could not
     * be broken.
     *
     * @return The best assumption between the two given possibilities for the
     * given spectrum.
     */
    public PeptideAssumption getBestPeptideAssumption(
            String spectrumFile,
            String spectrumTitle,
            PeptideAssumption peptideAssumption1,
            PeptideAssumption peptideAssumption2,
            boolean silentFail
    ) {

        int proteinMaxOccurrence1 = peptideAssumption1.getPeptide().getProteinMapping().navigableKeySet().stream()
                .filter(
                        accession -> proteinCount.containsKey(accession)
                )
                .mapToInt(
                        accession -> proteinCount.get(accession)
                )
                .max()
                .orElse(1);
        int proteinMaxOccurrence2 = peptideAssumption1.getPeptide().getProteinMapping().navigableKeySet().stream()
                .filter(
                        accession -> proteinCount.containsKey(accession)
                )
                .mapToInt(
                        accession -> proteinCount.get(accession)
                )
                .max()
                .orElse(1);

        if (proteinMaxOccurrence1 > proteinMaxOccurrence2) {

            return peptideAssumption1;

        } else if (proteinMaxOccurrence1 < proteinMaxOccurrence2) {

            return peptideAssumption2;

        }

        Spectrum spectrum = spectrumProvider.getSpectrum(
                spectrumFile,
                spectrumTitle
        );

        int nCoveredAminoAcids1 = nCoveredAminoAcids(
                spectrumFile,
                spectrumTitle,
                peptideAssumption1,
                spectrum
        );
        int nCoveredAminoAcids2 = nCoveredAminoAcids(
                spectrumFile,
                spectrumTitle,
                peptideAssumption2,
                spectrum
        );

        if (nCoveredAminoAcids1 > nCoveredAminoAcids2) {

            return peptideAssumption1;

        } else if (nCoveredAminoAcids1 < nCoveredAminoAcids2) {

            return peptideAssumption2;

        }

        double massError1 = Math.abs(
                peptideAssumption1.getDeltaMass(
                        spectrum.getPrecursor().mz,
                        searchParameters.isPrecursorAccuracyTypePpm(),
                        searchParameters.getMinIsotopicCorrection(),
                        searchParameters.getMaxIsotopicCorrection()
                ));

        double massError2 = Math.abs(
                peptideAssumption2.getDeltaMass(
                        spectrum.getPrecursor().mz,
                        searchParameters.isPrecursorAccuracyTypePpm(),
                        searchParameters.getMinIsotopicCorrection(),
                        searchParameters.getMaxIsotopicCorrection()
                ));

        if (massError1 < massError2) {

            return peptideAssumption1;

        } else if (massError1 > massError2) {

            return peptideAssumption2;

        }

        if (silentFail) {

            return peptideAssumption1;

        }

        throw new IllegalArgumentException("Tie during best match selection in spectrum " + spectrumTitle + " of file " + spectrumFile + "(" + peptideAssumption1.getPeptide().getSequence() + " vs. " + peptideAssumption2.getPeptide().getSequence() + ".");

    }

    /**
     * Returns the number of amino acids covered by the fragment ions.
     *
     * @param spectrumFile The spectrum file name.
     * @param spectrumTitle The spectrum title.
     * @param peptideAssumption The peptide assumption.
     * @param spectrum The spectrum.
     *
     * @return The number of amino acids covered by the fragment ions.
     */
    private int nCoveredAminoAcids(
            String spectrumFile,
            String spectrumTitle,
            PeptideAssumption peptideAssumption,
            Spectrum spectrum
    ) {

        SpecificAnnotationParameters specificAnnotationPreferences = annotationParameters.getSpecificAnnotationParameters(
                spectrumFile,
                spectrumTitle,
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                peptideSpectrumAnnotator
        );
        Map<Integer, ArrayList<IonMatch>> coveredAminoAcids = peptideSpectrumAnnotator.getCoveredAminoAcids(
                annotationParameters,
                specificAnnotationPreferences,
                spectrumFile,
                spectrumTitle,
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
