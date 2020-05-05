package eu.isas.peptideshaker.stirred.modules;

import com.compomics.software.log.CliLogger;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.IdfileReaderFactory;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;

/**
 * This class imports the ids from a file.
 *
 * @author Marc Vaudel
 */
public class IdImporter {
    
    private final File searchEngineResultsFile;
    private final IdfileReader idFileReader;
    
    public IdImporter(
            File searchEngineResultsFile,
            CliLogger cliLogger
    ) {
        
        this.searchEngineResultsFile = searchEngineResultsFile;

        IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();

        try {

            idFileReader = readerFactory.getFileReader(searchEngineResultsFile);

        } catch (OutOfMemoryError error) {

            String errorMessage = "Ran out of memory when parsing \'" + IoUtil.getFileName(searchEngineResultsFile) + "\'.";

            cliLogger.logError(errorMessage);

            throw new OutOfMemoryError(errorMessage);

        }

        if (idFileReader == null) {

            String errorMessage = "Identification result file \'" + IoUtil.getFileName(searchEngineResultsFile) + "\' not recognized.";

            cliLogger.logError(errorMessage);

            throw new IllegalArgumentException(errorMessage);

        }
    }

    public ArrayList<SpectrumMatch> loadSpectrumMatches(
            IdentificationParameters identificationParameters,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        try {

            return idFileReader.getAllSpectrumMatches(
                    spectrumProvider,
                    waitingHandler,
                    identificationParameters.getSearchParameters(),
                    identificationParameters.getSequenceMatchingParameters(),
                    true
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "An exception occurred when parsing " + searchEngineResultsFile + ".",
                    e
            );

        } finally {

            try {

                idFileReader.close();

            } catch (Exception e) {

                // Ignore
            }
        }
    }

    public IdfileReader getIdFileReader() {
        return idFileReader;
    }

    

}
