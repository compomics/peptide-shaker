package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideVariantMatches;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
     * The sequence matching parameters.
     */
    private final SequenceMatchingParameters sequenceMatchingParameters;
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
        this.sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
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

        long matchingKey1 = peptideAssumption1.getPeptide().getMatchingKey(sequenceMatchingParameters);
        long matchingKey2 = peptideAssumption2.getPeptide().getMatchingKey(sequenceMatchingParameters);

        if (matchingKey1 == matchingKey2) {

            if (silentFail) {

                return peptideAssumption1;

            } else {

                throw new IllegalArgumentException("Tie during best match selection in spectrum " + spectrumTitle + " of file " + spectrumFile + "(" + peptideAssumption1.getPeptide().getSequence() + " provided twice.");

            }
        }

        int variantCount1 = variantsCount(peptideAssumption1);
        int variantCount2 = variantsCount(peptideAssumption2);

        if (variantCount1 < variantCount2) {

            return peptideAssumption1;

        } else if (variantCount1 > variantCount2) {

            return peptideAssumption2;

        }

        int modCount1 = peptideAssumption1.getPeptide().getNVariableModifications();
        int modCount2 = peptideAssumption2.getPeptide().getNVariableModifications();

        if (modCount1 < modCount2) {

            return peptideAssumption1;

        } else if (modCount1 > modCount2) {

            return peptideAssumption2;

        }

        int proteinMaxOccurrence1 = proteinCount(peptideAssumption1);
        int proteinMaxOccurrence2 = proteinCount(peptideAssumption2);

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
                spectrum,
                true
        );
        int nCoveredAminoAcids2 = nCoveredAminoAcids(
                spectrumFile,
                spectrumTitle,
                peptideAssumption2,
                spectrum,
                true
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
                )
        );

        double massError2 = Math.abs(
                peptideAssumption2.getDeltaMass(
                        spectrum.getPrecursor().mz,
                        searchParameters.isPrecursorAccuracyTypePpm(),
                        searchParameters.getMinIsotopicCorrection(),
                        searchParameters.getMaxIsotopicCorrection()
                )
        );

        if (massError1 < massError2) {

            return peptideAssumption1;

        } else if (massError1 > massError2) {

            return peptideAssumption2;

        }

        nCoveredAminoAcids1 = nCoveredAminoAcids(
                spectrumFile,
                spectrumTitle,
                peptideAssumption1,
                spectrum,
                false
        );
        nCoveredAminoAcids2 = nCoveredAminoAcids(
                spectrumFile,
                spectrumTitle,
                peptideAssumption2,
                spectrum,
                false
        );

        if (nCoveredAminoAcids1 > nCoveredAminoAcids2) {

            return peptideAssumption1;

        } else if (nCoveredAminoAcids1 < nCoveredAminoAcids2) {

            return peptideAssumption2;

        }

        double annotatedIntensity1 = shareOfIntensityAnnotated(
                spectrumFile,
                spectrumTitle,
                peptideAssumption1,
                spectrum
        );
        double annotatedIntensity2 = shareOfIntensityAnnotated(
                spectrumFile,
                spectrumTitle,
                peptideAssumption2,
                spectrum
        );

        if (annotatedIntensity1 > annotatedIntensity2) {

            return peptideAssumption1;

        } else if (annotatedIntensity1 < annotatedIntensity2) {

            return peptideAssumption2;

        }

        return matchingKey1 < matchingKey2 ? peptideAssumption1 : peptideAssumption2;

    }

    /**
     * Returns the minimal number of variants found for the given peptide.
     *
     * @param peptideAssumption The peptide assumption.
     *
     * @return The minimal number of variants found for the given peptide.
     */
    private int variantsCount(
            PeptideAssumption peptideAssumption
    ) {

        HashMap<String, HashMap<Integer, PeptideVariantMatches>> variantMatchesMap = peptideAssumption
                .getPeptide()
                .getVariantMatches();

        return variantMatchesMap == null ? 0
                : variantMatchesMap.values()
                        .stream()
                        .flatMap(
                                variantMap -> variantMap
                                        .values()
                                        .stream()
                        )
                        .mapToInt(
                                variantMatches -> variantMatches.getVariantMatches().size()
                        )
                        .min()
                        .orElse(0);

    }

    /**
     * Returns the maximal number of peptides found for a protein the given
     * peptide can map to.
     *
     * @param peptideAssumption The peptide assumption.
     *
     * @return The maximal number of peptides found for a protein the given
     * peptide can map to.
     */
    private int proteinCount(
            PeptideAssumption peptideAssumption
    ) {

        return peptideAssumption
                .getPeptide()
                .getProteinMapping()
                .navigableKeySet()
                .stream()
                .filter(
                        accession -> proteinCount.containsKey(accession)
                )
                .mapToInt(
                        accession -> proteinCount.get(accession)
                )
                .max()
                .orElse(1);

    }

    /**
     * Returns the number of amino acids covered by the fragment ions.
     *
     * @param spectrumFile The spectrum file name.
     * @param spectrumTitle The spectrum title.
     * @param peptideAssumption The peptide assumption.
     * @param spectrum The spectrum.
     * @param spectrum If true lower intensities are filtered out.
     *
     * @return The number of amino acids covered by the fragment ions.
     */
    private int nCoveredAminoAcids(
            String spectrumFile,
            String spectrumTitle,
            PeptideAssumption peptideAssumption,
            Spectrum spectrum,
            boolean intensityLimit
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
                intensityLimit
        );
        return coveredAminoAcids.size();

    }

    /**
     * Returns the share of spectrum intensity annotated.
     *
     * @param spectrumFile The spectrum file name.
     * @param spectrumTitle The spectrum title.
     * @param peptideAssumption The peptide assumption.
     * @param spectrum The spectrum.
     *
     * @return The number of amino acids covered by the fragment ions.
     */
    private double shareOfIntensityAnnotated(
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

        IonMatch[] ionMatches = peptideSpectrumAnnotator.getSpectrumAnnotation(
                annotationParameters,
                specificAnnotationPreferences,
                spectrumFile,
                spectrumTitle,
                spectrum,
                peptideAssumption.getPeptide(),
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                false
        );

        double annotatedIntensity = Arrays.stream(ionMatches)
                .collect(
                        Collectors.toMap(
                                ionMatch -> ionMatch.peakMz,
                                ionMatch -> ionMatch.peakIntensity,
                                (a, b) -> a,
                                HashMap::new
                        )
                )
                .values()
                .stream()
                .mapToDouble(
                        a -> a
                )
                .sum();

        return annotatedIntensity / spectrum.getTotalIntensity();

    }

}
