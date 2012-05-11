package eu.isas.peptideshaker.recalibration;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.dialogs.ProgressDialogX;
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
    /**
     * The precursor errors binned by 100 mz error = experimental value -
     * theoretic (identification) value.
     */
    private HashMap<Double, ArrayList<Double>> precursorDeviations = new HashMap<Double, ArrayList<Double>>();
    /**
     * The fragments errors binned by 100 mz error = experimental value -
     * theoretic (identification) value.
     */
    private HashMap<Double, ArrayList<Double>> fragmentsDeviations = new HashMap<Double, ArrayList<Double>>();
    /**
     * The bin size used for ms2 correction.
     */
    private double ms2Bin;
    /**
     * The precursor bin size -1.
     */
    public static final int precBinSize = 200;
    /**
     * The precursor bins.
     */
    private ArrayList<Double> precursorKeys;

    /**
     * Returns the bins used for the precursor error binning.
     * 
     * @return the bins used for the precursor error binning
     */
    public ArrayList<Double> getPrecursorBins() {
        return precursorKeys;
    }

    /**
     * Returns the precursor errors in the given bin.
     *
     * @param bin the bin
     * @return the precursor errors
     */
    public ArrayList<Double> getPrecursorErrors(double bin) {
        return precursorDeviations.get(bin);
    }

    /**
     * Returns the bins used for the fragment ions binning.
     * 
     * @return the bins used for the fragment ions binning
     */
    public ArrayList<Double> getFragmentBins() {
        ArrayList<Double> result = new ArrayList<Double>(fragmentsDeviations.keySet());
        Collections.sort(result);
        return result;
    }

    /**
     * Returns an interpolation of the median error in the bins surrounding the
     * given precursor m/z.
     *
     * @param precursorMz the precursor m/z
     * @return the median error
     */
    public double getPrecursorCorrection(Double precursorMz) {
        double key = precursorKeys.get(0);
        
        if (precursorMz <= key) {
            return BasicMathFunctions.median(precursorDeviations.get(key));
        }
        
        key = precursorKeys.get(precursorKeys.size() - 1);
        
        if (precursorMz >= key) {
            return BasicMathFunctions.median(precursorDeviations.get(key));
        }
        
        for (int i = 0; i < precursorKeys.size() - 1; i++) {
            
            key = precursorKeys.get(i);
            
            if (key == precursorMz) {
                return BasicMathFunctions.median(precursorDeviations.get(key));
            }
            
            double key1 = precursorKeys.get(i + 1);
            
            if (key < precursorMz && precursorMz < key1) {
                double y1 = BasicMathFunctions.median(precursorDeviations.get(key));
                double y2 = BasicMathFunctions.median(precursorDeviations.get(key1));
                return y1 + ((precursorMz - key) * (y2 - y1) / (key1 - key));
            }
        }
        
        throw new IllegalArgumentException("Precursor m/z not found.");
    }

    /**
     * Returns the error found in fragment m/z in the given bin.
     *
     * @param bin the bin
     * @return the errors found
     */
    public ArrayList<Double> getFragmentErrors(Double bin) {
        return fragmentsDeviations.get(bin);
    }

    /**
     * Returns a map of the median of the error in every bin.
     *
     * @return a map of the median of the error in every bin
     */
    public HashMap<Double, Double> getFragmentCorrections() {
        HashMap<Double, Double> result = new HashMap<Double, Double>();
        for (Double key : fragmentsDeviations.keySet()) {
            ArrayList<Double> errors = fragmentsDeviations.get(key);
            result.put(key, BasicMathFunctions.median(errors));
        }
        return result;
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
        HashMap<Double, ArrayList<Double>> precursorRawMap = new HashMap<Double, ArrayList<Double>>();
        HashMap<Double, ArrayList<Double>> fragmentRawMap;
        
        for (String spectrumName : spectrumFactory.getSpectrumTitles(fileName)) {
            
            String spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumName);
            
            if (identification.matchExists(spectrumKey)) {
                
                psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
                
                if (psParameter.isValidated()) {
                    
                    double precursorMz = spectrumFactory.getPrecursor(spectrumKey, false).getMz();
                    
                    if (!precursorRawMap.containsKey(precursorMz)) {
                        precursorRawMap.put(precursorMz, new ArrayList<Double>());
                    }
                    
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    double error = spectrumMatch.getBestAssumption().getDeltaMass(precursorMz, false);
                    precursorRawMap.get(precursorMz).add(error);

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
                    fragmentRawMap = new HashMap<Double, ArrayList<Double>>();
                    for (IonMatch ionMatch : annotations) {
                        double fragmentMz = ionMatch.peak.mz;
                        int roundedValue = (int) (fragmentMz / ms2Bin);
                        double key = (double) roundedValue * ms2Bin;
                        
                        if (!fragmentRawMap.containsKey(key)) {
                            fragmentRawMap.put(key, new ArrayList<Double>());
                        }
                        
                        fragmentRawMap.get(key).add(ionMatch.getAbsoluteError());
                    }
                    
                    for (double key : fragmentRawMap.keySet()) {
                        if (!fragmentsDeviations.containsKey(key)) {
                            fragmentsDeviations.put(key, new ArrayList<Double>());
                        }
                        fragmentsDeviations.get(key).add(BasicMathFunctions.median(fragmentRawMap.get(key)));
                    }
                    
                }
            }
            
            if (progressDialog != null) {
                progressDialog.incrementValue();
            }
        }
        
        ArrayList<Double> keys = new ArrayList<Double>(precursorRawMap.keySet());
        Collections.sort(keys);
        double key = BasicMathFunctions.median(keys);
        ArrayList<Double> tempList = new ArrayList<Double>();
        ArrayList<Double> tempKeys = new ArrayList<Double>();
        
        for (double mz : keys) {
            if (tempList.size() > precBinSize) {
                key = BasicMathFunctions.median(tempKeys);
                precursorDeviations.put(key, tempList);
                tempList = new ArrayList<Double>();
                tempKeys = new ArrayList<Double>();
            }
            for (double currentError : precursorRawMap.get(mz)) {
                tempKeys.add(mz);
                tempList.add(currentError);
            }
        }
        
        precursorDeviations.get(key).addAll(tempList);
        precursorKeys = new ArrayList<Double>(precursorDeviations.keySet());
        Collections.sort(precursorKeys);

        // merge fragment groups < 100 items
        tempList = new ArrayList<Double>();
        Double previousKey = null;
        keys = new ArrayList<Double>(fragmentsDeviations.keySet());
        Collections.sort(keys);
        
        for (Double fragmentBin : keys) {
            
            fragmentsDeviations.get(fragmentBin).addAll(tempList);
            
            if (fragmentsDeviations.get(fragmentBin).size() < 100) {
                
                if (previousKey == null) {
                    tempList.clear();
                    tempList.addAll(fragmentsDeviations.get(fragmentBin));
                } else {
                    fragmentsDeviations.get(previousKey).addAll(fragmentsDeviations.get(fragmentBin));
                }
                
                fragmentsDeviations.remove(fragmentBin);
                
            } else {
                if (!tempList.isEmpty()) {
                    tempList.clear();
                }
                previousKey = fragmentBin;
            }
        }
        
        if (!tempList.isEmpty()) {
            fragmentsDeviations.put(0.0, tempList);
            tempList.clear();
        }
    }
}
