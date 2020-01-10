package eu.isas.peptideshaker.export;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.waiting.WaitingHandler;
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
 */
public class UnipeptExport {

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
     * @throws IOException thrown if something goes wrong with the export
     */
    public static void analyzeInUnipept(List<String> peptides, boolean equateIandL, boolean filterDuplicates, boolean handleMissingCleavage, File tempHtmlFile, WaitingHandler waitingHandler) throws IOException {

        String htmlBeforePeptides
                = "<html>"
                + "   <body>"
                + "       <form id=\"unipept-form\" action=\"https://unipept.ugent.be/mpa\" accept-charset=\"UTF-8\" method=\"post\">" // @TODO: name should not be MPA anymore?
                + "           <input name=\"utf8\" type=\"hidden\" value=\"âœ“\">"
                + "            <textarea name=\"qs\" id=\"qs\" rows=\"7\" style=\"visibility: hidden;\">";

        String htmlAfterPeptides
                = "            </textarea>"
                + "            <input type=\"text\" name=\"search_name\" id=\"search_name\" style=\"visibility: hidden;\">"
                + "            <input type=\"checkbox\" name=\"il\" id=\"il\" value=\"1\" " + (equateIandL ? "checked=\"checked\"" : "") + " style=\"visibility: hidden;\">"
                + "            <input type=\"checkbox\" name=\"dupes\" id=\"dupes\"  value=\"1\" " + (filterDuplicates ? "checked=\"checked\"" : "") + " style=\"visibility: hidden;\">"
                + "            <input type=\"checkbox\" name=\"missed\" id=\"missed\" value=\"1\" " + (handleMissingCleavage ? "checked=\"checked\"" : "") + " style=\"visibility: hidden;\">"
                + "       </form>"
                + "       <script>"
                + "            window.onload = () => {"
                + "                document.getElementById(\"unipept-form\").submit();"
                + "            };"
                + "        </script>"
                + "   </body>"
                + "</html>";

        // write the list of peptides to the form
        StringBuilder builder = new StringBuilder(htmlBeforePeptides);
        for (String peptide : peptides) {
            builder.append(peptide);
            builder.append("\n");
        }
        String tempHtml = builder.toString() + htmlAfterPeptides;

        // write the html form to a temporary file
        FileWriter writer = new FileWriter(tempHtmlFile);
        writer.write(tempHtml);
        writer.close();

        // open the temporary file in the default browser, which automatically forwards to the
        // unipept analysis page, with all required values automatically set and filled in
        BareBonesBrowserLaunch.openURL(tempHtmlFile.toURI().toString());
        
        // remove the temporary file
        //tempHtmlFile.delete(); // @TODO: implement? but only after the forwarding has happened!
    }
}
