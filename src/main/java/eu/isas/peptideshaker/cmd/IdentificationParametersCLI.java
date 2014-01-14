/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.peptideshaker.cmd;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.experiment.identification.search_parameters_cli.AbstractIdentificationParametersCli;
import eu.isas.peptideshaker.PeptideShaker;
import java.io.File;
import org.apache.commons.cli.Options;

/**
 * The SearchParametersCLI allows creating search parameters files using command line arguments.
 *
 * @author Marc
 */
public class IdentificationParametersCLI extends AbstractIdentificationParametersCli {
    
    
    /**
     * Construct a new SearchParametersCLI runnable from a list of arguments. When
     * initialization is successful, calling "run" will
     * write the created parameters file.
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
    protected File getModificationsFile() {
        return new File(getJarFilePath(), PeptideShaker.MODIFICATIONS_FILE);
    }

    @Override
    protected File getUserModificationsFile() {
        return new File(getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE);
    }

    @Override
    protected File getEnzymeFile() {
        return new File(getJarFilePath(), PeptideShaker.ENZYME_FILE);
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
