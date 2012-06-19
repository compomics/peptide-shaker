package eu.isas.peptideshaker.recalibration;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.math.BasicMathFunctions;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class computes the mz deviations for a a given file.
 *
 * @author Marc Vaudel
 */
public class FractionError {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The name of the spectrum file.
     */
    private String fileName;
    private HashMap<Double, Double> precursorGrades = new HashMap<Double, Double>();
    private HashMap<Double, Double> precursorOffsets = new HashMap<Double, Double>();
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
    public static final int mzBinSize = 101;

    public ArrayList<Double> getPrecursorRTList() {
        return precursorRTList;
    }

    public ArrayList<Double> getFragmentMZList(double precursorRT) {
        return new ArrayList<Double>(fragmentsRtDeviations.get(precursorRT).keySet());
    }

    public Double getGrade(Double rtBin) {
        return precursorGrades.get(rtBin);
    }

    public Double getOffset(Double rtBin) {
        return precursorOffsets.get(rtBin);
    }

    /**
     * Returns an interpolation of the median error in the bins surrounding the
     * given precursor m/z when recalibrating with mz only.
     *
     * @param precursorMz the precursor m/z
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

        double grade = (precursorGrades.get(key1) + precursorGrades.get(key2)) / 2;
        double offset = (precursorOffsets.get(key1) + precursorOffsets.get(key2)) / 2;
        return grade * precursorMz + offset;
    }

    /**
     * Returns the fragment error at the given retention time and framgent m/z.
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

    public HashMap<Double, Peak> recalibratePeakList(double precursorRT, HashMap<Double, Peak> originalPeakList) {
        HashMap<Double, Peak> recalibratedPeakList = new HashMap<Double, Peak>(originalPeakList.size());
        double correction, newMz;
        Peak peak;
        for (double mz : originalPeakList.keySet()) {
            correction = getFragmentMzError(precursorRT, mz);
            newMz = mz - correction;
            peak = new Peak(newMz, originalPeakList.get(mz).intensity);
            recalibratedPeakList.put(newMz, peak);
        }
        return recalibratedPeakList;
    }

    /**
     * Constructor, creates the map from the spectrum matches.
     *
     * @param peptideShakerGUI main instance of the GUI
     * @param fileName the name of the file of interest
     * @param progressDialog a dialog displaying progress to the user. Can be
     * null
     * @throws IOException
     * @throws MzMLUnmarshallerException
     */
    public FractionError(PeptideShakerGUI peptideShakerGUI, String fileName, ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        this.fileName = fileName;
        Identification identification = peptideShakerGUI.getIdentification();
        SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        PSParameter psParameter = new PSParameter();
        ms2Bin = 100 * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> precursorRawMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> fragmentRawMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, ArrayList<Double>> spectrumFragmentMap;
        double precursorMz, precursorRT, error, fragmentMz, fragmentMzKey;
        int roundedValue;
        Precursor precursor;

        for (String spectrumName : spectrumFactory.getSpectrumTitles(fileName)) {

            String spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumName);

            if (identification.matchExists(spectrumKey)) {
                try {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }

                if (psParameter.isValidated()) {

                    precursor = spectrumFactory.getPrecursor(spectrumKey, false);
                    precursorMz = precursor.getMz();
                    precursorRT = precursor.getRt();

                    if (!precursorRawMap.containsKey(precursorRT)) {
                        precursorRawMap.put(precursorRT, new HashMap<Double, ArrayList<Double>>());
                    }
                    if (!precursorRawMap.get(precursorRT).containsKey(precursorMz)) {
                        precursorRawMap.get(precursorRT).put(precursorMz, new ArrayList<Double>());
                    }

                    try {
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    error = spectrumMatch.getBestAssumption().getDeltaMass(precursorMz, false);
                    precursorRawMap.get(precursorRT).get(precursorMz).add(error);

                    MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                    ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(
                            annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            spectrumMatch.getBestAssumption().getIdentificationCharge().value,
                            currentSpectrum,
                            spectrumMatch.getBestAssumption().getPeptide(),
                            currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getFragmentIonAccuracy(), false);
                    spectrumFragmentMap = new HashMap<Double, ArrayList<Double>>();
                    for (IonMatch ionMatch : annotations) {
                        fragmentMz = ionMatch.peak.mz;
                        roundedValue = (int) (fragmentMz / ms2Bin);
                        fragmentMzKey = (double) roundedValue * ms2Bin;

                        if (!spectrumFragmentMap.containsKey(fragmentMzKey)) {
                            spectrumFragmentMap.put(fragmentMzKey, new ArrayList<Double>());
                        }

                        spectrumFragmentMap.get(fragmentMzKey).add(ionMatch.getAbsoluteError());
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    return;
                }

                    if (!fragmentRawMap.containsKey(precursorRT)) {
                        fragmentRawMap.put(precursorRT, new HashMap<Double, ArrayList<Double>>());
                    }
                    for (double key : spectrumFragmentMap.keySet()) {
                        if (!fragmentRawMap.get(precursorRT).containsKey(key)) {
                            fragmentRawMap.get(precursorRT).put(key, new ArrayList<Double>());
                        }
                        fragmentRawMap.get(precursorRT).get(key).add(BasicMathFunctions.median(spectrumFragmentMap.get(key)));
                    }
                }
            }

            if (progressDialog != null) {
                progressDialog.increaseProgressValue();
            }
        }

        ArrayList<Double> keys = new ArrayList<Double>(precursorRawMap.keySet());
        Collections.sort(keys);
        HashMap<Double, ArrayList<Double>> mzToErrorMap;
        ArrayList<Double> mzList, rtList;
        ArrayList<Double> mz1;
        ArrayList<Double> mz2;
        ArrayList<Double> err1;
        ArrayList<Double> err2;
        int cpt1 = 0, cpt2;
        double mzRef, rtRef, grade, offset, x1, x2, y1, y2;
        HashMap<Double, HashMap<Double, ArrayList<Double>>> precursorTempMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, HashMap<Double, ArrayList<Double>>> fragmentTempMap = new HashMap<Double, HashMap<Double, ArrayList<Double>>>();
        HashMap<Double, ArrayList<Double>> tempValues;

        for (double rt : keys) {
            tempValues = precursorRawMap.get(rt);
            precursorTempMap.put(rt, tempValues);
            fragmentTempMap.put(rt, fragmentRawMap.get(rt));
            for (ArrayList<Double> errors : tempValues.values()) {
                cpt1 += errors.size();
            }
            if (cpt1 > rtBinSize) {
                rtList = new ArrayList<Double>(precursorTempMap.keySet());
                Collections.sort(rtList);
                rtRef = BasicMathFunctions.median(rtList);
                mzToErrorMap = new HashMap<Double, ArrayList<Double>>();
                for (HashMap<Double, ArrayList<Double>> errors : precursorTempMap.values()) {
                    for (double mz : errors.keySet()) {
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
                cpt2 = 0;
                for (double mz : mzList) {
                    for (double err : mzToErrorMap.get(mz)) {
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
                x1 = BasicMathFunctions.median(mz1);
                x2 = BasicMathFunctions.median(mz2);
                y1 = BasicMathFunctions.median(err1);
                y2 = BasicMathFunctions.median(err2);
                if (x1 == x2) {
                    grade = 0;
                } else {
                    grade = (y2 - y1) / (x2 - x1);
                }
                offset = (y2 + y1 - grade * (x1 + x2)) / 2;
                precursorGrades.put(rtRef, grade);
                precursorOffsets.put(rtRef, offset);

                fragmentsRtDeviations.put(rtRef, new HashMap<Double, Double>());
                mzToErrorMap = new HashMap<Double, ArrayList<Double>>();
                for (HashMap<Double, ArrayList<Double>> errors : fragmentTempMap.values()) {
                    for (double mz : errors.keySet()) {
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
                mzRef = -1;
                for (double mz : mzList) {
                    mz1.add(mz);
                    err1.addAll(mzToErrorMap.get(mz));
                    if (err1.size() >= mzBinSize) {
                        mzRef = BasicMathFunctions.median(mz1);
                        error = BasicMathFunctions.median(err1);
                        fragmentsRtDeviations.get(rtRef).put(mzRef, error);
                        mz2.clear();
                        err2.clear();
                        mz2.addAll(mz1);
                        err2.addAll(err1);
                        mz1.clear();
                        err1.clear();
                    }
                }
                if (!mz1.isEmpty()) {
                    if (fragmentsRtDeviations.get(rtRef) != null) {
                        fragmentsRtDeviations.get(rtRef).remove(mzRef);
                    }
                    mz1.addAll(mz2);
                    err1.addAll(err2);
                    mzRef = BasicMathFunctions.median(mz1);
                    error = BasicMathFunctions.median(err1);
                    fragmentsRtDeviations.get(rtRef).put(mzRef, error);
                }

                for (double tempRt : rtList) {
                    tempValues = precursorTempMap.get(tempRt);
                    for (ArrayList<Double> errors : tempValues.values()) {
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

        if (precursorGrades.isEmpty()) {
            rtRef = BasicMathFunctions.median(keys);
            mzToErrorMap = new HashMap<Double, ArrayList<Double>>();
            for (HashMap<Double, ArrayList<Double>> errors : precursorRawMap.values()) {
                for (double mz : errors.keySet()) {
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
            cpt2 = 0;
            for (double mz : mzList) {
                for (double err : mzToErrorMap.get(mz)) {
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
            x1 = BasicMathFunctions.median(mz1);
            x2 = BasicMathFunctions.median(mz2);
            y1 = BasicMathFunctions.median(err1);
            y2 = BasicMathFunctions.median(err2);
            if (x1 == x2) {
                grade = 0;
            } else {
                grade = (y2 - y1) / (x2 - x1);
            }
            offset = (y2 + y1 - grade * (x1 + x2)) / 2;
            precursorGrades.put(rtRef, grade);
            precursorOffsets.put(rtRef, offset);


            for (double tempRt : keys) {
                tempValues = precursorTempMap.get(tempRt);
                for (ArrayList<Double> errors : tempValues.values()) {
                    cpt1 -= errors.size();
                }
                precursorTempMap.remove(tempRt);
                if (cpt1 <= rtBinSize) {
                    break;
                }
            }

            mzToErrorMap = new HashMap<Double, ArrayList<Double>>();
            for (HashMap<Double, ArrayList<Double>> errors : precursorTempMap.values()) {
                for (double mz : errors.keySet()) {
                    if (!mzToErrorMap.containsKey(mz)) {
                        mzToErrorMap.put(mz, new ArrayList<Double>());
                    }
                    mzToErrorMap.get(mz).addAll(errors.get(mz));
                }
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
                mz1.add(mz);
                err1.addAll(mzToErrorMap.get(mz));
                if (err1.size() >= mzBinSize) {
                    mzRef = BasicMathFunctions.median(mz1);
                    error = BasicMathFunctions.median(err1);
                    fragmentsRtDeviations.get(rtRef).put(mzRef, error);
                    mz2.addAll(mz1);
                    err2.addAll(err1);
                    mz1.clear();
                    err1.clear();
                }
            }
            if (!mz1.isEmpty()) {
                mzList = new ArrayList<Double>(fragmentsRtDeviations.get(rtRef).keySet());
                Collections.sort(mzList);
                fragmentsRtDeviations.remove(mzList.get(mzList.size() - 1));
                mz1.addAll(mz2);
                err1.addAll(err2);
                mzRef = BasicMathFunctions.median(mz1);
                error = BasicMathFunctions.median(err1);
                fragmentsRtDeviations.get(rtRef).put(mzRef, error);
            }
        }

        precursorRTList = new ArrayList<Double>(precursorGrades.keySet());
        Collections.sort(precursorRTList);

    }
}
