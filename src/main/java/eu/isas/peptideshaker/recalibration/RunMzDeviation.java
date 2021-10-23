package eu.isas.peptideshaker.recalibration;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class computes the mz deviations for a a given run (i.e. file).
 *
 * @author Marc Vaudel
 */
public class RunMzDeviation {

    /**
     * The precursor slopes.
     */
    private HashMap<Double, Double> precursorSlopes = new HashMap<>();
    /**
     * The precursor offsets.
     */
    private HashMap<Double, Double> precursorOffsets = new HashMap<>();
    /**
     * The precursor RT list.
     */
    private ArrayList<Double> precursorRTList;
    /**
     * The fragments errors binned by mz and rt. error = experimental value -
     * theoretic (identification) value.
     */
    private HashMap<Double, TreeMap<Double, Double>> fragmentsRtDeviations = new HashMap<>();
    /**
     * The bin size used for ms2 correction.
     */
    private double ms2Bin;
    /**
     * The bin size in retention time in number of MS/MS spectra.
     */
    public static final int rtBinSize = 202;
    /**
     * The bin size in m/z in number of MS/MS spectra.
     */
    public static final int mzBinSize = 101;

    /**
     * Returns the list of precursor retention time bins.
     *
     * @return the list of precursor retention time bins
     */
    public ArrayList<Double> getPrecursorRTList() {
        return precursorRTList;
    }

    /**
     * Returns the list for fragment ion m/z bins at a given retention time
     * point.
     *
     * @param precursorRT the precursor retention time
     *
     * @return the list for fragment ion m/z bins
     */
    public ArrayList<Double> getFragmentMZList(
            double precursorRT
    ) {
        return new ArrayList<>(fragmentsRtDeviations.get(precursorRT).keySet());
    }

    /**
     * Returns the precursor m/z deviation slope at a given retention time
     * point.
     *
     * @param rtBin the retention time bin
     *
     * @return the precursor m/z deviation slope
     */
    public double getSlope(
            double rtBin
    ) {
        return precursorSlopes.get(rtBin);
    }

    /**
     * Returns the precursor m/z deviation offset at a given retention time
     * point.
     *
     * @param rtBin the retention time bin
     *
     * @return the precursor m/z deviation offset
     */
    public double getOffset(
            double rtBin
    ) {
        return precursorOffsets.get(rtBin);
    }

    /**
     * Returns an interpolation of the median error in the bins surrounding the
     * given precursor m/z when recalibrating with m/z only.
     *
     * @param precursorMz the precursor m/z
     * @param precursorRT the precursor retention time
     *
     * @return the median error
     */
    public double getPrecursorMzCorrection(
            double precursorMz,
            double precursorRT
    ) {

        double key1 = precursorRTList.get(0);
        double key2 = key1;

        if (precursorRT > key1) {
            key1 = precursorRTList.get(precursorRTList.size() - 1);
            key2 = key1;
            if (precursorRT < key1) {
                for (int i = 0; i < precursorRTList.size() - 1; i++) {
                    key1 = precursorRTList.get(i);
                    if (precursorRT == key1) {
                        key2 = precursorRT;
                        break;
                    }
                    key2 = precursorRTList.get(i + 1);
                    if (key1 < precursorRT && precursorRT < key2) {
                        break;
                    }
                }
            }
        }

        double slope = (precursorSlopes.get(key1) + precursorSlopes.get(key2)) / 2;
        double offset = (precursorOffsets.get(key1) + precursorOffsets.get(key2)) / 2;
        return slope * precursorMz + offset;

    }

    /**
     * Returns the fragment error at the given retention time and fragment m/z.
     *
     * @param precursorRT the precursor retention time
     * @param fragmentMZ the fragment m/z
     *
     * @return the error found
     */
    public double getFragmentMzError(
            double precursorRT,
            double fragmentMZ
    ) {

        double rtKey1 = precursorRTList.get(0);
        double rtKey2 = rtKey1;

        if (precursorRT > rtKey1) {
            rtKey1 = precursorRTList.get(precursorRTList.size() - 1);
            rtKey2 = rtKey1;
            if (precursorRT < rtKey1) {
                for (int i = 0; i < precursorRTList.size() - 1; i++) {
                    rtKey1 = precursorRTList.get(i);
                    if (precursorRT == rtKey1) {
                        rtKey2 = precursorRT;
                        break;
                    }
                    rtKey2 = precursorRTList.get(i + 1);
                    if (rtKey1 < precursorRT && precursorRT < rtKey2) {
                        break;
                    }
                }
            }
        }

        ArrayList<Double> mzList = new ArrayList<>(fragmentsRtDeviations.get(rtKey1).keySet());
        Collections.sort(mzList);
        double mzKey1 = mzList.get(0);
        double mzKey2 = mzKey1;

        if (fragmentMZ > mzKey1) {
            mzKey1 = mzList.get(mzList.size() - 1);
            mzKey2 = mzKey1;
            if (fragmentMZ < mzKey1) {
                for (int i = 0; i < mzList.size() - 1; i++) {
                    mzKey1 = mzList.get(i);
                    if (fragmentMZ == mzKey1) {
                        mzKey2 = fragmentMZ;
                        break;
                    }
                    mzKey2 = mzList.get(i + 1);
                    if (mzKey1 < fragmentMZ && fragmentMZ < mzKey2) {
                        break;
                    }
                }
            }
        }

        double correction11 = fragmentsRtDeviations.get(rtKey1).get(mzKey1);
        double correction12 = fragmentsRtDeviations.get(rtKey1).get(mzKey2);
        double correction1 = correction11 * mzKey1 / (mzKey1 + mzKey2) + correction12 * mzKey2 / (mzKey1 + mzKey2);

        mzList = new ArrayList<>(fragmentsRtDeviations.get(rtKey2).keySet());
        Collections.sort(mzList);
        mzKey1 = mzList.get(0);
        mzKey2 = mzKey1;

        if (fragmentMZ > mzKey1) {
            mzKey1 = mzList.get(mzList.size() - 1);
            mzKey2 = mzKey1;
            if (fragmentMZ < mzKey1) {
                for (int i = 0; i < mzList.size() - 1; i++) {
                    mzKey1 = mzList.get(i);
                    if (fragmentMZ == mzKey1) {
                        mzKey2 = fragmentMZ;
                        break;
                    }
                    mzKey2 = mzList.get(i + 1);
                    if (mzKey1 < fragmentMZ && fragmentMZ < mzKey2) {
                        break;
                    }
                }
            }
        }

        double correction21 = fragmentsRtDeviations.get(rtKey2).get(mzKey1);
        double correction22 = fragmentsRtDeviations.get(rtKey2).get(mzKey2);
        double correction2 = correction21 * mzKey1 / (mzKey1 + mzKey2) + correction22 * mzKey2 / (mzKey1 + mzKey2);

        return correction1 * rtKey1 / (rtKey1 + rtKey2) + correction2 * rtKey2 / (rtKey1 + rtKey2);
    }

    /**
     * Recalibrate an m/z array.
     *
     * @param precursorRT the precursor retention time
     * @param originalMz the original m/z
     *
     * @return the recalibrated m/z
     */
    public double[] recalibrateFragmentMz(
            double precursorRT,
            double[] originalMz
    ) {

        double[] result = new double[originalMz.length];

        for (int i = 0; i < originalMz.length; i++) {

            double mz = originalMz[i];
            double correction = getFragmentMzError(precursorRT, mz);
            result[i] = mz - correction;

        }

        return result;

    }

    /**
     * Creates a map of m/z deviations for a given run.
     *
     * @param spectrumFileNameWithoutExtension the name of the file of the run
     * @param identification the corresponding identification
     * @param sequenceProvider the protein sequence provider
     * @param spectrumProvider the spectrum provider
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler displaying the progress and
     * allowing the user to cancel the process. Can be null
     */
    public RunMzDeviation(
            String spectrumFileNameWithoutExtension,
            Identification identification,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler
    ) {

        AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();
        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
        ms2Bin = 100 * annotationPreferences.getFragmentIonAccuracy();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> precursorRawMap = new HashMap<>();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> fragmentRawMap = new HashMap<>();
        HashMap<Double, ArrayList<Double>> spectrumFragmentMap;

        PSParameter psParameter = new PSParameter();

        if (waitingHandler != null) {
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(spectrumProvider.getSpectrumTitles(spectrumFileNameWithoutExtension).length);
        }

        for (long spectrumKey : identification.getSpectrumIdentification().get(spectrumFileNameWithoutExtension)) {

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }

            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);

            String spectrumTitle = spectrumMatch.getSpectrumTitle();

            psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);

            if (psParameter.getMatchValidationLevel().isValidated()) {

                double precursorMz = spectrumProvider.getPrecursorMz(
                        spectrumFileNameWithoutExtension,
                        spectrumTitle
                );
                double precursorRT = spectrumProvider.getPrecursorRt(
                        spectrumFileNameWithoutExtension,
                        spectrumTitle
                );

                if (!precursorRawMap.containsKey(precursorRT)) {

                    precursorRawMap.put(precursorRT, new HashMap<>(1));

                }
                if (!precursorRawMap.get(precursorRT).containsKey(precursorMz)) {

                    precursorRawMap.get(precursorRT).put(precursorMz, new ArrayList<>(1));

                }

                PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

                if (bestPeptideAssumption != null) {

                    SearchParameters searchParameters = identificationParameters.getSearchParameters();
                    double error = bestPeptideAssumption.getDeltaMz(
                            precursorMz,
                            false,
                            searchParameters.getMinIsotopicCorrection(),
                            searchParameters.getMaxIsotopicCorrection()
                    );
                    precursorRawMap.get(precursorRT).get(precursorMz).add(error);

                    Spectrum spectrum = spectrumProvider.getSpectrum(
                            spectrumFileNameWithoutExtension,
                            spectrumTitle
                    );
                    ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                    SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                    SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(
                            spectrumFileNameWithoutExtension,
                            spectrumTitle,
                            bestPeptideAssumption,
                            modificationParameters,
                            sequenceProvider,
                            modificationSequenceMatchingParameters,
                            spectrumAnnotator
                    );
                    IonMatch[] ionMatches = spectrumAnnotator.getSpectrumAnnotation(
                            annotationPreferences,
                            specificAnnotationPreferences,
                            spectrumFileNameWithoutExtension,
                            spectrumTitle,
                            spectrum,
                            bestPeptideAssumption.getPeptide(),
                            modificationParameters,
                            sequenceProvider,
                            modificationSequenceMatchingParameters
                    );

                    spectrumFragmentMap = new HashMap<>();

                    for (IonMatch ionMatch : ionMatches) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        double fragmentMz = ionMatch.peakMz;
                        int roundedValue = (int) (fragmentMz / ms2Bin);
                        double fragmentMzKey = (double) roundedValue * ms2Bin;

                        if (!spectrumFragmentMap.containsKey(fragmentMzKey)) {
                            spectrumFragmentMap.put(fragmentMzKey, new ArrayList<>(1));
                        }

                        spectrumFragmentMap.get(fragmentMzKey).add(ionMatch.getAbsoluteError());
                    }

                    if (!fragmentRawMap.containsKey(precursorRT)) {
                        fragmentRawMap.put(precursorRT, new HashMap<>(1));
                    }

                    for (double key : spectrumFragmentMap.keySet()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        if (!fragmentRawMap.get(precursorRT).containsKey(key)) {
                            fragmentRawMap.get(precursorRT).put(key, new ArrayList<>(1));
                        }

                        fragmentRawMap.get(precursorRT)
                                .get(key)
                                .add(BasicMathFunctions.median(spectrumFragmentMap.get(key))
                                );
                    }
                }
            }

            if (waitingHandler != null) {
                waitingHandler.increaseSecondaryProgressCounter();
            }
        }

        if (waitingHandler != null) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        ArrayList<Double> keys = new ArrayList<>(precursorRawMap.keySet());
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("No validated PSM found for file " + spectrumFileNameWithoutExtension + ".");
        }
        Collections.sort(keys);
        int cpt1 = 0;
        TreeMap<Double, HashMap<Double, ArrayList<Double>>> precursorTempMap = new TreeMap<>();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> fragmentTempMap = new HashMap<>();

        for (double rt : keys) {

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }

            HashMap<Double, ArrayList<Double>> tempValues = precursorRawMap.get(rt);
            precursorTempMap.put(rt, tempValues);
            fragmentTempMap.put(rt, fragmentRawMap.get(rt));

            for (ArrayList<Double> errors : tempValues.values()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                cpt1 += errors.size();
            }

            if (cpt1 > rtBinSize) {

                double[] rtArray = precursorTempMap.keySet().stream()
                        .mapToDouble(a -> a)
                        .toArray();
                double rtRef = BasicMathFunctions.medianSorted(rtArray);
                TreeMap<Double, ArrayList<Double>> mzToErrorMap = new TreeMap<>();

                for (HashMap<Double, ArrayList<Double>> errors : precursorTempMap.values()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    for (double mz : errors.keySet()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        ArrayList<Double> errorsAtMz = errors.get(mz);

                        if (!mzToErrorMap.containsKey(mz)) {

                            mzToErrorMap.put(
                                    mz,
                                    new ArrayList<>(errorsAtMz.size())
                            );
                        }

                        mzToErrorMap.get(mz).addAll(errorsAtMz);
                    }
                }

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    return;
                }

                ArrayList<Double> mz1 = new ArrayList<>();
                ArrayList<Double> mz2 = new ArrayList<>();
                ArrayList<Double> err1 = new ArrayList<>();
                ArrayList<Double> err2 = new ArrayList<>();
                int cpt2 = 0;

                for (Entry<Double, ArrayList<Double>> entry : mzToErrorMap.entrySet()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    for (double err : entry.getValue()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        if (cpt2 < cpt1 / 2) {
                            mz1.add(entry.getKey());
                            err1.add(err);
                            cpt2++;
                        } else {
                            mz2.add(entry.getKey());
                            err2.add(err);
                        }
                    }
                }

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    return;
                }

                double x1 = BasicMathFunctions.median(mz1);
                double x2 = BasicMathFunctions.median(mz2);
                double y1 = BasicMathFunctions.median(err1);
                double y2 = BasicMathFunctions.median(err2);
                double slope;

                if (x1 == x2) {
                    slope = 0;
                } else {
                    slope = (y2 - y1) / (x2 - x1);
                }

                double offset = (y2 + y1 - slope * (x1 + x2)) / 2;
                precursorSlopes.put(rtRef, slope);
                precursorOffsets.put(rtRef, offset);

                fragmentsRtDeviations.put(rtRef, new TreeMap<>());
                mzToErrorMap = new TreeMap<>();

                for (HashMap<Double, ArrayList<Double>> errors : fragmentTempMap.values()) {
                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    for (double mz : errors.keySet()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        ArrayList<Double> errorsAtMz = errors.get(mz);

                        if (!mzToErrorMap.containsKey(mz)) {
                            mzToErrorMap.put(mz, new ArrayList<>(errorsAtMz.size()));
                        }

                        mzToErrorMap.get(mz).addAll(errorsAtMz);
                    }
                }

                mz1 = new ArrayList<>();
                mz2 = new ArrayList<>();
                err1 = new ArrayList<>();
                err2 = new ArrayList<>();
                double mzRef = -1;

                for (Entry<Double, ArrayList<Double>> entry : mzToErrorMap.entrySet()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    mz1.add(entry.getKey());
                    err1.addAll(entry.getValue());

                    if (err1.size() >= mzBinSize) {

                        mzRef = BasicMathFunctions.median(mz1);
                        double error = BasicMathFunctions.median(err1);
                        fragmentsRtDeviations.get(rtRef).put(mzRef, error);
                        mz2.clear();
                        err2.clear();
                        mz2.addAll(mz1);
                        err2.addAll(err1);
                        mz1.clear();
                        err1.clear();

                    }
                }

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    return;
                }

                if (!mz1.isEmpty()) {

                    if (fragmentsRtDeviations.get(rtRef) != null) {
                        fragmentsRtDeviations.get(rtRef).remove(mzRef);
                    }

                    mz1.addAll(mz2);
                    err1.addAll(err2);
                    mzRef = BasicMathFunctions.median(mz1);
                    double error = BasicMathFunctions.median(err1);
                    fragmentsRtDeviations.get(rtRef).put(mzRef, error);
                }

                for (double tempRt : rtArray) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    tempValues = precursorTempMap.get(tempRt);

                    for (ArrayList<Double> errors : tempValues.values()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        cpt1 -= errors.size();
                    }

                    precursorTempMap.remove(tempRt);
                    fragmentTempMap.remove(tempRt);

                    if (cpt1 <= rtBinSize) {
                        break;
                    }
                }
            }
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            return;
        }

        if (precursorSlopes.isEmpty()) {

            double rtRef = BasicMathFunctions.median(keys);
            TreeMap<Double, ArrayList<Double>> mzToErrorMap = new TreeMap<>();

            for (HashMap<Double, ArrayList<Double>> errors : precursorRawMap.values()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                for (double mz : errors.keySet()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    if (!mzToErrorMap.containsKey(mz)) {
                        mzToErrorMap.put(mz, new ArrayList<>());
                    }
                    mzToErrorMap.get(mz).addAll(errors.get(mz));
                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            ArrayList<Double> mz1 = new ArrayList<>();
            ArrayList<Double> mz2 = new ArrayList<>();
            ArrayList<Double> err1 = new ArrayList<>();
            ArrayList<Double> err2 = new ArrayList<>();
            int cpt2 = 0;

            for (Entry<Double, ArrayList<Double>> entry : mzToErrorMap.entrySet()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                for (double err : entry.getValue()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    if (cpt2 < cpt1 / 2) {
                        mz1.add(entry.getKey());
                        err1.add(err);
                        cpt2++;
                    } else {
                        mz2.add(entry.getKey());
                        err2.add(err);
                    }
                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            double x1 = BasicMathFunctions.median(mz1);
            double x2 = BasicMathFunctions.median(mz2);
            double y1 = BasicMathFunctions.median(err1);
            double y2 = BasicMathFunctions.median(err2);
            double slope;

            if (x1 == x2) {
                slope = 0;
            } else {
                slope = (y2 - y1) / (x2 - x1);
            }

            double offset = (y2 + y1 - slope * (x1 + x2)) / 2;
            precursorSlopes.put(rtRef, slope);
            precursorOffsets.put(rtRef, offset);

            for (double tempRt : keys) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                HashMap<Double, ArrayList<Double>> tempValues = precursorTempMap.get(tempRt);

                for (ArrayList<Double> errors : tempValues.values()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    cpt1 -= errors.size();
                }

                precursorTempMap.remove(tempRt);

                if (cpt1 <= rtBinSize) {
                    break;
                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            mzToErrorMap = new TreeMap<>();

            for (HashMap<Double, ArrayList<Double>> errors : precursorTempMap.values()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                for (double mz : errors.keySet()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    if (!mzToErrorMap.containsKey(mz)) {
                        mzToErrorMap.put(mz, new ArrayList<>());
                    }
                    mzToErrorMap.get(mz).addAll(errors.get(mz));
                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            fragmentsRtDeviations.put(rtRef, new TreeMap<>());

            mz1 = new ArrayList<>();
            mz2 = new ArrayList<>();
            err1 = new ArrayList<>();
            err2 = new ArrayList<>();

            for (Entry<Double, ArrayList<Double>> entry : mzToErrorMap.entrySet()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                mz1.add(entry.getKey());
                err1.addAll(entry.getValue());

                if (err1.size() >= mzBinSize) {

                    double mzRef = BasicMathFunctions.median(mz1);
                    double error = BasicMathFunctions.median(err1);
                    fragmentsRtDeviations.get(rtRef).put(mzRef, error);
                    mz2.addAll(mz1);
                    err2.addAll(err1);
                    mz1.clear();
                    err1.clear();

                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            if (!mz1.isEmpty()) {

                TreeMap<Double, Double> rtDeviationAtRtRef = fragmentsRtDeviations.get(rtRef);

                mz1.addAll(mz2);
                err1.addAll(err2);
                double mzRef = BasicMathFunctions.median(mz1);
                double error = BasicMathFunctions.median(err1);
                rtDeviationAtRtRef.put(mzRef, error);

            }
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            return;
        }

        precursorRTList = new ArrayList<>(precursorSlopes.keySet());
        Collections.sort(precursorRTList);

    }
}
