package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.validation.percolator.PercolatorFeature;
import com.compomics.util.experiment.identification.validation.percolator.PercolatorFeaturesCache;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

/**
 * Utils for the export and import of Percolator results.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class PercolatorUtils {

    /**
     * Returns the header of the Percolator training file.
     *
     * @param searchParameters The parameters of the search.
     * @param rtPredictionsAvailable Flag indicating whether RT predictions are
     * given.
     *
     * @return The header of the Percolator training file.
     */
    public static String getHeader(
            SearchParameters searchParameters,
            Boolean rtPredictionsAvailable
    ) {

        StringBuilder header = new StringBuilder();

        header.append(
                String.join(
                        "\t",
                        "PSMId",
                        "Label",
                        "ScanNr",
                        "measured_mz",
                        "mz_error",
                        "pep",
                        "delta_pep",
                        "ion_fraction",
                        "peptide_length"
                )
        );

        for (int charge = searchParameters.getMinChargeSearched(); charge <= searchParameters.getMaxChargeSearched(); charge++) {

            header.append("\t").append("charge_").append(charge);

        }

        for (int isotope = searchParameters.getMinIsotopicCorrection(); isotope <= searchParameters.getMaxChargeSearched(); isotope++) {

            header.append("\t").append("isotope_").append(isotope);

        }

        if (searchParameters.getDigestionParameters().hasEnzymes()) {

            header.append("\t").append("unspecific");
            header.append("\t").append("enzymatic_N");
            header.append("\t").append("enzymatic_C");
            header.append("\t").append("enzymatic");

        }

        if (rtPredictionsAvailable) {
            header.append("\t").append("measured_rt").append("\t").append("rt_error");
        }

        header.append("\t").append("Peptide").append("\t").append("Proteins");

        return header.toString();

    }

    /**
     * Gets the peptide data to provide to percolator.
     *
     * @param spectrumMatch The spectrum match where the peptide was found.
     * @param peptideAssumption The peptide assumption.
     * @param rtPredictionsAvailable Flag indicating whether retention time
     * predictions are given.
     * @param predictedRts The retention time predictions for this peptide.
     * @param searchParameters The parameters of the search.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param spectrumProvider The spectrum provider.
     *
     * @return The peptide data as string.
     */
    public static String getPeptideData(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            Boolean rtPredictionsAvailable,
            ArrayList<Double> predictedRts,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider
    ) {

        PercolatorFeaturesCache percolatorFeaturesCache = (PercolatorFeaturesCache) peptideAssumption.getUrParam(PercolatorFeaturesCache.dummy);

        if (percolatorFeaturesCache == null) {

            percolatorFeaturesCache = new PercolatorFeaturesCache();

        }

        StringBuilder line = new StringBuilder();

        // PSM id
        long spectrumKey = spectrumMatch.getKey();

        Peptide peptide = peptideAssumption.getPeptide();
        long peptideKey = peptide.getMatchingKey();
        line.append(spectrumKey).append("_").append(peptideKey);

        // Label
        String decoyFlag = PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider) ? "-1" : "1";
        line.append("\t").append(decoyFlag);

        // Spectrum number
        line.append("\t").append(spectrumKey);

        // m/z
        Object cacheValue = percolatorFeaturesCache.cache.get(PercolatorFeature.measuredAndDeltaMz);

        if (cacheValue == null) {

            cacheValue = getMeasuredAndDeltaMzFeature(
                    spectrumMatch, 
                    peptideAssumption, 
                    searchParameters, 
                    spectrumProvider
            );

        }

        double[] measuredAndDeltaMz = (double[]) cacheValue;

        double measuredMz = measuredAndDeltaMz[0];
        double deltaMz = measuredAndDeltaMz[1];

        line.append("\t").append(measuredMz)
                .append("\t").append(deltaMz);

        // pep
        PSParameter psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
        double pep = psParameter.getProbability();
        line.append("\t").append(pep);
        double deltaPep = psParameter.getDeltaPEP();
        line.append("\t").append(deltaPep);

        // Ion fraction
        cacheValue = percolatorFeaturesCache.cache.get(PercolatorFeature.intensityCoverage);

        if (cacheValue == null) {

            cacheValue = getIntensityCoverageFeature(
                    spectrumMatch, 
                    peptideAssumption, 
                    searchParameters, 
                    annotationParameters, 
                    modificationLocalizationParameters, 
                    sequenceProvider, 
                    spectrumProvider
            );

        }

        double intensityCoverage = (double) cacheValue;

        line.append("\t").append(intensityCoverage);

// Peptide length
        line.append("\t").append(peptide.getSequence().length());

        // Charge
        for (int charge = searchParameters.getMinChargeSearched(); charge <= searchParameters.getMaxChargeSearched(); charge++) {

            char chargeOneHot = charge == peptideAssumption.getIdentificationCharge() ? '1' : '0';
            line.append("\t").append(chargeOneHot);

        }

        // Isotope
        for (int isotope = searchParameters.getMinIsotopicCorrection(); isotope <= searchParameters.getMaxChargeSearched(); isotope++) {

            char isotopeOneHot = isotope == peptideAssumption.getIsotopeNumber(measuredMz, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) ? '1' : '0';
            line.append("\t").append(isotopeOneHot);

        }

        // Enzymaticity
        if (searchParameters.getDigestionParameters().hasEnzymes()) {

            cacheValue = percolatorFeaturesCache.cache.get(PercolatorFeature.enzymaticity);

            if (cacheValue == null) {

                cacheValue = getEnzymaticityFeature(
                        peptideAssumption, 
                        searchParameters, 
                        sequenceProvider
                );

            }

            boolean[] enzymaticity = (boolean[]) cacheValue;

            line.append("\t");

            if (enzymaticity[0]) {

                line.append("1");

            } else {

                line.append("0");

            }

            line.append("\t");

            if (enzymaticity[1]) {

                line.append("1");

            } else {

                line.append("0");

            }

            line.append("\t");

            if (enzymaticity[2]) {

                line.append("1");

            } else {

                line.append("0");

            }

            line.append("\t");

            if (enzymaticity[3]) {

                line.append("1");

            } else {

                line.append("0");

            }
        }

        // Retention time
        if (rtPredictionsAvailable) {

            double measuredRt = spectrumProvider.getPrecursorRt(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());

            double rtError = predictedRts == null ? Double.NaN : predictedRts.stream()
                    .mapToDouble(
                            predictedRt -> Math.abs(predictedRt - measuredRt)
                    )
                    .min()
                    .orElse(Double.NaN);

            line.append("\t").append(measuredRt).append("\t").append(rtError);

        }

        line.append("\t").append("-.-.-").append("\t").append("-");

        return line.toString();

    }

    /**
     * Returns a unique key corresponding to the given peptide.
     *
     * @param peptideData The peptide data as string.
     *
     * @return The unique key corresponding to the peptide data.
     */
    public static long getPeptideKey(
            String peptideData
    ) {

        return ExperimentObject.asLong(peptideData);

    }

    /**
     * Computes the value for the enzymaticity feature.
     *
     * @param peptideAssumption The peptide assumption object.
     * @param searchParameters The search parameters.
     * @param sequenceProvider The sequence provider to use for protein
     * sequences.
     *
     * @return The value of the given feature as object.
     */
    public static boolean[] getEnzymaticityFeature(
            PeptideAssumption peptideAssumption,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider
    ) {

        boolean[] enzymaticity = new boolean[4];

        if (searchParameters.getDigestionParameters().hasEnzymes()) {

            boolean n = false;
            boolean c = false;
            boolean nc = false;

            Peptide peptide = peptideAssumption.getPeptide();

            for (Entry<String, int[]> entry : peptide.getProteinMapping().entrySet()) {

                String proteinSequence = sequenceProvider.getSequence(entry.getKey());

                for (int start : entry.getValue()) {

                    int end = start + peptide.getSequence().length() - 1;

                    boolean locationN = false;
                    boolean locationC = false;

                    for (Enzyme enzyme : searchParameters.getDigestionParameters().getEnzymes()) {

                        if (PeptideUtils.isNtermEnzymatic(start, end, proteinSequence, enzyme)) {

                            locationN = true;

                        }

                        if (PeptideUtils.isCtermEnzymatic(start, end, proteinSequence, enzyme)) {

                            locationC = true;

                        }

                    }

                    if (locationN && locationC) {

                        nc = true;

                    }

                    if (locationN) {

                        n = true;

                    }

                    if (locationC) {

                        c = true;

                    }
                }
            }

            if (!n && !c && !nc) {

                enzymaticity[0] = true;
                enzymaticity[1] = false;
                enzymaticity[2] = false;
                enzymaticity[3] = false;

            } else if (nc) {

                enzymaticity[0] = false;
                enzymaticity[1] = false;
                enzymaticity[2] = false;
                enzymaticity[3] = true;

            } else {

                enzymaticity[0] = false;
                enzymaticity[1] = n;
                enzymaticity[2] = c;
                enzymaticity[3] = false;

            }

            return enzymaticity;

        }

        return null;

    }

    /**
     * Computes the value for the measured and delta mass feature.
     *
     * @param spectrumMatch The spectrum match object.
     * @param peptideAssumption The peptide assumption object.
     * @param searchParameters The search parameters.
     * @param spectrumProvider The spectrum provider to use for spectra.
     *
     * @return The value of the given feature as object.
     */
    public static double[] getMeasuredAndDeltaMzFeature(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            SearchParameters searchParameters,
            SpectrumProvider spectrumProvider
    ) {

        double[] measuredAndDeltaMz = new double[2];

        double measuredMz = spectrumProvider.getPrecursorMz(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());

        measuredAndDeltaMz[0] = measuredMz;

        double deltaMz = peptideAssumption.getDeltaMz(
                measuredMz,
                searchParameters.isPrecursorAccuracyTypePpm(),
                searchParameters.getMinIsotopicCorrection(),
                searchParameters.getMaxIsotopicCorrection()
        );

        measuredAndDeltaMz[1] = deltaMz;

        return measuredAndDeltaMz;

    }

    /**
     * Computes the value for the intensity coverage feature.
     *
     * @param spectrumMatch The spectrum match object.
     * @param peptideAssumption The peptide assumption object.
     * @param searchParameters The search parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param sequenceProvider The sequence provider to use for protein
     * sequences.
     * @param spectrumProvider The spectrum provider to use for spectra.
     *
     * @return The value of the given feature as object.
     */
    public static double getIntensityCoverageFeature(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            SearchParameters searchParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider
    ) {

        PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();
        Spectrum spectrum = spectrumProvider.getSpectrum(spectrumFile, spectrumTitle);

        SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                spectrumFile,
                spectrumTitle,
                peptideAssumption,
                searchParameters.getModificationParameters(),
                sequenceProvider,
                modificationLocalizationParameters.getSequenceMatchingParameters(),
                peptideSpectrumAnnotator
        );

        IonMatch[] matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationParameters,
                specificAnnotationParameters,
                spectrumFile,
                spectrumTitle,
                spectrum,
                peptideAssumption.getPeptide(),
                searchParameters.getModificationParameters(),
                sequenceProvider,
                modificationLocalizationParameters.getSequenceMatchingParameters()
        );

        double coveredIntensity = Arrays.stream(matches)
                .mapToDouble(
                        ionMatch -> ionMatch.peakIntensity
                )
                .sum();

        double intensityCoverage = coveredIntensity / spectrum.getTotalIntensity();

        return intensityCoverage;

    }

}
