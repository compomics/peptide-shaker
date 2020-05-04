package eu.isas.peptideshaker.stirred.modules;

import com.compomics.software.log.CliLogger;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationProvider;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.scores.PhosphoRS;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.IdfileReaderFactory;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class imports the ids from a file.
 *
 * @author Marc Vaudel
 */
public class IdImport {

    public static ArrayList<SpectrumMatch> loadSpectrumMatches(
            File searchEngineResultsFile,
            IdentificationParameters identificationParameters,
            SpectrumProvider spectrumProvider,
            CliLogger cliLogger,
            WaitingHandler waitingHandler
    ) {

        IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();

        IdfileReader fileReader = null;
        try {

            fileReader = readerFactory.getFileReader(searchEngineResultsFile);

        } catch (OutOfMemoryError error) {

            String errorMessage = "Ran out of memory when parsing \'" + IoUtil.getFileName(searchEngineResultsFile) + "\'.";

            cliLogger.logError(errorMessage);

            throw new OutOfMemoryError(errorMessage);

        }

        if (fileReader == null) {

            String errorMessage = "Identification result file \'" + IoUtil.getFileName(searchEngineResultsFile) + "\' not recognized.";

            cliLogger.logError(errorMessage);

            throw new IllegalArgumentException(errorMessage);

        }

        try {

            return fileReader.getAllSpectrumMatches(
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

                fileReader.close();

            } catch (Exception e) {

                // Ignore
            }
        }
    }

    public static void mapModifications(
            SpectrumMatch spectrumMatch
    ) {
        
        

    }

}
