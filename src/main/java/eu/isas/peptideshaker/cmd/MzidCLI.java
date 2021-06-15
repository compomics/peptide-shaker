package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.utils.PsdbParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Command line interface to export mzid files from psdb files.
 *
 * @author Marc Vaudel
 */
public class MzidCLI extends PsdbParent {

    /**
     * The mzid creation options.
     */
    private MzidCLIInputBean mzidCLIInputBean = null;
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The PTM factory.
     */
    private ModificationFactory ptmFactory;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserParameters utilitiesUserPreferences;

    /**
     * Construct a new MzidCLI runnable from a MzidCLI input bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired mzid file.
     *
     * @param mzidCLIInputBean the mzId creation options
     */
    public MzidCLI(MzidCLIInputBean mzidCLIInputBean) {
        this.mzidCLIInputBean = mzidCLIInputBean;
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     *
     * @return returns 1 if the process was canceled
     */
    public Object call() {

        // turn off illegal access log messages
        try {
            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Long offset = (Long) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, loggerField);
            unsafeClass.getMethod("putObjectVolatile", Object.class, long.class, Object.class) //
                    .invoke(unsafe, loggerClass, offset, null);
        } catch (Throwable ex) {
            // ignore, i.e. simply show the warnings...
            //ex.printStackTrace();
        }

        setDbFolder(PeptideShaker.getMatchesFolder());

        // Load user preferences
        utilitiesUserPreferences = UtilitiesUserParameters.loadUserParameters();

        // Instantiate factories
        PeptideShaker.instantiateFacories(utilitiesUserPreferences);
        ptmFactory = ModificationFactory.getInstance();
        enzymeFactory = EnzymeFactory.getInstance();

        // Load resources files
        loadSpecies();

        waitingHandler = new WaitingHandlerCLIImpl();

        String inputFilePath = null;

        try {
            if (mzidCLIInputBean.getZipFile() != null) {
                inputFilePath = mzidCLIInputBean.getZipFile().getAbsolutePath();
                loadPsdbFromZipFile(mzidCLIInputBean.getZipFile(), PeptideShaker.getMatchesFolder(), waitingHandler);
            } else if (mzidCLIInputBean.getPsdbFile() != null) {
                inputFilePath = mzidCLIInputBean.getPsdbFile().getAbsolutePath();
                psdbFile = mzidCLIInputBean.getPsdbFile();
                loadPsdbFile(PeptideShaker.getMatchesFolder(), waitingHandler, false);
            } else {
                waitingHandler.appendReport("PeptideShaker project input missing.", true, true);
                return 1;
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

        // If not available on the computer, parse summary information about the fasta file
        try {

            loadFastaFile(waitingHandler);

        } catch (IOException e) {

            e.printStackTrace();
            waitingHandler.appendReport("An error occurred while parsing the fasta file.", true, true);
            waitingHandler.setRunCanceled();
            return 1;

        }

        // Load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // export mzid file
        // make sure that all annotations are included
        double currentIntensityLimit = this.getIdentificationParameters().getAnnotationParameters().getAnnotationIntensityLimit();
        this.getIdentificationParameters().getAnnotationParameters().setIntensityLimit(0.0);

        try {
            CLIExportMethods.exportMzId(mzidCLIInputBean, this, waitingHandler);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while generating the mzid file.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } finally {
            // reset the annotation level
            this.getIdentificationParameters().getAnnotationParameters().setIntensityLimit(currentIntensityLimit);
        }

        try {
            PeptideShakerCLI.closePeptideShaker(identification);
        } catch (Exception e2) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e2.printStackTrace();
        }

        if (!waitingHandler.isRunCanceled()) {
            waitingHandler.appendReport("MzIdentML export completed.", true, true);
            System.exit(0);
            return 0;
        } else {
            System.exit(1);
            return 1;
        }
    }

    /**
     * PeptideShaker mzid CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker mzid command line takes a psdb file and export the identification results in the mzIdentML format." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see https://compomics.github.io/projects/peptide-shaker.html and https://compomics.github.io/projects/peptide-shaker/wiki/PeptideshakerCLI.html." + System.getProperty("line.separator")
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
        for (MzidCLIParams mzidCLIParam : MzidCLIParams.values()) {
            if (mzidCLIParam.mandatory && mzidCLIParam.hasArgument
                    && (!aLine.hasOption(mzidCLIParam.id) || ((String) aLine.getOptionValue(mzidCLIParam.id)).equals(""))) {
                System.out.println("\n" + mzidCLIParam.description + " not specified.\n");
                return false;
            }
        }
        String fileTxt = aLine.getOptionValue(MzidCLIParams.PSDB_FILE.id);
        File testFile = new File(fileTxt.trim());
        if (!testFile.exists()) {
            System.out.println("\n" + MzidCLIParams.PSDB_FILE.description + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
            return false;
        }

        return true;
    }

    /**
     * Loads the species from the species file into the species factory.
     */
    private void loadSpecies() {
        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(PeptideShaker.getJarFilePath());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the species.");
            e.printStackTrace();
        }
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
            MzidCLIParams.createOptionsCLI(nonPathOptions);
            DefaultParser parser = new DefaultParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "========================================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker mzid - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("========================================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(MzidCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                MzidCLIInputBean lCLIBean = new MzidCLIInputBean(line);
                MzidCLI mzidCLI = new MzidCLI(lCLIBean);
                mzidCLI.call();
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
        return "MzidCLI{"
                + ", cliInputBean=" + mzidCLIInputBean
                + '}';
    }
}
