package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.utils.CpsParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Command line interface to get mgf index files from psdb or mgf files.
 *
 * @author Marc Vaueel
 * @author Harald Barsnes
 * @author Carlos horro
 */
public class MgfIndexCLI extends CpsParent {

    /**
     * The mgf index creation options.
     */
    private MgfIndexCLIInputBean mgfIndexCLIInputBean = null;
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    

    /**
     * Construct a new MgfIndexCLI runnable from a MgfIndexCLI input bean. When
     * initialization is successful, calling "run" with a PeptideShaker project 
     * will open it, get its spectra and write its indexes. Callin "run" with 
     * specific spectrum files will just use SpectrumFactory to generate their
     * indexes and write them.
     *
     * @param mgfIndexCLIInputBean the mgf index creation options
     */
    public MgfIndexCLI(MgfIndexCLIInputBean mgfIndexCLIInputBean) {
        this.mgfIndexCLIInputBean = mgfIndexCLIInputBean;
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     *
     * @return returns 1 if the process was canceled
     */
    public Object call() {
        
        waitingHandler = new WaitingHandlerCLIImpl();

        String inputFilePath = null;
        
        try {
            if (mgfIndexCLIInputBean.getInputZipFile() != null) {
                inputFilePath = mgfIndexCLIInputBean.getInputZipFile().getAbsolutePath();
                loadCpsFromZipFile(mgfIndexCLIInputBean.getInputZipFile(), PeptideShaker.getMatchesFolder(), waitingHandler);
            } else if (mgfIndexCLIInputBean.getInputPsdbFile() != null) {
                inputFilePath = mgfIndexCLIInputBean.getInputPsdbFile().getAbsolutePath();
                cpsFile = mgfIndexCLIInputBean.getInputPsdbFile();
                loadCpsFile(PeptideShaker.getMatchesFolder(), waitingHandler);
            } else {
                if ( (mgfIndexCLIInputBean.getSpectrumFiles() == null) ){
                    waitingHandler.appendReport("PeptideShaker project input and spectrum files are missing.", true, true);
                    return 1;
                } 
            }
        } catch (IOException e) {
            waitingHandler.appendReport("An error occurred while reading: " + inputFilePath + ".", true, true);
            e.printStackTrace();
            try {
                PeptideShakerCLI.closePeptideShaker(identification);
            } catch (Exception e2) {
                waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                e2.printStackTrace();
            }
            return 1;
        }
        // psdb or zip files...
        if ( mgfIndexCLIInputBean.getInputZipFile() != null || mgfIndexCLIInputBean.getInputPsdbFile() != null ){
            // load the spectrum files
            try {
                
                if (!loadSpectrumFiles(waitingHandler)) {
                    if (identification.getFractions().size() > 1) {
                        waitingHandler.appendReport("The spectrum files were not found. Please provide their location in the command line parameters.", true, true);
                    } else {
                        waitingHandler.appendReport("The spectrum file was not found. Please provide its location in the command line parameters.", true, true);
                    }
                    try {
                        PeptideShakerCLI.closePeptideShaker(identification);
                    } catch (Exception e2) {
                        waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                        e2.printStackTrace();
                    }
                    return 1;
                }
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while loading the spectrum file(s).", true, true);
                e.printStackTrace();
                try {
                    PeptideShakerCLI.closePeptideShaker(identification);
                } catch (Exception e2) {
                    waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                    e2.printStackTrace();
                }
                return 1;
            }
            
        }else{  // specified spectrum files...
            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            ArrayList<File> spectrumFiles = mgfIndexCLIInputBean.getSpectrumFiles();
            try{

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Getting Spectrum Files. Please Wait...");
                    waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                    waitingHandler.setSecondaryProgressCounter(0);
                    waitingHandler.setMaxSecondaryProgressCounter(spectrumFiles.size());
                }
                for(File spectrumFile : spectrumFiles){
                    spectrumFactory.addSpectra(spectrumFile, waitingHandler);
                }  
            }catch (IOException e) {
                waitingHandler.appendReport("An error occurred while loading spectra.", true, true);
                e.printStackTrace();
                try {
                    PeptideShakerCLI.closePeptideShaker(identification);
                } catch (Exception e2) {
                    waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                    e2.printStackTrace();
                }
                return 1;
            }
        }    

        try {
            CLIExportMethods.exportMgfIndex(mgfIndexCLIInputBean, this, waitingHandler);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while creating the index files.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } 

        try {
            PeptideShakerCLI.closePeptideShaker(identification);
        } catch (Exception e2) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e2.printStackTrace();
        }

        if (!waitingHandler.isRunCanceled()) {
            waitingHandler.appendReport("Mgf indexes export completed.", true, true);
            System.exit(0);
            return 0;
        } else {
            System.exit(1);
            return 1;
        }
    }

    /**
     * PeptideShaker mgf index CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker mgf index command line takes a psdb file or mgf files and exports their index files." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see https://compomics.github.io/projects/peptide-shaker.html and https://compomics.github.io/projects/peptide-shaker/wiki/peptideshakercli.html." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/peptide-shaker." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator");
    }

    /**
     * Verifies the command line start parameters.
     *
     * @return true if the startup was valid
     */
    private static boolean isValidStartup(CommandLine aLine) throws IOException {

        if (aLine.getOptions().length == 0) {
            return false;
        }
        for (MgfIndexCLIParams mgfIndexCLIParam : MgfIndexCLIParams.values()) {
            if (mgfIndexCLIParam.mandatory && mgfIndexCLIParam.hasArgument
                    && (!aLine.hasOption(mgfIndexCLIParam.id) || ((String) aLine.getOptionValue(mgfIndexCLIParam.id)).equals(""))) {
                System.out.println("\n" + mgfIndexCLIParam.description + " not specified.\n");
                return false;
            }
        }
        
        
        String psdbFileTxt = aLine.getOptionValue(MgfIndexCLIParams.PSDB_FILE.id);
        File psdbTestFile = null;
        if (psdbFileTxt!=null)
            psdbTestFile = new File(psdbFileTxt.trim());
        
        if (aLine.hasOption(MgfIndexCLIParams.PSDB_FILE.id) && aLine.hasOption(MgfIndexCLIParams.SPECTRUM_FILES.id)){
            System.out.println("\n'" + MgfIndexCLIParams.PSDB_FILE.id + "\' and '"+MgfIndexCLIParams.SPECTRUM_FILES.id +"' options cannot be chosen together.\n");
            return false;
        }
        
        if (!aLine.hasOption(MgfIndexCLIParams.PSDB_FILE.id) && !aLine.hasOption(MgfIndexCLIParams.SPECTRUM_FILES.id)){
            System.out.println("\n'" + MgfIndexCLIParams.PSDB_FILE.id + "\' or '"+MgfIndexCLIParams.SPECTRUM_FILES.id +"' options must be used.\n");
            return false;
        }
        
        if (aLine.hasOption(MgfIndexCLIParams.EXPORT_FOLDER.id) && aLine.hasOption(MgfIndexCLIParams.EXPORT_ZIP.id)){
            System.out.println("\n'" + MgfIndexCLIParams.EXPORT_FOLDER.id + "\' and '"+MgfIndexCLIParams.EXPORT_ZIP.id +"' options cannot be chosen together.\n");
            return false;
        }
        
        if (!aLine.hasOption(MgfIndexCLIParams.EXPORT_FOLDER.id) && !aLine.hasOption(MgfIndexCLIParams.EXPORT_ZIP.id)){
            System.out.println("\n'" + MgfIndexCLIParams.EXPORT_FOLDER.id + "\' or '"+MgfIndexCLIParams.EXPORT_ZIP.id +"' options must be used.\n");
            return false;
        }
        
        if (aLine.hasOption(MgfIndexCLIParams.PSDB_FILE.id) && (psdbTestFile == null || !psdbTestFile.exists()) ) {
            System.out.println("\n" + MgfIndexCLIParams.PSDB_FILE.id + " \'" + psdbTestFile.getAbsolutePath() + "\' not found.\n");
            return false;
        }
        
        if (aLine.hasOption(MgfIndexCLIParams.EXPORT_FOLDER.id)){
            if (((String) aLine.getOptionValue(MgfIndexCLIParams.EXPORT_FOLDER.id)).equals("")) {
                System.out.println("\n" + MgfIndexCLIParams.EXPORT_FOLDER.id + " not properly specified.\n");
                return false;
            } else {
                String fileTxt = aLine.getOptionValue(MgfIndexCLIParams.EXPORT_FOLDER.id);
                File testFile = new File(fileTxt.trim());
                if (!testFile.exists()) {
                    System.out.println("\n" + MgfIndexCLIParams.EXPORT_FOLDER.id + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
                    return false;
                }
            }
        }
        
        
        if (aLine.hasOption(MgfIndexCLIParams.SPECTRUM_FILES.id) ) {
            String spectrumFilesTxt = aLine.getOptionValue(MgfIndexCLIParams.SPECTRUM_FILES.id);
            try{
                ArrayList<File> spectrumFiles = MgfIndexCLIInputBean.getSpectrumFiles(spectrumFilesTxt);
            }catch(IOException ioe){
                System.out.println("\n" + MgfIndexCLIParams.SPECTRUM_FILES.id + " not properly specified or without any file.\n");
                return false;

            }
        }
        
        
        
        

        return true;
    }

    
    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            // check if there are updates to the paths
            String[] nonPathSettingArgsAsList = PathSettingsCLI.extractAndUpdatePathOptions(args);

            // parse the rest of the cptions   
            Options nonPathOptions = new Options();
            MgfIndexCLIParams.createOptionsCLI(nonPathOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "========================================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker mgf index - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("========================================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(MgfIndexCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                MgfIndexCLIInputBean lCLIBean = new MgfIndexCLIInputBean(line);
                MgfIndexCLI mgfIndexCLI = new MgfIndexCLI(lCLIBean);
                mgfIndexCLI.call();
            }
        } catch (OutOfMemoryError e) {
            System.out.println("<CompomicsError>PeptideShaker used up all the memory and had to be stopped. See the PeptideShaker log for details.</CompomicsError>");
            System.err.println("Ran out of memory!");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.print("<CompomicsError>PeptideShaker processing failed. See the PeptideShaker log for details.</CompomicsError>");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "MgfIndexCLI{"
                + ", cliInputBean=" + mgfIndexCLIInputBean
                + '}';
    }
}
