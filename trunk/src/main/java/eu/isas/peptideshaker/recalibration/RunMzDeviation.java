package eu.isas.peptideshaker.recalibration;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.math.BasicMathFunctions;
import eu.isas.peptideshaker.myparameters.PSParameter;
import com.compomics.util.preferences.AnnotationPreferences;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class computes the mz deviations for a a given run (i.e. file).
 *
 * @author Marc Vaudel
 */
public class RunMzDeviation {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The precursor slopes.
     */
    private HashMap<Double, Double> precursorSlopes = new HashMap<Double, Double>();
    /**
     * The precursor offsets.
     */
    private HashMap<Double, Double> precursorOffsets = new HashMap<Double, Double>();
    /**
     * The precursor RT list.
     */
    private ArrayList<Double> precursorRTList;
    /**
     * The fragments errors binned by mz and rt. error = experimental value -
     * theoretic (identification) value.
     */
    private HashMap<Double, HashMap<Double, Double>> fragmentsRtDeviations = new HashMap<Double, HashMap<Double, Double>>();
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
     * @return the list for fragment ion m/z bins
     */
    public ArrayList<Double> getFragmentMZList(double precursorRT) {
        return new ArrayList<Double>(fragmentsRtDeviations.get(precursorRT).keySet());
    }

    /**
     * Returns the precursor m/z deviation slope at a given retention time
     * point.
     *
     * @param rtBin the retention time bin
     * @return the precursor m/z deviation slope
     */
    public Double getSlope(Double rtBin) {
        return precursorSlopes.get(rtBin);
    }

    /**
     * Returns the precursor m/z deviation offset at a given retention time
     * point.
     *
     * @param rtBin the retention time bin
     * @return the precursor m/z deviation offset
     */
    public Double getOffset(Double rtBin) {
        return precursorOffsets.get(rtBin);
    }

    /**
     * Returns an interpolation of the median error in the bins surrounding the
     * given precursor m/z when recalibrating with mz only.
     *
     * @param precursorMz the precursor m/z
     * @param precursorRT
     * @return the median error
     */
    public double getPrecursorMzCorrection(Double precursorMz, Double precursorRT) {

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

        double grade = (precursorSlopes.get(key1) + precursorSlopes.get(key2)) / 2;
        double offset = (precursorOffsets.get(key1) + precursorOffsets.get(key2)) / 2;
        return grade * precursorMz + offset;
    }

    /**
     * Returns the fragment error at the given retention time and fragment m/z.
     *
     * @param precursorRT the precursor retention time
     * @param fragmentMZ the fragment m/z
     * @return the error found
     */
    public Double getFragmentMzError(double precursorRT, double fragmentMZ) {

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

        ArrayList<Double> mzList = new ArrayList<Double>(fragmentsRtDeviations.get(rtKey1).keySet());
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

        mzList = new ArrayList<Double>(fragmentsRtDeviations.get(rtKey2).keySet());
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
     * Recalibrate a peak list.
     *
     * @param precursorRT
     * @param originalPeakList
     * @return the recalibrated peak list
     */
    public HashMap<Double, Peak> recalibratePeakList(double precursorRT, HashMap<Double, Peak> originalPeakList) {
        HashMap<Double, Peak> recalibratedPeakList = new HashMap<Double, Peak>(originalPeakList.size());

        for (double mz : originalPeakList.keySet()) {
            double correction = getFragmentMzError(precursorRT, mz);
            double newMz = mz - correction;
            Peak peak = new Peak(newMz, originalPeakList.get(mz).intensity);
            recalibratedPeakList.put(newMz, peak);
        }
        return recalibratedPeakList;
    }

    /**
     * Creates a map of m/z deviations for a given run.
     *
     * @param spectrumFileName the name of the file of the run
     * @param identification the corresponding identification
     * @param annotationPreferences the annotation preferences to be used
     * @param waitingHandler a waiting handler displaying the progress and
     * allowing the user to cancel the process. Can be null
     *
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public RunMzDeviation(String spectrumFileName, Identification identification, AnnotationPreferences annotationPreferences,
            WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        // @TODO: the progress bar usage in the code below could be improved
        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
        PSParameter psParameter = new PSParameter();
        ms2Bin = 100 * annotationPreferences.getFragmentIonAccuracy();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> precursorRawMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> fragmentRawMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, ArrayList<Double>> spectrumFragmentMap;

        identification.loadSpectrumMatchParameters(spectrumFileName, psParameter, waitingHandler);
        identification.loadSpectrumMatches(spectrumFileName, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(spectrumFactory.getSpectrumTitles(spectrumFileName).size());
        }

        for (String spectrumName : spectrumFactory.getSpectrumTitles(spectrumFileName)) {

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }

            String spectrumKey = Spectrum.getSpectrumKey(spectrumFileName, spectrumName);

            if (identification.matchExists(spectrumKey)) {

                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {

                    Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);
                    double precursorMz = precursor.getMz();
                    double precursorRT = precursor.getRt();

                    if (!precursorRawMap.containsKey(precursorRT)) {
                        precursorRawMap.put(precursorRT, new HashMap<Double, ArrayList<Double>>());
                    }
                    if (!precursorRawMap.get(precursorRT).containsKey(precursorMz)) {
                        precursorRawMap.get(precursorRT).put(precursorMz, new ArrayList<Double>());
                    }

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    double error = spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursorMz, false);
                    precursorRawMap.get(precursorRT).get(precursorMz).add(error);

                    MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                    ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(
                            annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
                            currentSpectrum,
                            spectrumMatch.getBestPeptideAssumption().getPeptide(),
                            currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getFragmentIonAccuracy(), false, annotationPreferences.isHighResolutionAnnotation());
                    spectrumFragmentMap = new HashMap<Double, ArrayList<Double>>();

                    for (IonMatch ionMatch : annotations) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        double fragmentMz = ionMatch.peak.mz;
                        int roundedValue = (int) (fragmentMz / ms2Bin);
                        double fragmentMzKey = (double) roundedValue * ms2Bin;

                        if (!spectrumFragmentMap.containsKey(fragmentMzKey)) {
                            spectrumFragmentMap.put(fragmentMzKey, new ArrayList<Double>());
                        }

                        spectrumFragmentMap.get(fragmentMzKey).add(ionMatch.getAbsoluteError());
                    }

                    if (!fragmentRawMap.containsKey(precursorRT)) {
                        fragmentRawMap.put(precursorRT, new HashMap<Double, ArrayList<Double>>());
                    }

                    for (double key : spectrumFragmentMap.keySet()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        if (!fragmentRawMap.get(precursorRT).containsKey(key)) {
                            fragmentRawMap.get(precursorRT).put(key, new ArrayList<Double>());
                        }

                        fragmentRawMap.get(precursorRT).get(key).add(BasicMathFunctions.median(spectrumFragmentMap.get(key)));
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

        ArrayList<Double> keys = new ArrayList<Double>(precursorRawMap.keySet());
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("No validated PSM found for file " + spectrumFileName + ".");
        }
        Collections.sort(keys);
        int cpt1 = 0;
        HashMap<Double, HashMap<Double, ArrayList<Double>>> precursorTempMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> fragmentTempMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();

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

            if (cpt1 > rtBinSize && waitingHandler != null && !waitingHandler.isRunCanceled()) {

                ArrayList<Double> rtList = new ArrayList<Double>(precursorTempMap.keySet());
                Collections.sort(rtList);
                double rtRef = BasicMathFunctions.median(rtList);
                HashMap<Double, ArrayList<Double>> mzToErrorMap = new HashMap<Double, ArrayList<Double>>();

                for (HashMap<Double, ArrayList<Double>> errors : precursorTempMap.values()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    for (double mz : errors.keySet()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        if (!mzToErrorMap.containsKey(mz)) {
                            mzToErrorMap.put(mz, new ArrayList<Double>());
                        }

                        mzToErrorMap.get(mz).addAll(errors.get(mz));
                    }
                }

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    return;
                }

                ArrayList<Double> mzList = new ArrayList<Double>(mzToErrorMap.keySet());
                Collections.sort(mzList);
                ArrayList<Double> mz1 = new ArrayList<Double>();
                ArrayList<Double> mz2 = new ArrayList<Double>();
                ArrayList<Double> err1 = new ArrayList<Double>();
                ArrayList<Double> err2 = new ArrayList<Double>();
                int cpt2 = 0;

                for (double mz : mzList) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    for (double err : mzToErrorMap.get(mz)) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        if (cpt2 < cpt1 / 2) {
                            mz1.add(mz);
                            err1.add(err);
                            cpt2++;
                        } else {
                            mz2.add(mz);
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

                fragmentsRtDeviations.put(rtRef, new HashMap<Double, Double>());
                mzToErrorMap = new HashMap<Double, ArrayList<Double>>();

                for (HashMap<Double, ArrayList<Double>> errors : fragmentTempMap.values()) {
                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    for (double mz : errors.keySet()) {

                        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                            break;
                        }

                        if (!mzToErrorMap.containsKey(mz)) {
                            mzToErrorMap.put(mz, new ArrayList<Double>());
                        }

                        mzToErrorMap.get(mz).addAll(errors.get(mz));
                    }
                }

                mzList = new ArrayList<Double>(mzToErrorMap.keySet());
                Collections.sort(mzList);
                mz1 = new ArrayList<Double>();
                mz2 = new ArrayList<Double>();
                err1 = new ArrayList<Double>();
                err2 = new ArrayList<Double>();
                double mzRef = -1;

                for (double mz : mzList) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    mz1.add(mz);
                    err1.addAll(mzToErrorMap.get(mz));

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

                for (double tempRt : rtList) {

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
            HashMap<Double, ArrayList<Double>> mzToErrorMap = new HashMap<Double, ArrayList<Double>>();

            for (HashMap<Double, ArrayList<Double>> errors : precursorRawMap.values()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                for (double mz : errors.keySet()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    if (!mzToErrorMap.containsKey(mz)) {
                        mzToErrorMap.put(mz, new ArrayList<Double>());
                    }
                    mzToErrorMap.get(mz).addAll(errors.get(mz));
                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            ArrayList<Double> mzList = new ArrayList<Double>(mzToErrorMap.keySet());
            Collections.sort(mzList);
            ArrayList<Double> mz1 = new ArrayList<Double>();
            ArrayList<Double> mz2 = new ArrayList<Double>();
            ArrayList<Double> err1 = new ArrayList<Double>();
            ArrayList<Double> err2 = new ArrayList<Double>();
            int cpt2 = 0;

            for (double mz : mzList) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                for (double err : mzToErrorMap.get(mz)) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    if (cpt2 < cpt1 / 2) {
                        mz1.add(mz);
                        err1.add(err);
                        cpt2++;
                    } else {
                        mz2.add(mz);
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

            mzToErrorMap = new HashMap<Double, ArrayList<Double>>();

            for (HashMap<Double, ArrayList<Double>> errors : precursorTempMap.values()) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                for (double mz : errors.keySet()) {

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }

                    if (!mzToErrorMap.containsKey(mz)) {
                        mzToErrorMap.put(mz, new ArrayList<Double>());
                    }
                    mzToErrorMap.get(mz).addAll(errors.get(mz));
                }
            }

            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                return;
            }

            fragmentsRtDeviations.put(rtRef, new HashMap<Double, Double>());
            mzList = new ArrayList<Double>(mzToErrorMap.keySet());
            Collections.sort(mzList);
            Collections.sort(mzList);
            mz1 = new ArrayList<Double>();
            mz2 = new ArrayList<Double>();
            err1 = new ArrayList<Double>();
            err2 = new ArrayList<Double>();

            for (double mz : mzList) {

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }

                mz1.add(mz);
                err1.addAll(mzToErrorMap.get(mz));
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
                mzList = new ArrayList<Double>(fragmentsRtDeviations.get(rtRef).keySet());
                Collections.sort(mzList);
                if (!mzList.isEmpty()) {
                    fragmentsRtDeviations.remove(mzList.get(mzList.size() - 1));
                }
                mz1.addAll(mz2);
                err1.addAll(err2);
                double mzRef = BasicMathFunctions.median(mz1);
                double error = BasicMathFunctions.median(err1);
                fragmentsRtDeviations.get(rtRef).put(mzRef, error);
            }
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            return;
        }

        precursorRTList = new ArrayList<Double>(precursorSlopes.keySet());
        Collections.sort(precursorRTList);
    }
}
