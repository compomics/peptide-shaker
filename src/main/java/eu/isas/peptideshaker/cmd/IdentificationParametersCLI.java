package eu.isas.peptideshaker.cmd;

import com.compomics.software.CompomicsWrapper;
import com.compomics.cli.identification_parameters.AbstractIdentificationParametersCli;
import org.apache.commons.cli.Options;

/**
 * The SearchParametersCLI allows creating search parameters files using command
 * line arguments.
 *
 * @author Marc Vaudel
 */
public class IdentificationParametersCLI extends AbstractIdentificationParametersCli {

    /**
     * Construct a new SearchParametersCLI runnable from a list of arguments.
     * When initialization is successful, calling "run" will write the created
     * parameters file.
     *
     * @param args the command line arguments
     */
    public IdentificationParametersCLI(String[] args) {
        initiate(args);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new IdentificationParametersCLI(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void createOptionsCLI(Options options) {
        PeptideShakerIdentificationParametersCLIParams.createOptionsCLI(options);
    }

    @Override
    protected String getOptionsAsString() {
        return PeptideShakerIdentificationParametersCLIParams.getOptionsAsString();
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("IdentificationParametersCLI.class").getPath(), "PeptideShaker");
    }
}
