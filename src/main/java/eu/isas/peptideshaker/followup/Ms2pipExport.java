package eu.isas.peptideshaker.followup;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.peptide_fragmentation.models.ms2pip.features_configuration.FeaturesMap;
import com.compomics.util.experiment.identification.peptide_fragmentation.models.ms2pip.features_configuration.Ms2pipFeature;
import com.compomics.util.experiment.identification.peptide_fragmentation.models.ms2pip.features_generation.FeaturesGenerator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SimpleNoiseDistribution;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

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
    /**
     * The columns separator.
     */
    public final static String documentationSeparator = "\t";

    public final static String fileName = "ms2pip_targets";
    public final static String documentationFileName = "features_description.txt";
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

    private FeaturesMap featuresMap;

    public Ms2pipExport() {

    }

    public void exportFeatures(IdentificationParameters identificationParameters, File destinationFolder, Identification identification, FeaturesMap featuresMap, int nThreads) throws IOException, InterruptedException {

        this.featuresMap = featuresMap;
        
        writeDocumentation(destinationFolder);

        BufferedWriter[] bufferedWriters = new BufferedWriter[2];

        String header = getHeaderLine();

        for (int i = 0; i < 2; i++) {
            int index = i + 1;
            File destinationFile = getFeaturesFile(destinationFolder, index);
            FileOutputStream fileStream = new FileOutputStream(destinationFile);
            GZIPOutputStream gzipStream = new GZIPOutputStream(fileStream);
            OutputStreamWriter encoder = new OutputStreamWriter(gzipStream, encoding);
            BufferedWriter bw = new BufferedWriter(encoder);
            bufferedWriters[i] = bw;
            semaphores[i] = new Semaphore(1);
            writeLine(index, header);
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PsmIterator psmIterator = new PsmIterator(identification, parameters, false, waitingHandler);

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);

        for (int i = 0; i < nThreads; i++) {
            PsmProcessor psmProcessor = new PsmProcessor(identification, psmIterator, identificationParameters);
            pool.submit(psmProcessor);
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("Features extraction timed out. Please contact the developers.");
        }

        for (BufferedWriter bw : bufferedWriters) {
            bw.close();
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        packageResults(destinationFolder);

    }

    private void packageResults(File destinationFolder) throws IOException {

        File destinationFile = new File(destinationFolder, fileName);

        FileOutputStream fos = new FileOutputStream(destinationFile);

        try {
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            try {
                ZipOutputStream out = new ZipOutputStream(bos);

                try {

                    File documentationFile = getDocumentationFile(destinationFolder);
                    ZipUtils.addFileToZip(documentationFile, out);

                    for (int i = 0; i < 2; i++) {
                        int index = i + 1;
                        File featuresFile = getFeaturesFile(destinationFolder, index);
                        ZipUtils.addFileToZip(featuresFile, out);
                    }

                } finally {
                    out.close();
                }
            } finally {
                bos.close();
            }
        } finally {
            fos.close();
        }
    }

    private void writeDocumentation(File destinationFolder) throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(getDocumentationFile(destinationFolder)));

        StringBuilder stringBuilder = new StringBuilder();

        int index = 0;

        for (String category : featuresMap.getSortedFeaturesList()) {

            for (Ms2pipFeature ms2pipFeature : featuresMap.getFeatures(category)) {

                stringBuilder.append(index).append(documentationSeparator).append(category).append(documentationSeparator).append(ms2pipFeature.getDescription()).append(END_LINE);
                bw.write(stringBuilder.toString());
                stringBuilder = new StringBuilder(stringBuilder.length());

            }
        }
        bw.close();
    }

    public static File getDocumentationFile(File destinationFolder) {
        return new File(destinationFolder, documentationFileName);
    }

    public static File getFeaturesFile(File destinationFolder, int index) {
        return new File(destinationFolder, fileName + "_" + index);
    }

    private void writeLine(int index, String line) throws InterruptedException, IOException {

        BufferedWriter bw = bufferedWriters[index];

        Semaphore semaphore = semaphores[index];

        semaphore.acquire();

        bw.write(line);

        semaphore.release();
    }

    private String getHeaderLine() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("psmid").append(separator).append("target");

        int index = 0;

        for (String category : featuresMap.getSortedFeaturesList()) {

            int nFeatures = featuresMap.getFeatures(category).length;

            for (int i = 0; i < nFeatures; i++) {

                stringBuilder.append(separator).append(index++);

            }
        }

        stringBuilder.append(END_LINE);

        return stringBuilder.toString();
    }

    private String getLine(String psmId, double target, int[] features) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(psmId).append(separator).append(target);
        for (int feature : features) {
            stringBuilder.append(separator).append(feature);
        }
        stringBuilder.append(END_LINE);
        return stringBuilder.toString();
    }

    /**
     * Private runnable to process a sequence.
     */
    private class PsmProcessor implements Runnable {

        private Identification identification;

        private PsmIterator psmIterator;

        private PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();

        private AnnotationSettings annotationSettings;

        private SequenceMatchingPreferences sequenceMatchingPreferences;

        private SequenceMatchingPreferences ptmSequenceMatchingPreferences;

        private FeaturesGenerator featuresGenerator;
        
        private PsmProcessor(Identification identification, PsmIterator psmIterator, IdentificationParameters identificationParameters) {
            
            this.identification = identification;
            this.psmIterator = psmIterator;
            this.annotationSettings = identificationParameters.getAnnotationPreferences();
            this.sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
            this.ptmSequenceMatchingPreferences = identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences();
            featuresGenerator = new FeaturesGenerator(featuresMap);
        }

        @Override
        public void run() {

            try {

                PSParameter psParameter = new PSParameter();

                SpectrumMatch spectrumMatch;
                while ((spectrumMatch = psmIterator.next()) != null && !waitingHandler.isRunCanceled()) {

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

                                    PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ion;

                                    int index;
                                    int aaIndex;
                                    int[] features;
                                    switch (peptideFragmentIon.getSubType()) {
                                        case PeptideFragmentIon.A_ION:
                                        case PeptideFragmentIon.B_ION:
                                        case PeptideFragmentIon.C_ION:
                                            index = 1;
                                            aaIndex = peptideFragmentIon.getNumber() - 1;
                                            features = featuresGenerator.getForwardIonsFeatures(peptide, charge, aaIndex);
                                            break;
                                        case PeptideFragmentIon.X_ION:
                                        case PeptideFragmentIon.Y_ION:
                                        case PeptideFragmentIon.Z_ION:
                                            index = 2;
                                            aaIndex = peptide.getSequence().length() - peptideFragmentIon.getNumber();
                                            features = featuresGenerator.getComplementaryIonsFeatures(peptide, charge, aaIndex);
                                            break;
                                        default:
                                            throw new UnsupportedOperationException("Peptide fragment ion of type " + ion.getSubTypeAsString() + " not supported.");
                                    }

                                    double pMinusLog = -binnedCumulativeFunction.getBinnedCumulativeProbabilityLog(intensity);

                                    String line = getLine(spectrumKey, pMinusLog, features);

                                    writeLine(index, line);

                                }

                            }

                        }

                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }

            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

    }

}
