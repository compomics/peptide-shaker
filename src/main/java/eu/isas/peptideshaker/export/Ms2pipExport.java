package eu.isas.peptideshaker.export;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
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
        
        PsmIterator psmIterator = new PsmIterator(identification, false, waitingHandler);
        
        
    }

    /**
     * Private runnable to process a sequence.
     */
    private class PsmProcessor implements Runnable {
        
        private  PsmIterator psmIterator;

        @Override
        public void run() {
            
            try {
            
            SpectrumMatch spectrumMatch;
            while ((spectrumMatch = psmIterator.next()) != null) {
                
            }
            
            }catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

    }

}
