package eu.isas.peptideshaker.followup;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SimpleNoiseDistribution;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPOutputStream;

/**
 * Export for ms2pip training files.
 *
 * @author Marc Vaudel
 */
public class Ms2pipExport {

    /**
     * A handler for the exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * A waiting handler providing feedback to the user and allowing canceling
     * the process.
     */
    private WaitingHandler waitingHandler;
    /**
     * The end of line separator.
     */
    public static final String END_LINE = System.getProperty("line.separator");
    /**
     * The columns separator.
     */
    public final static char separator = ' ';

    public final static String fileName = "ms2pip";
    /**
     * Encoding for the file according to the second rule.
     */
    public static final String encoding = "UTF-8";
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    private BufferedWriter[] bufferedWriters;

    private Semaphore[] semaphores;

    public Ms2pipExport(AnnotationSettings annotationSettings, File destinationFolder, Identification identification) throws IOException {

        BufferedWriter[] bufferedWriters = new BufferedWriter[2];

        for (int i = 0; i < 2; i++) {
            int index = i + 1;
            File destinationFile = new File(destinationFolder, fileName + "_" + index);
            FileOutputStream fileStream = new FileOutputStream(destinationFile);
            GZIPOutputStream gzipStream = new GZIPOutputStream(fileStream);
            OutputStreamWriter encoder = new OutputStreamWriter(gzipStream, encoding);
            BufferedWriter bw = new BufferedWriter(encoder);
            bufferedWriters[i] = bw;
            semaphores[i] = new Semaphore(1);
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PsmIterator psmIterator = new PsmIterator(identification, parameters, false, waitingHandler);

    }

    private void writeLine(int index, int[] features, int intensity) {

    }

    /**
     * Private runnable to process a sequence.
     */
    private class PsmProcessor implements Runnable {

        Identification identification;

        private PsmIterator psmIterator;

        private PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();

        private AnnotationSettings annotationSettings;

        private SequenceMatchingPreferences sequenceMatchingPreferences;

        private SequenceMatchingPreferences ptmSequenceMatchingPreferences;

        @Override
        public void run() {

            try {

                PSParameter psParameter = new PSParameter();

                SpectrumMatch spectrumMatch;
                while ((spectrumMatch = psmIterator.next()) != null) {

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (peptideAssumption != null) {

                        String spectrumKey = spectrumMatch.getKey();

                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                        if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {

                            SpecificAnnotationSettings specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);

                            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                            Peptide peptide = peptideAssumption.getPeptide();
                            int charge = peptideAssumption.getIdentificationCharge().value;

                            ArrayList<IonMatch> ionMatches = spectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, spectrum, peptide, false);

                            SimpleNoiseDistribution binnedCumulativeFunction = spectrum.getIntensityLogDistribution();

                            for (IonMatch ionMatch : ionMatches) {

                                double intensity = ionMatch.peak.intensity;

                                Ion ion = ionMatch.ion;

                                if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {

                                    int index;
                                    switch (ion.getSubType()) {
                                        case PeptideFragmentIon.A_ION:
                                        case PeptideFragmentIon.B_ION:
                                        case PeptideFragmentIon.C_ION:
                                            index = 0;
                                            break;
                                        case PeptideFragmentIon.X_ION:
                                        case PeptideFragmentIon.Y_ION:
                                        case PeptideFragmentIon.Z_ION:
                                            index = 1;
                                            break;
                                        default:
                                            throw new UnsupportedOperationException("Peptide fragment ion of type " + ion.getSubTypeAsString() + " not supported.");
                                    }

                                    double pMinusLog = -binnedCumulativeFunction.getBinnedCumulativeProbabilityLog(intensity);

                                }

                            }

                        }

                    }

                }

            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

    }

}
