package eu.isas.peptideshaker.stirred;

import com.compomics.software.log.CliLogger;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.fm_index.FMIndex;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.IdfileReaderFactory;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.stirred.modules.IdImport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class imports the results of a search engine from SearchGUI in the
 * utilities model.
 *
 * @author Marc Vaudel
 */
public class Stirred {

    private final File searchEngineResultsFile;
    private final File spectrumFile;
    private final File fastaFile;
    private final File ouputFile;
    private final File identificationParametersFile;
    private final File tempFolder;
    private final CliLogger cliLogger;
    private final WaitingHandler waitingHandler = new WaitingHandlerCLIImpl();

    public Stirred(
            File searchEngineResultsFile,
            File spectrumFile,
            File fastaFile,
            File ouputFile,
            File identificationParametersFile,
            File tempFolder,
            CliLogger cliLogger
    ) {

        this.searchEngineResultsFile = searchEngineResultsFile;
        this.spectrumFile = spectrumFile;
        this.fastaFile = fastaFile;
        this.ouputFile = ouputFile;
        this.identificationParametersFile = identificationParametersFile;
        this.tempFolder = tempFolder;
        this.cliLogger = cliLogger;

    }

    public void run() throws IOException {

        // Import identification parameters
        cliLogger.logMessage("Importing identification parameters file");
        IdentificationParameters identificationParameters = IdentificationParameters.getIdentificationParameters(identificationParametersFile);

        // Import fasta file
        cliLogger.logMessage("Importing protein sequences");
        FMIndex fmIndex = new FMIndex(
                fastaFile,
                identificationParameters.getFastaParameters(),
                waitingHandler,
                true,
                identificationParameters.getSearchParameters().getModificationParameters(),
                identificationParameters.getPeptideVariantsParameters()
        );

        // Import spectrum file
        cliLogger.logMessage("Importing spectra");
        MsFileHandler msFileHandler = new MsFileHandler();
        msFileHandler.register(
                fastaFile, 
                tempFolder, 
                waitingHandler
        );

        // Import identification results
        ArrayList<SpectrumMatch> spectrumMatches = IdImport.loadSpectrumMatches(
                searchEngineResultsFile, 
                identificationParameters, 
                msFileHandler, 
                cliLogger, 
                waitingHandler
        );
        
        // 
        
        
    }
}
