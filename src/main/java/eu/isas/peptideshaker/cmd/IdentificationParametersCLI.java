package eu.isas.peptideshaker.cmd;

import com.compomics.cli.identification_parameters.AbstractIdentificationParametersCli;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.waiting.WaitingHandler;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The SearchParametersCLI allows creating search parameters files using command
 * line arguments.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class IdentificationParametersCLI extends AbstractIdentificationParametersCli {

    /**
     * The waiting handler.
     */
    private WaitingHandler waitingHandler;

    /**
     * Construct a new SearchParametersCLI runnable from a list of arguments.
     * When initialization is successful, calling "run" will write the created
     * parameters file.
     *
     * @param args the command line arguments
     */
    public IdentificationParametersCLI(String[] args) {

        try {

            waitingHandler = new WaitingHandlerCLIImpl();

            // check if there are updates to the paths
            String[] nonPathSettingArgsAsList = PathSettingsCLI.extractAndUpdatePathOptions(args);
            initiate(nonPathSettingArgsAsList);

        } catch (ParseException ex) {

            waitingHandler.appendReport("An error occurred while running the command line.", true, true);
            ex.printStackTrace();

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

}
