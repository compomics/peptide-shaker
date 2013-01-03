package eu.isas.peptideshaker.cmd;

import com.compomics.software.ToolFactory;
import java.io.File;
import javax.swing.JOptionPane;

/**
 * A simple class that can be used to open a PeptideShaker project in
 * PeptideShaker from the command line, e.g., java -cp PeptideShaker-X.Y.Z.jar 
 * eu.isas.peptideshaker.cmd.PeptideShakerCLIOpenProject "path_to_cps_file".
 *
 * @author Harald Barsnes
 */
public class PeptideShakerCLIOpenProject {

    /**
     * Open a PeptideShaker project in PeptideShaker from the command line.
     *
     * @param args the command line arguments, a single cps file only
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Incorrect number of arguments. Please provide one cps file.");
            return;
        }

        if (!new File(args[0]).exists()) {
            System.out.println("The file does not exist. Please provide a cps file.");
            return;
        }

        if (!args[0].endsWith(".cps")) {
            System.out.println("The file is not a cps file. Please provide a valid cps file.");
            return;
        }

        try {
            ToolFactory.startPeptideShaker(null, new File(args[0]));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An exception occurred while opening the cps file: " + e.getLocalizedMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
