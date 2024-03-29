package eu.isas.peptideshaker.export;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Export peptide sequences to the Unipept web interface.
 *
 * @author Tim Van Den Bossche
 * @author Pieter Verschaffelt
 * @author Thilo Muth
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class UnipeptExport {

    /**
     * The line break type.
     */
    private static final String LINE_BREAK = System.getProperty("line.separator");

    /**
     * Analyze the given list of peptides, with the specified configuration,
     * using the Unipept web interface.
     *
     * @param peptides a list of peptides that should be analyzed using Unipept
     * @param equateIandL true if the amino acids I and L should be equated
     * @param filterDuplicates true if duplicate peptide sequences should be
     * filtered
     * @param handleMissingCleavage true if missing cleavage handling should be
     * enabled
     * @param tempHtmlFile the temporary file for Unipept HTML forwarding
     * @param waitingHandler the waiting handler
     * 
     * @throws IOException thrown if something goes wrong with the export
     */
    public static void analyzeInUnipept(
            List<String> peptides,
            boolean equateIandL,
            boolean filterDuplicates,
            boolean handleMissingCleavage,
            File tempHtmlFile,
            WaitingHandler waitingHandler
    ) throws IOException {

        // write the html form to a temporary file
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(tempHtmlFile))) {

            // write the initial html section
            writer.write("<html>");
            writer.newLine();
            writer.write("\t<body>");
            writer.newLine();
            writer.write("\t\t<form id=\"unipept-form\" action=\"https://unipept.ugent.be/export\" accept-charset=\"UTF-8\" method=\"post\">");
            writer.newLine();
            writer.write("\t\t\t<input name=\"utf8\" type=\"hidden\" value=\"âœ“\">");
            writer.newLine();
            writer.write("\t\t\t<textarea name=\"qs\" id=\"qs\" rows=\"7\" style=\"visibility: hidden;\">");
            writer.newLine();

            // write the peptide sequences
            for (String peptide : peptides) {

                writer.write(peptide); // @TODO: possible to also avoid creating the potentially large peptide list?
                writer.newLine();

            }

            // write the ending html section
            writer.write("\t\t\t</textarea>");
            writer.newLine();
            writer.write("\t\t\t<input type=\"text\" name=\"search_name\" id=\"search_name\" style=\"visibility: hidden;\">");
            writer.newLine();
            writer.write("\t\t\t<input type=\"checkbox\" name=\"il\" id=\"il\" value=\"1\" " + (equateIandL ? "checked=\"checked\"" : "") + " style=\"visibility: hidden;\">");
            writer.newLine();
            writer.write("\t\t\t<input type=\"checkbox\" name=\"dupes\" id=\"dupes\" value=\"1\" " + (filterDuplicates ? "checked=\"checked\"" : "") + " style=\"visibility: hidden;\">");
            writer.newLine();
            writer.write("\t\t\t<input type=\"checkbox\" name=\"missed\" id=\"missed\" value=\"1\" " + (handleMissingCleavage ? "checked=\"checked\"" : "") + " style=\"visibility: hidden;\">");
            writer.newLine();
            writer.write("\t\t</form>");
            writer.newLine();
            writer.write("\t\t<script>");
            writer.newLine();
            writer.write("\t\t\twindow.onload = () => {");
            writer.newLine();
            writer.write("\t\t\t\tdocument.getElementById(\"unipept-form\").submit();");
            writer.newLine();
            writer.write("\t\t\t};");
            writer.newLine();
            writer.write("\t\t</script>");
            writer.newLine();
            writer.write("\t</body>");
            writer.newLine();
            writer.write("</html>");
            writer.newLine();

        }

        // open the temporary file in the default browser, which automatically forwards to the
        // unipept analysis page, with all required values automatically set and filled in
        BareBonesBrowserLaunch.openURL(tempHtmlFile.toURI().toString());

        // remove the temporary file
        //tempHtmlFile.delete(); // @TODO: implement? but only after the forwarding has happened!
    }
}
