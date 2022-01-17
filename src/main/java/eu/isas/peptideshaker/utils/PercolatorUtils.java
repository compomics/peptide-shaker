package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.modifications.Modification;
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
            header.append("\t").append("measured_rt");
            header.append("\t").append("rt_Abs_error");
            header.append("\t").append("rt_Square_error");
            header.append("\t").append("rt_Log_error");
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
                        "scaled_predicted_rt"
                )
        );
        
        return header.toString();
    
    }

    /**
     * Gets the peptide data to provide to percolator.
     *
     * @param spectrumMatch The spectrum match where the peptide was found.
     * @param peptideAssumption The peptide assumption.
     * @param rtPredictionsAvailable Flag indicating whether retention time
     * predictions are given.
     * @param peptideRTs The retention time predictions for this peptide.
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
            ArrayList<Double> peptideRTs,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            ModificationParameters modificationParameters
    ) {

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
        double measuredMz = spectrumProvider.getPrecursorMz(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());
        line.append("\t").append(measuredMz);

        double deltaMz = peptideAssumption.getDeltaMz(
                measuredMz,
                searchParameters.isPrecursorAccuracyTypePpm(),
                searchParameters.getMinIsotopicCorrection(),
                searchParameters.getMaxIsotopicCorrection()
        );

        line.append("\t").append(deltaMz);

        // pep
        PSParameter psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
        double pep = psParameter.getProbability();
        line.append("\t").append(pep);
        double deltaPep = psParameter.getDeltaPEP();
        line.append("\t").append(deltaPep);

        // Ion fraction
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
                peptide,
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

            boolean n = false;
            boolean c = false;
            boolean nc = false;

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

                line.append("\t").append('1');
                line.append("\t").append('0');
                line.append("\t").append('0');
                line.append("\t").append('0');

            } else if (nc) {

                line.append("\t").append('0');
                line.append("\t").append('0');
                line.append("\t").append('0');
                line.append("\t").append('1');

            } else {

                char nChar = n ? '1' : '0';
                char cChar = c ? '1' : '0';

                line.append("\t").append('0');
                line.append("\t").append(nChar);
                line.append("\t").append(cChar);
                line.append("\t").append('0');

            }
        }

        // Retention time
        if (rtPredictionsAvailable) {

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
            double rtSqrError = Math.pow((predictedRT - measuredRt),2);
            double rtLogError = Math.log(predictedRT) - Math.log(measuredRt);
            
            line.append("\t").append(measuredRt);
            line.append("\t").append(rtAbsError);
            line.append("\t").append(rtSqrError);
            line.append("\t").append(rtLogError);

        }
        
        //Peptide sequence
        
        String peptideSequence = getSequenceWithModifications(peptideAssumption.getPeptide(), modificationParameters, sequenceProvider, sequenceMatchingParameters, modificationFactory);
        
        line.append("\t").append("-." + peptideSequence + ".-");
        
        line.append("\t").append("-");

        return line.toString();

    }
    
    /**
     * Returns the sequence of the peptides with modifications encoded as required by Percolator.
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
        String sequenceWithMods = "";

        // Fixed modifications
        String[] fixedModifications = peptide.getFixedModifications(modificationParameters, sequenceProvider, sequenceMatchingParameters);

        ArrayList<String> fixedModificationsInfo = new ArrayList<String>();
        
        for (int i = 0; i < fixedModifications.length; i++) {

            if (fixedModifications[i] != null) {

                //int site = i < peptideSequence.length() + 1 ? i : -1;

                String modName = fixedModifications[i];
                Modification modification = modificationFactory.getModification(modName);
                CvTerm cvTerm = modification.getUnimodCvTerm();
                String accession = cvTerm.getAccession();

                if (cvTerm == null) {

                    throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

                }
                        
                //String seqBegin = sequenceWithMods.substring(0,site);
                //String seqEnd = sequenceWithMods.substring(site);
                //sequenceWithMods = seqBegin + "[" + accession + "]" + seqEnd;
                
                fixedModificationsInfo.add("[" + accession.substring(accession.indexOf(":")+1) + "]");

            }
            else{
                fixedModificationsInfo.add("");
            }

        }

        // Variable modifications
        String[] variableModifications = peptide.getIndexedVariableModifications();
        
        ArrayList<String> variableModificationsInfo = new ArrayList<String>();
        
        for (int i = 0; i < variableModifications.length; i++) {

            if (variableModifications[i] != null) {

                //int site = i < peptideSequence.length() + 1 ? i : -1;

                String modName = variableModifications[i];
                Modification modification = modificationFactory.getModification(modName);
                CvTerm cvTerm = modification.getUnimodCvTerm();
                String accession = cvTerm.getAccession();

                if (cvTerm == null) {

                    throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

                }

                //String seqBegin = sequenceWithMods.substring(0,site);
                //String seqEnd = sequenceWithMods.substring(site);
                //sequenceWithMods = seqBegin + "[" + accession + "]" + seqEnd;
                
                variableModificationsInfo.add("[" + accession.substring(accession.indexOf(":")+1) + "]");

            }
            else{
                variableModificationsInfo.add("");
            }

        }
        
        for (int i=0; i < peptideSequence.length(); i++){
            
            String modificationsInfo = fixedModificationsInfo.get(i) + variableModificationsInfo.get(i);
            sequenceWithMods = sequenceWithMods + modificationsInfo + peptideSequence.charAt(i);
            
        }

        return sequenceWithMods;

    }
    
    public static String getPeptideRTData(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            ArrayList<Double> peptideRTs,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider
    ){
        
        StringBuilder line = new StringBuilder();

        // PSM id
        long spectrumKey = spectrumMatch.getKey();
        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();
        
        Peptide peptide = peptideAssumption.getPeptide();
        long peptideKey = peptide.getMatchingKey();
        line.append(spectrumKey).append("_").append(peptideKey);
        
        // Get measured spectrum
        Spectrum measuredSpectrum = spectrumProvider.getSpectrum(spectrumFile, spectrumTitle);
        measuredSpectrum.

        // Label
        String decoyFlag = PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider) ? "-1" : "1";
        line.append("\t").append(decoyFlag);
        
        /*double measuredRt = spectrumProvider.getPrecursorRt(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());
        line.append("\t").append(measuredRt);
        
        int bestRTindex = 0;
        double minRTdistance = Math.abs(predictedRts.get(0) - measuredRt);
        for (int i=1; i<predictedRts.size(); i++){
            if (Math.abs(predictedRts.get(i) - measuredRt) < minRTdistance){
                bestRTindex = i;
            }
        }
        double bestRTprediction = predictedRts.get(bestRTindex);
        line.append("\t").append(bestRTprediction);*/
        
        line.append("\t").append(peptideRTs.get(0));
        line.append("\t").append(peptideRTs.get(1));
        line.append("\t").append(peptideRTs.get(2));
        line.append("\t").append(peptideRTs.get(3));
        
        return line.toString();
    }
    
    public static ArrayList<Double> getPeptideObservedPredictedRT(
            SpectrumMatch spectrumMatch,
            ArrayList<Double> predictedRts,
            SpectrumProvider spectrumProvider
    ){
        ArrayList<Double> rtValues = new ArrayList<>();
        
        double measuredRt = spectrumProvider.getPrecursorRt(spectrumMatch.getSpectrumFile(), spectrumMatch.getSpectrumTitle());
        
        int bestRTindex = 0;
        double minRTdistance = Math.abs(predictedRts.get(0) - measuredRt);
        for (int i=1; i<predictedRts.size(); i++){
            if (Math.abs(predictedRts.get(i) - measuredRt) < minRTdistance){
                bestRTindex = i;
            }
        }
        double bestRTprediction = predictedRts.get(bestRTindex);
        rtValues.add(measuredRt);
        rtValues.add(bestRTprediction);
        
        return rtValues;
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

}
