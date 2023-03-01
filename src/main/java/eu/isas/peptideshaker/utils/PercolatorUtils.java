package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
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
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.pride.CvTerm;
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
     * @param spectraPredictionsAvailable Flag indicating whether spectra
     * predictions are given.
     *
     * @return The header of the Percolator training file.
     */
    public static String getHeader(
            SearchParameters searchParameters,
            Boolean rtPredictionsAvailable,
            Boolean spectraPredictionsAvailable
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
            header.append("\t").append("measured_rt");
            header.append("\t").append("rt_Abs_error");
            header.append("\t").append("rt_Square_error");
            header.append("\t").append("rt_Log_error");
        }

        if (spectraPredictionsAvailable) {
            header.append("\t").append("matched_peaks");
            header.append("\t").append("spectra_log");
            header.append("\t").append("spectra_cos_similarity");
            header.append("\t").append("spectra_angular_similarity");
            header.append("\t").append("spectra_cross_entropy");

            header.append("\t").append("b_ion_coverage");
            header.append("\t").append("b_ion_matched_peaks");
            header.append("\t").append("b_ion_spectra_log");
            header.append("\t").append("b_ion_spectra_cos_similarity");
            header.append("\t").append("b_ion_spectra_angular_similarity");
            header.append("\t").append("b_ion_spectra_cross_entropy");

            header.append("\t").append("y_ion_coverage");
            header.append("\t").append("y_ion_matched_peaks");
            header.append("\t").append("y_ion_spectra_log");
            header.append("\t").append("y_ion_spectra_cos_similarity");
            header.append("\t").append("y_ion_spectra_angular_similarity");
            header.append("\t").append("y_ion_spectra_cross_entropy");
        }

        header.append("\t").append("Peptide").append("\t").append("Proteins");

        return header.toString();

    }

    /**
     * Returns the header of the file with the RT observed and predicted values.
     *
     * @return The header of the file with the RT observed and predicted values.
     */
    public static String getRTValuesHeader() {

        StringBuilder header = new StringBuilder();

        header.append(
                String.join(
                        "\t",
                        "PSMId",
                        "Decoy",
                        "measured_rt",
                        "predicted_rt",
                        "scaled_measured_rt",
                        "scaled_predicted_rt",
                        "engine_score"
                )
        );

        return header.toString();

    }

    /**
     * Gets the peptide data to provide to percolator.
     *
     * @param spectrumMatch The spectrum match where the peptide was found.
     * @param peptideAssumption The peptide assumption.
     * @param peptideRTs The retention time predictions for this peptide.
     * @param predictedSpectra The predicted mass spectra for this peptide.
     * @param searchParameters The parameters of the search.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param spectrumProvider The spectrum provider.
     * @param modificationParameters The modification parameters
     *
     * @return The peptide data as string.
     */
    public static String getPeptideData(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            ArrayList<Double> peptideRTs,
            ArrayList<Spectrum> predictedSpectra,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            ModificationParameters modificationParameters
    ) {

        PercolatorFeaturesCache percolatorFeaturesCache = (PercolatorFeaturesCache) peptideAssumption.getUrParam(PercolatorFeaturesCache.dummy);

        if (percolatorFeaturesCache == null) {

            percolatorFeaturesCache = new PercolatorFeaturesCache();

        }

        StringBuilder line = new StringBuilder();

        // PSM id
        long spectrumKey = spectrumMatch.getKey();
        Peptide peptide = peptideAssumption.getPeptide();

        //long peptideKey = peptide.getMatchingKey();
        //line.append(spectrumKey).append("_").append(peptideKey);
        // Get peptide data
        String peptideData = Ms2PipUtils.getPeptideData(
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );

        // Get corresponding key
        long peptideMs2PipKey = Ms2PipUtils.getPeptideKey(peptideData);
        String peptideID = Long.toString(peptideMs2PipKey);

        String psmID = String.join("_", String.valueOf(spectrumKey), peptideID);
        line.append(psmID);

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
        if (peptideRTs != null) {

            /*double measuredRt = spectrumProvider.getPrecursorRt(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());

            double rtAbsError = predictedRts == null ? Double.NaN : predictedRts.stream()
                    .mapToDouble(
                            predictedRt -> Math.abs(predictedRt - measuredRt)
                    )
                    .min()
                    .orElse(Double.NaN);
            
            double rtSqrError = predictedRts == null ? Double.NaN : predictedRts.stream()
                    .mapToDouble(
                            predictedRt -> Math.pow((predictedRt - measuredRt),2)
                    )
                    .min()
                    .orElse(Double.NaN);
            
            double rtLogError = predictedRts == null ? Double.NaN : predictedRts.stream()
                    .mapToDouble(
                            predictedRt -> (Math.log(predictedRt) - Math.log(measuredRt))
                    )
                    .min()
                    .orElse(Double.NaN);*/
            double measuredRt = peptideRTs.get(0);
            double predictedRT = peptideRTs.get(1);
            double rtAbsError = Math.abs(predictedRT - measuredRt);
            double rtSqrError = Math.pow((predictedRT - measuredRt), 2);

            if (predictedRT < 0.0) {
                predictedRT = 0.0000000001;
            }
            if (measuredRt < 0.0) {
                measuredRt = 0.0000000001;
            }
            double rtLogError = Math.abs(Math.log(predictedRT) - Math.log(measuredRt));

            line.append("\t").append(measuredRt);
            line.append("\t").append(rtAbsError);
            line.append("\t").append(rtSqrError);
            line.append("\t").append(rtLogError);

        }

        // Distance of observed and predicted mass spectrum
        if (predictedSpectra != null) {

            // Get measured spectrum
            Spectrum measuredSpectrum = spectrumProvider.getSpectrum(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());

            Spectrum predictedSpectrum = predictedSpectra.get(0);

            String ionSpectrum = "whole";

            StringBuilder wholeSpectrumMetrics = calculateSpectraDistances(measuredSpectrum, predictedSpectrum, ionSpectrum);

            line.append("\t").append(wholeSpectrumMetrics);

            Spectrum predictedSpectrumBIon = predictedSpectra.get(1);

            ionSpectrum = "B";

            StringBuilder BIonSpectrumMetrics = calculateSpectraDistances(measuredSpectrum, predictedSpectrumBIon, ionSpectrum);

            line.append("\t").append(BIonSpectrumMetrics);

            Spectrum predictedSpectrumYIon = predictedSpectra.get(2);

            ionSpectrum = "Y";

            StringBuilder YIonSpectrumMetrics = calculateSpectraDistances(measuredSpectrum, predictedSpectrumYIon, ionSpectrum);

            line.append("\t").append(YIonSpectrumMetrics);

        }

        //Peptide sequence
        String peptideSequence = getSequenceWithModifications(peptideAssumption.getPeptide(), modificationParameters, sequenceProvider, sequenceMatchingParameters, modificationFactory);

        line.append("\t").append("-.").append(peptideSequence).append(".-");

        line.append("\t").append("-");

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

        if (searchParameters.getDigestionParameters().hasEnzymes()) {

            boolean[] enzymaticity = new boolean[4];

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

                        try {

                            if (PeptideUtils.isCtermEnzymatic(start, end, proteinSequence, enzyme)) {

                                locationC = true;

                            }

                        } catch (Exception e) {

                            System.out.println("Protein accession: " + entry.getKey());
                            System.out.println("Protein: " + proteinSequence);
                            System.out.println("Peptide: " + peptide.getSequence());
                            System.out.println("Start: " + String.valueOf(start));
                            System.out.println("End: " + String.valueOf(end));
                            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

                            locationC = true;

                            //throw(e);
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
     * Returns the spectra metrics.
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     * @param ionSpectrum String showing whether the spectrum consists of only B
     * or Y ions.
     *
     * @return String builder with the spectra distances.
     */
    public static StringBuilder calculateSpectraDistances(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            String ionSpectrum
    ) {
        StringBuilder results = new StringBuilder();

        ArrayList<ArrayList<Integer>> aligned_peaks = getAlignedPeaks(measuredSpectrum, predictedSpectrum);

        ArrayList<ArrayList<Integer>> matchedPeaks = new ArrayList<>();
        for (int i = 0; i < aligned_peaks.size(); i++) {
            if (aligned_peaks.get(i).get(0) != -1) {
                matchedPeaks.add(aligned_peaks.get(i));
            }
        }

        if (ionSpectrum.equals("B") || ionSpectrum.equals("Y")) {
            int terminusCoverage = findTerminiCoverage(aligned_peaks, ionSpectrum);
            results.append(terminusCoverage).append("\t");
        }

        double matchedPeaksRatio = findMatchedPeaksRatio(predictedSpectrum, matchedPeaks.size());
        results.append(matchedPeaksRatio);

        ArrayList<Spectrum> spectraScaledIntensities = scaleIntensities(measuredSpectrum, predictedSpectrum, matchedPeaks);

        double spectraLogDistance = getSpectraLogDist(spectraScaledIntensities.get(0), spectraScaledIntensities.get(1), aligned_peaks, matchedPeaksRatio);
        results.append("\t").append(spectraLogDistance);

        double spectraCosineSimilarity = getSpectraCosine(spectraScaledIntensities.get(0), spectraScaledIntensities.get(1), aligned_peaks);
        results.append("\t").append(spectraCosineSimilarity);

        double angularSimilarity = 1.0 - (Math.acos(spectraCosineSimilarity) / Math.PI);

        results.append("\t").append(angularSimilarity);

        ArrayList<Spectrum> normalizedIntensities = normalizeIntensities(measuredSpectrum, predictedSpectrum, aligned_peaks);

        double crossEntropy = getCrossEntropy(normalizedIntensities.get(0), normalizedIntensities.get(1), matchedPeaksRatio);
        results.append("\t").append(crossEntropy);

        return results;
    }

    /**
     * Returns the ratio of matched predicted peaks.
     *
     * @param aligned_peaks The indices of aligned peaks.
     * @param ion "B" or "Y" ion.
     *
     * @return #mathced peaks / #predicted peaks
     */
    public static int findTerminiCoverage(
            ArrayList<ArrayList<Integer>> aligned_peaks,
            String ion
    ) {
        int coverage = 0;

        if (aligned_peaks.isEmpty()) {
            return coverage;
        }

        if (ion.equals("B")) {
            for (int i = 0; i < aligned_peaks.size(); i++) {
                if (aligned_peaks.get(i).get(0) == -1) {
                    coverage = i;
                    return coverage;
                }
            }
            coverage = aligned_peaks.size();
        } else {
            for (int i = aligned_peaks.size() - 1; i >= 0; i--) {
                if (aligned_peaks.get(i).get(0) == -1) {
                    coverage = aligned_peaks.size() - i - 1;
                    return coverage;
                }
            }
            coverage = aligned_peaks.size();
        }

        return coverage;
    }

    /**
     * Returns the ratio of matched predicted peaks.
     *
     * @param predictedSpectrum The predicted spectrum.
     * @param alignedPeaksNum The number of matched peaks.
     *
     * @return #mathced peaks / #predicted peaks
     */
    public static double findMatchedPeaksRatio(
            Spectrum predictedSpectrum,
            int alignedPeaksNum
    ) {
        if (alignedPeaksNum == 0) {
            return 0.0;
        }

        double[] predictedIntensities = predictedSpectrum.intensity;

        int nonZeroIntensityPeaks = 0;
        for (int i = 0; i < predictedIntensities.length; i++) {
            if (predictedIntensities[i] > 0.0) {
                nonZeroIntensityPeaks = nonZeroIntensityPeaks + 1;
            }
        }
        double ratio = (double) alignedPeaksNum / nonZeroIntensityPeaks;

        return ratio;
    }

    /**
     * Scale the intensities of the observed and predicted spectra.
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     * @param alignedPeaks The indices of the matched peaks.
     *
     * @return ArrayList(ScaledObservedSpectrum, ScaledPredictedSpectrum)
     */
    public static ArrayList<Spectrum> scaleIntensities(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            ArrayList<ArrayList<Integer>> alignedPeaks
    ) {

        double[] measuredIntensity = measuredSpectrum.intensity;
        double[] predictedIntensity = predictedSpectrum.intensity;

        double scaleFactorMeasured = 0.0;
        double scaleFactorPredicted = 0.0;

        if (alignedPeaks.size() <= 1) {

            double[] sortedMeasuredIntensity = Arrays.copyOf(measuredIntensity, measuredIntensity.length);
            Arrays.sort(sortedMeasuredIntensity);
            double avgIntensityMeasured = 0.0;
            int counter = 0;
            for (int i = sortedMeasuredIntensity.length - 1; i > Math.max(sortedMeasuredIntensity.length - 10, -1); i--) {
                avgIntensityMeasured += sortedMeasuredIntensity[i];
                counter = counter + 1;
            }
            avgIntensityMeasured = (double) avgIntensityMeasured / counter;

            double[] sortedPredictedIntensity = Arrays.copyOf(predictedIntensity, predictedIntensity.length);
            Arrays.sort(sortedPredictedIntensity);
            double avgIntensityPredicted = 0.0;
            counter = 0;
            for (int i = sortedPredictedIntensity.length - 1; i > Math.max(sortedPredictedIntensity.length - 10, -1); i--) {
                avgIntensityPredicted += sortedPredictedIntensity[i];
                counter = counter + 1;
            }
            avgIntensityPredicted = (double) avgIntensityPredicted / counter;

            scaleFactorMeasured = avgIntensityMeasured;
            scaleFactorPredicted = avgIntensityPredicted;

        } else {

            double[] measuredAlignedIntensities = new double[alignedPeaks.size()];
            for (int i = 0; i < measuredAlignedIntensities.length; i++) {
                measuredAlignedIntensities[i] = measuredIntensity[alignedPeaks.get(i).get(0)];
            }
            Arrays.sort(measuredAlignedIntensities);

            double[] predictedAlignedIntensities = new double[alignedPeaks.size()];
            for (int i = 0; i < measuredAlignedIntensities.length; i++) {
                predictedAlignedIntensities[i] = predictedIntensity[alignedPeaks.get(i).get(1)];
            }
            Arrays.sort(predictedAlignedIntensities);

            if (alignedPeaks.size() % 2 == 0) {
                scaleFactorMeasured = (measuredAlignedIntensities[alignedPeaks.size() / 2] + measuredAlignedIntensities[(alignedPeaks.size() / 2) - 1]) / 2.0;
                scaleFactorPredicted = (predictedAlignedIntensities[alignedPeaks.size() / 2] + predictedAlignedIntensities[(alignedPeaks.size() / 2) - 1]) / 2.0;
            } else {
                scaleFactorMeasured = measuredAlignedIntensities[alignedPeaks.size() / 2];
                scaleFactorPredicted = predictedAlignedIntensities[alignedPeaks.size() / 2];
            }
        }

        /*if (measuredMedian == 0.0){
            System.out.println("Measured median = 0.0");
        }
        if (predictedMedian == 0.0){
            System.out.println("Predicted median = 0.0");
            System.out.println(alignedPeaks.size());
            System.out.println(Arrays.toString(predictedIntensity));
        }*/
        double[] scaledMeasuredIntensity = new double[measuredIntensity.length];
        for (int i = 0; i < scaledMeasuredIntensity.length; i++) {
            scaledMeasuredIntensity[i] = measuredIntensity[i] / scaleFactorMeasured;
        }

        double[] scaledPredictedIntensity = new double[predictedIntensity.length];
        for (int i = 0; i < scaledPredictedIntensity.length; i++) {
            scaledPredictedIntensity[i] = predictedIntensity[i] / scaleFactorPredicted;
        }

        Spectrum scaledMeasuredSpectrum = new Spectrum(null, measuredSpectrum.mz, scaledMeasuredIntensity, 2);
        Spectrum scaledPredictedSpectrum = new Spectrum(null, predictedSpectrum.mz, scaledPredictedIntensity, 2);

        ArrayList<Spectrum> scaledSpectra = new ArrayList<>(2);
        scaledSpectra.add(scaledMeasuredSpectrum);
        scaledSpectra.add(scaledPredictedSpectrum);

        return scaledSpectra;

    }

    /**
     * Normalize the intensities of the observed and predicted spectra, as
     * probability distributions for cross entropy computation.
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     * @param alignedPeaks The indices of the matched peaks.
     *
     * @return ArrayList(NormalizedObservedSpectrum,
     * NormalizedPredictedSpectrum)
     */
    public static ArrayList<Spectrum> normalizeIntensities(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            ArrayList<ArrayList<Integer>> alignedPeaks
    ) {

        double[] measuredIntensity = measuredSpectrum.intensity;
        double[] predictedIntensity = predictedSpectrum.intensity;

        double predictedIntsSum = 0.0;
        double measuredIntsSum = 0.0;
        for (int i = 0; i < alignedPeaks.size(); i++) {
            predictedIntsSum += predictedIntensity[alignedPeaks.get(i).get(1)];
            if (alignedPeaks.get(i).get(0) != -1) {
                measuredIntsSum += measuredIntensity[alignedPeaks.get(i).get(0)];
            }
        }

        double[] normMeasuredInts = new double[alignedPeaks.size()];
        double[] normPredictedInts = new double[alignedPeaks.size()];
        double[] mzs = new double[alignedPeaks.size()];
        for (int i = 0; i < alignedPeaks.size(); i++) {
            if (alignedPeaks.get(i).get(0) != -1) {
                normMeasuredInts[i] = measuredIntensity[alignedPeaks.get(i).get(0)] / measuredIntsSum;
            } else {
                normMeasuredInts[i] = 0.0;
            }
            normPredictedInts[i] = predictedIntensity[alignedPeaks.get(i).get(1)] / predictedIntsSum;
            mzs[i] = predictedSpectrum.mz[alignedPeaks.get(i).get(1)];
        }

        Spectrum normMeasuredSpectrum = new Spectrum(null, mzs, normMeasuredInts, 2);
        Spectrum normPredictedSpectrum = new Spectrum(null, mzs, normPredictedInts, 2);

        ArrayList<Spectrum> normSpectra = new ArrayList<>();
        normSpectra.add(normMeasuredSpectrum);
        normSpectra.add(normPredictedSpectrum);

        return normSpectra;

    }

    //ArrayList<ArrayList<Integer>> alignedPeaks
    /**
     * Returns the cross entropy between the measured and predicted mass
     * spectrum.
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     * @param matchedPeaksRatio The ratio of matched predicted peaks. //@param
     * alignedPeaks The indices of the matched peaks.
     *
     * @return spectra cross entropy
     */
    public static double getCrossEntropy(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            double matchedPeaksRatio
    ) {

        //Need to define a max value for all the spectra with 0 matched peaks
        if (matchedPeaksRatio == 0.0) {
            //return 10.0;
            return 15.0;
        }

        double[] measuredIntensities = measuredSpectrum.intensity;
        double[] predictedIntensities = predictedSpectrum.intensity;

        /*
        //Replace 0 measured intensity when peak is not matched
        // with the entropy of the bionomial distribution for the 
        // specific number of predicted peaks.
        double entropy = 2 * Math.PI * Math.exp(1) * predictedIntensities.length;
        entropy = entropy * matchedPeaksRatio * (1.0 - matchedPeaksRatio);
        entropy = Math.log(entropy);
        entropy = 0.5 * entropy;
         */
        for (int i = 0; i < measuredIntensities.length; i++) {
            if (measuredIntensities[i] == 0.0) {
                //measuredIntensities[i] = entropy;
                measuredIntensities[i] = 0.000001;
            }
        }

        double crossEntropy = 0.0;
        for (int i = 0; i < measuredIntensities.length; i++) {
            double predictedIntensity = predictedIntensities[i];
            double measuredIntensity = measuredIntensities[i];
            //crossEntropy = crossEntropy - (measuredIntensity * Math.log(predictedIntensity));
            crossEntropy = crossEntropy - (predictedIntensity * Math.log(measuredIntensity));
        }

        return crossEntropy;

    }

    /**
     * Returns the cosine distance between the measured and predicted mass
     * spectrum.
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     * @param alignedPeaks The indices of the matched peaks.
     *
     * @return spectra cosine similarity
     */
    public static double getSpectraCosine(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            ArrayList<ArrayList<Integer>> alignedPeaks
    ) {
        /*if (alignedPeaks.isEmpty()){
            return -1.0;
        }*/

        double[] measuredIntensities = measuredSpectrum.intensity;
        double[] predictedIntensities = predictedSpectrum.intensity;

        double sumMP = 0.0;
        double sumM = 0.0;
        double sumP = 0.0;
        for (int i = 0; i < alignedPeaks.size(); i++) {
            double predictedIntensity = predictedIntensities[alignedPeaks.get(i).get(1)];
            double measuredIntensity = 0.0;
            if (alignedPeaks.get(i).get(0) != -1) {
                measuredIntensity = measuredIntensities[alignedPeaks.get(i).get(0)];
            }
            sumMP += measuredIntensity * predictedIntensity;
            sumM += Math.pow(measuredIntensity, 2.0);
            sumP += Math.pow(predictedIntensity, 2.0);
        }
        sumM = Math.sqrt(sumM);
        sumP = Math.sqrt(sumP);

        double cosSim = 0.0;
        if (sumM != 0) {
            cosSim = sumMP / (sumM * sumP);
        }

        return cosSim;
    }

    /**
     * Returns the log distance between the measured and predicted mass
     * spectrum.
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     * @param alignedPeaks The indices of the matched peaks.
     * @param matchedPeaksRatio The ratio of matched predicted peaks.
     *
     * @return spectra log distance
     */
    public static double getSpectraLogDist(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            ArrayList<ArrayList<Integer>> alignedPeaks,
            double matchedPeaksRatio
    ) {

        //Need to define a max value for all the spectra with 0 matched peaks
        if (matchedPeaksRatio == 0.0) {
            return 18.0;
        }

        double[] measuredIntensities = measuredSpectrum.intensity;
        double[] predictedIntensities = predictedSpectrum.intensity;

        double dist = 0.0;
        for (int i = 0; i < alignedPeaks.size(); i++) {
            double predictedIntensity = predictedIntensities[alignedPeaks.get(i).get(1)];
            double measuredIntensity = 0.0;
            if (alignedPeaks.get(i).get(0) != -1) {
                measuredIntensity = measuredIntensities[alignedPeaks.get(i).get(0)];
            }
            double minimumIntensity = 0.000001;
            if (predictedIntensity == 0.0) {
                predictedIntensity = minimumIntensity;
            }
            if (measuredIntensity == 0.0) {
                measuredIntensity = minimumIntensity;
            }
            predictedIntensity = Math.log(predictedIntensity);
            measuredIntensity = Math.log(measuredIntensity);
            dist += Math.abs(measuredIntensity - predictedIntensity);
        }
        dist = dist / alignedPeaks.size();

        return dist;
    }

    /**
     * Returns the indices of the predicted peaks and their measured matches
     * (measuredIndex = -1 if predicted peak unmatched).
     *
     * @param measuredSpectrum The measured spectrum.
     * @param predictedSpectrum The predicted spectrum.
     *
     * @return [ [measuredMatchedPeakIndex_1, predictedMatchedPeakIndex_1], ...
     * [] [measuredMatchedPeakIndex_n, predictedMatchedPeakIndex_n]]
     */
    public static ArrayList<ArrayList<Integer>> getAlignedPeaks(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum
    ) {
        double ppm_error_threshold = 10.0;
        ArrayList<ArrayList<Integer>> alignedPeaks = new ArrayList<>();

        double[] measuredMzs = measuredSpectrum.mz;
        double[] predictedMzs = predictedSpectrum.mz;
        double[] predictedIntensities = predictedSpectrum.intensity;

        int measuredMzIndex = 0;
        for (int i = 0; i < predictedMzs.length; i++) {

            if (predictedIntensities[i] == 0.0) {
                continue;
            }

            double minMzDist = Math.abs(predictedMzs[i] - measuredMzs[measuredMzIndex]);
            boolean foundMatchedPeak = false;
            while (measuredMzIndex < measuredMzs.length - 1) {
                measuredMzIndex++;
                double mzDist = Math.abs(predictedMzs[i] - measuredMzs[measuredMzIndex]);
                if (minMzDist < mzDist) {
                    double ppm_error = 1000000 * minMzDist / predictedMzs[i];
                    if (ppm_error <= ppm_error_threshold) {
                        ArrayList<Integer> matchedPeaksIndices = new ArrayList<>();
                        matchedPeaksIndices.add(measuredMzIndex - 1);
                        matchedPeaksIndices.add(i);
                        alignedPeaks.add(matchedPeaksIndices);
                        foundMatchedPeak = true;
                    }
                    break;
                } else {
                    minMzDist = mzDist;
                }
            }

            if (!foundMatchedPeak) {
                ArrayList<Integer> unmatchedPeakIndex = new ArrayList<>();
                unmatchedPeakIndex.add(-1);
                unmatchedPeakIndex.add(i);
                alignedPeaks.add(unmatchedPeakIndex);
            }

        }

        return alignedPeaks;

    }

    /**
     * Returns the sequence of the peptides with modifications encoded as
     * required by Percolator.
     *
     * @param peptide The peptide.
     * @param modificationParameters The modification parameters of the search.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param modificationFactory The factory containing the modification
     * details
     *
     * @return the modifications of the peptides encoded as required by DeepLc.
     */
    public static String getSequenceWithModifications(
            Peptide peptide,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory
    ) {

        String peptideSequence = peptide.getSequence();
        int nMods = 0;

        // Fixed modifications
        String[] fixedModifications = peptide.getFixedModifications(modificationParameters, sequenceProvider, sequenceMatchingParameters);

        String[] modificationsUnimodIds = new String[peptideSequence.length()];
        Arrays.fill(modificationsUnimodIds, "");

        for (int i = 0; i < fixedModifications.length; i++) {

            if (fixedModifications[i] != null) {

            int indexOnSequence = PeptideUtils.getModifiedAaIndex(i, peptideSequence.length());

                nMods++;

                String modName = fixedModifications[i];
                Modification modification = modificationFactory.getModification(modName);
                CvTerm cvTerm = modification.getUnimodCvTerm();
                String accession = cvTerm.getAccession();

                if (cvTerm == null) {

                    throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

                }

                modificationsUnimodIds[indexOnSequence] = String.join("", modificationsUnimodIds[indexOnSequence], "[", accession.substring(accession.indexOf(":") + 1), "]");

            }
        }

        // Variable modifications
        nMods += peptide.getVariableModifications().length;

        for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

            int indexOnSequence = PeptideUtils.getModifiedAaIndex(modificationMatch.getSite(), peptideSequence.length());

            String modName = modificationMatch.getModification();
            Modification modification = modificationFactory.getModification(modName);
            CvTerm cvTerm = modification.getUnimodCvTerm();
            String accession = cvTerm.getAccession();

            if (cvTerm == null) {

                throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

            }

            modificationsUnimodIds[indexOnSequence] = String.join("", modificationsUnimodIds[indexOnSequence], modificationsUnimodIds[indexOnSequence], "[", accession.substring(accession.indexOf(":") + 1), "]");

        }

        if (nMods == 0) {

            return peptideSequence;

        }

        StringBuilder sequenceWithMods = new StringBuilder(peptideSequence.length() + 4 * nMods);

        for (int i = 0; i < peptideSequence.length(); i++) {

            sequenceWithMods.append(peptideSequence.charAt(i)).append(modificationsUnimodIds[i]);

        }

        return sequenceWithMods.toString();

    }

    public static String getPeptideRTData(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            ModificationParameters modificationParameters,
            ArrayList<Double> peptideRTs,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory
    ) {

        StringBuilder line = new StringBuilder();

        // PSM id
        long spectrumKey = spectrumMatch.getKey();

        // Get peptide data
        String peptideData = Ms2PipUtils.getPeptideData(
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );
        // Get corresponding key
        long peptideMs2PipKey = Ms2PipUtils.getPeptideKey(peptideData);
        String peptideID = Long.toString(peptideMs2PipKey);

        String psmID = String.join("_", String.valueOf(spectrumKey), peptideID);
        line.append(psmID);

        // Label
        String decoyFlag = PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider) ? "-1" : "1";

        line.append("\t").append(decoyFlag);
        line.append("\t").append(peptideRTs.get(0));
        line.append("\t").append(peptideRTs.get(1));
        line.append("\t").append(peptideRTs.get(2));
        line.append("\t").append(peptideRTs.get(3));

        double searchEngineScore = peptideAssumption.getScore();
        line.append("\t").append(searchEngineScore);

        return line.toString();
    }

    public static ArrayList<Double> getPeptideObservedPredictedRT(
            SpectrumMatch spectrumMatch,
            ArrayList<Double> predictedRts,
            SpectrumProvider spectrumProvider
    ) {

        ArrayList<Double> rtValues = new ArrayList<>();

        double measuredRt = spectrumProvider.getPrecursorRt(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());

        int bestRTindex = 0;
        double minRTdistance = Math.abs(predictedRts.get(0) - measuredRt);
        for (int i = 1; i < predictedRts.size(); i++) {
            if (Math.abs(predictedRts.get(i) - measuredRt) < minRTdistance) {
                bestRTindex = i;
            }
        }
        double bestRTprediction = predictedRts.get(bestRTindex);
        rtValues.add(measuredRt);
        rtValues.add(bestRTprediction);

        return rtValues;
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

    /**
     * Returns a unique key corresponding to the given PSM.
     *
     * @param psmData The psm data as string.
     *
     * @return The unique key corresponding to the PSM data.
     */
    public static long getPsmKey(
            String psmData
    ) {

        return ExperimentObject.asLong(psmData);

    }

}

/**
 * Scale the intensities of the observed and predicted spectra.
 *
 * @param measuredSpectrum The measured spectrum.
 * @param predictedSpectrum The predicted spectrum.
 * @param alignedPeaks The indices of the matched peaks.
 *
 * @return ArrayList(ScaledObservedSpectrum, ScaledPredictedSpectrum)
 */
/*    public static ArrayList<Spectrum> scaleIntensities(
            Spectrum measuredSpectrum,
            Spectrum predictedSpectrum,
            ArrayList<ArrayList<Integer>> alignedPeaks
    ){
        
        double[] measuredIntensity = measuredSpectrum.intensity;
        double[] predictedIntensity = predictedSpectrum.intensity;
        
        double measuredMedian = 0.0;
        double predictedMedian = 0.0;
        if (alignedPeaks.size() == 0){
            
            double[] sortedMeasuredIntensity = Arrays.copyOf(measuredIntensity, measuredIntensity.length);
            Arrays.sort(sortedMeasuredIntensity);
            ArrayList<Double> nonZeroMeasuredIntensity = new ArrayList<>();
            for (int i=0; i<sortedMeasuredIntensity.length; i++){
                if (sortedMeasuredIntensity[i] != 0){
                    nonZeroMeasuredIntensity.add(sortedMeasuredIntensity[i]);
                }
            }
            double[] sortedPredictedIntensity = Arrays.copyOf(predictedIntensity, predictedIntensity.length);
            Arrays.sort(sortedPredictedIntensity);
            ArrayList<Double> nonZeroPredictedIntensity = new ArrayList<>();
            for (int i=0; i<sortedPredictedIntensity.length; i++){
                if (sortedPredictedIntensity[i] != 0){
                    nonZeroPredictedIntensity.add(sortedPredictedIntensity[i]);
                }
            }
            
            int medianIndex = nonZeroMeasuredIntensity.size() / 2;
            if (nonZeroMeasuredIntensity.size() % 2 == 0){
                measuredMedian = (nonZeroMeasuredIntensity.get(medianIndex) + nonZeroMeasuredIntensity.get(medianIndex) + 1) / 2.0;
            }
            else{
                measuredMedian = nonZeroMeasuredIntensity.get(medianIndex);
            }
            medianIndex = nonZeroPredictedIntensity.size() / 2;
            if (nonZeroPredictedIntensity.size() % 2 == 0){
                predictedMedian = (nonZeroPredictedIntensity.get(medianIndex) + nonZeroPredictedIntensity.get(medianIndex + 1)) / 2.0;
            }
            else{
                predictedMedian = nonZeroPredictedIntensity.get(medianIndex);
            } 
        }
        else if(alignedPeaks.size() == 1){
            measuredMedian = measuredIntensity[alignedPeaks.get(0).get(0)];
            predictedMedian = predictedIntensity[alignedPeaks.get(0).get(1)];
        }
        else{
            
            double[] measuredAlignedIntensities = new double[alignedPeaks.size()];
            for (int i=0; i<measuredAlignedIntensities.length; i++){
                measuredAlignedIntensities[i] = measuredIntensity[alignedPeaks.get(i).get(0)];
            }
            Arrays.sort(measuredAlignedIntensities);
            
            double[] predictedAlignedIntensities = new double[alignedPeaks.size()];
            for (int i=0; i<measuredAlignedIntensities.length; i++){
                predictedAlignedIntensities[i] = predictedIntensity[alignedPeaks.get(i).get(1)];
            }
            Arrays.sort(predictedAlignedIntensities);
            
            if (alignedPeaks.size() % 2 == 0){
                measuredMedian = (measuredAlignedIntensities[alignedPeaks.size() / 2] + measuredAlignedIntensities[(alignedPeaks.size() / 2) - 1]) / 2.0;
                predictedMedian = (predictedAlignedIntensities[alignedPeaks.size() / 2] + predictedAlignedIntensities[(alignedPeaks.size() / 2) - 1]) / 2.0;
            }
            else{
                measuredMedian = measuredAlignedIntensities[alignedPeaks.size() / 2];
                predictedMedian = predictedAlignedIntensities[alignedPeaks.size() / 2];
            }
        }
        
        if (measuredMedian == 0.0){
            System.out.println("Measured median = 0.0");
        }
        if (predictedMedian == 0.0){
            System.out.println("Predicted median = 0.0");
            System.out.println(alignedPeaks.size());
            System.out.println(Arrays.toString(predictedIntensity));
        }
        
        double[] scaledMeasuredIntensity = new double[measuredIntensity.length];
        for (int i=0; i<scaledMeasuredIntensity.length; i++){
            scaledMeasuredIntensity[i] = measuredIntensity[i] / measuredMedian;
        }
        
        double[] scaledPredictedIntensity = new double[predictedIntensity.length];
        for (int i=0; i<scaledPredictedIntensity.length; i++){
            scaledPredictedIntensity[i] = predictedIntensity[i] / predictedMedian;
        }
        
        Spectrum scaledMeasuredSpectrum = new Spectrum(null, measuredSpectrum.mz, scaledMeasuredIntensity);
        Spectrum scaledPredictedSpectrum = new Spectrum(null, predictedSpectrum.mz, scaledPredictedIntensity);
        
        ArrayList<Spectrum> scaledSpectra = new ArrayList<>();
        scaledSpectra.add(scaledMeasuredSpectrum);
        scaledSpectra.add(scaledPredictedSpectrum);
        
        return scaledSpectra;
        
    }

 */
