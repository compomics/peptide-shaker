package eu.isas.peptideshaker.preferences;

import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JOptionPane;

/**
 * Contains methods for downloading gene and GO mappings.
 *
 * @author Harald Barsnes
 */
public class GenePreferences {

    // @TODO: move to utilities
    // @TODO: remove the GUI references? or at least the PeptideShakerGUI references

    /**
     * The species separator used in the species comboboxes.
     */
    public final String SPECIES_SEPARATOR = "------------------------------------------------------------";
    /**
     * The suffix to use for files containing gene mappings.
     */
    public final String GENE_MAPPING_FILE_SUFFIX = "_gene_mappings";
    /**
     * The suffix to use for files containing GO mappings.
     */
    public final String GO_MAPPING_FILE_SUFFIX = "_go_mappings";
    /**
     * The path to the folder containing the gene mapping files.
     */
    private final String GENE_MAPPING_PATH = "/resources/conf/gene_ontology/";
    /**
     * The current species. Used for the gene mappings.
     */
    private String currentSpecies = "Homo sapiens";
    /**
     * The GO domain map. e.g., key: GO term: GO:0007568, element:
     * biological_process.
     */
    private HashMap<String, String> goDomainMap;
    /**
     * The species map, key: latin name, element: ensembl database name, e.g.,
     * key: Homo sapiens, element: hsapiens_gene_ensembl.
     */
    private HashMap<String, String> speciesMap;
    /**
     * The Ensembl versions for the downloaded species.
     */
    private HashMap<String, String> ensemblVersionsMap;
    /**
     * The list of species.
     */
    private Vector<String> species;
    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;

    /**
     * Create a new GenePreferences object.
     *
     * @param peptideShakerGUI
     */
    public GenePreferences(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
    }

    /**
     * Download the GO mappings.
     *
     * @param selectedSpecies
     * @param ensemblVersion
     * @param progressDialog
     * @return true if the download was ok
     * @throws MalformedURLException
     * @throws IOException
     */
    public boolean downloadGoMappings(String selectedSpecies, String ensemblVersion, ProgressDialogX progressDialog) throws MalformedURLException, IOException {

        // Construct data
        String requestXml = "query=<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE Query>"
                + "<Query  virtualSchemaName = \"default\" formatter = \"TSV\" header = \"0\" uniqueRows = \"1\" count = \"\" datasetConfigVersion = \"0.6\" >"
                + "<Dataset name = \"" + selectedSpecies + "\" interface = \"default\" >"
                + "<Attribute name = \"uniprot_swissprot_accession\" />"
                + "<Attribute name = \"goslim_goa_accession\" />"
                + "<Attribute name = \"goslim_goa_description\" />"
                + "</Dataset>"
                + "</Query>";

        if (!progressDialog.isRunCanceled()) {

            // Send data
            URL url = new URL("http://www.biomart.org/biomart/martservice/result");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(requestXml);
            wr.flush();

            if (!progressDialog.isRunCanceled()) {

                // Get the response
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                progressDialog.setTitle("Downloading GO Mappings. Please Wait...");

                int counter = 0;

                File tempFile = new File(getGeneMappingFolder(), selectedSpecies + GO_MAPPING_FILE_SUFFIX);
                boolean fileCreated = tempFile.createNewFile();

                if (fileCreated) {

                    FileWriter w = new FileWriter(tempFile);
                    BufferedWriter bw = new BufferedWriter(w);

                    String rowLine = br.readLine();

                    if (rowLine != null && rowLine.startsWith("Query ERROR")) {
                        JOptionPane.showMessageDialog(peptideShakerGUI, rowLine, "Query Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        while (rowLine != null && !progressDialog.isRunCanceled()) {
                            progressDialog.setTitle("Downloading GO Mappings. Please Wait... (" + counter++ + " rows downloaded)");
                            bw.write(rowLine + System.getProperty("line.separator"));
                            rowLine = br.readLine();
                        }
                    }

                    bw.close();
                    w.close();
                    wr.close();
                    br.close();

                    if (!progressDialog.isRunCanceled()) {

                        // update the Ensembl species versions
                        w = new FileWriter(new File(getGeneMappingFolder(), "ensembl_versions"));
                        bw = new BufferedWriter(w);

                        ensemblVersionsMap.put(selectedSpecies, "Ensembl " + ensemblVersion);

                        Iterator<String> iterator = ensemblVersionsMap.keySet().iterator();

                        while (iterator.hasNext() && !progressDialog.isRunCanceled()) {
                            String key = iterator.next();
                            bw.write(key + "\t" + ensemblVersionsMap.get(key) + System.getProperty("line.separator"));
                        }

                        bw.close();
                        w.close();
                    }
                } else {
                    progressDialog.setRunCanceled();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "The mapping file could not be created.", "File Error", JOptionPane.ERROR_MESSAGE);
                }

                return !progressDialog.isRunCanceled();

            } else {
                wr.close();
                return false;
            }
        }

        return false;
    }

    /**
     * Download the gene mappings.
     *
     * @param selectedSpecies
     * @param progressDialog
     * @return true if the download was ok
     * @throws MalformedURLException
     * @throws IOException
     */
    public boolean downloadGeneMappings(String selectedSpecies, ProgressDialogX progressDialog) throws MalformedURLException, IOException {

        // Construct data
        String requestXml = "query=<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE Query>"
                + "<Query  virtualSchemaName = \"default\" formatter = \"TSV\" header = \"0\" uniqueRows = \"1\" count = \"\" datasetConfigVersion = \"0.6\" >"
                + "<Dataset name = \"" + selectedSpecies + "\" interface = \"default\" >"
                + "<Attribute name = \"ensembl_gene_id\" />"
                + "<Attribute name = \"external_gene_id\" />"
                + "<Attribute name = \"chromosome_name\" />"
                + "</Dataset>"
                + "</Query>";

        if (!progressDialog.isRunCanceled()) {

            // Send data
            URL url = new URL("http://www.biomart.org/biomart/martservice/result");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(requestXml);
            wr.flush();

            if (!progressDialog.isRunCanceled()) {

                // Get the response
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                progressDialog.setTitle("Downloading Gene Mappings. Please Wait...");

                int counter = 0;

                File tempFile = new File(getGeneMappingFolder(), selectedSpecies + GO_MAPPING_FILE_SUFFIX);
                boolean fileCreated = tempFile.createNewFile();

                if (fileCreated) {

                    FileWriter w = new FileWriter(tempFile);
                    BufferedWriter bw = new BufferedWriter(w);

                    String rowLine = br.readLine();

                    if (rowLine != null && rowLine.startsWith("Query ERROR")) {
                        JOptionPane.showMessageDialog(peptideShakerGUI, rowLine, "Query Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        while (rowLine != null && !progressDialog.isRunCanceled()) {
                            progressDialog.setTitle("Downloading Gene Mappings. Please Wait... (" + counter++ + " rows downloaded)");
                            bw.write(rowLine + System.getProperty("line.separator"));
                            rowLine = br.readLine();
                        }
                    }

                    bw.close();
                    w.close();
                    wr.close();
                    br.close();

                } else {
                    progressDialog.setRunCanceled();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "The mapping file could not be created.", "File Error", JOptionPane.ERROR_MESSAGE);
                }

                return !progressDialog.isRunCanceled();
            } else {
                wr.close();
                return false;
            }
        }

        return false;
    }

    /**
     * Returns the path to the folder containing the gene mapping files.
     *
     * @return the gene mapping folder
     */
    public File getGeneMappingFolder() {
        return new File(peptideShakerGUI.getJarFilePath(), GENE_MAPPING_PATH);
    }

    /**
     * Returns the current species.
     *
     * @return the currentSpecies
     */
    public String getCurrentSpecies() {
        return currentSpecies;
    }

    /**
     * Set the current species.
     *
     * @param currentSpecies the currentSpecies to set
     */
    public void setCurrentSpecies(String currentSpecies) {
        this.currentSpecies = currentSpecies;
    }

    /**
     * Load the mapping files.
     */
    public void loadSpeciesAndGoDomains() {

        try {
            File speciesFile = new File(getGeneMappingFolder(), "species");
            File ensemblVersionsFile = new File(getGeneMappingFolder(), "ensembl_versions");
            File goDomainsFile = new File(getGeneMappingFolder(), "go_domains");

            goDomainMap = new HashMap<String, String>();
            species = new Vector<String>();
            speciesMap = new HashMap<String, String>();
            ensemblVersionsMap = new HashMap<String, String>();

            if (!goDomainsFile.exists()) {
                JOptionPane.showMessageDialog(peptideShakerGUI, "GO domains file \"" + goDomainsFile.getName() + "\" not found!\n"
                        + "Continuing without GO domains.", "File Not Found", JOptionPane.ERROR_MESSAGE);
            } else {

                // read the GO domains
                FileReader r = new FileReader(goDomainsFile);
                BufferedReader br = new BufferedReader(r);

                String line = br.readLine();

                while (line != null) {
                    String[] elements = line.split("\\t");
                    goDomainMap.put(elements[0], elements[1]);
                    line = br.readLine();
                }

                br.close();
                r.close();
            }

            if (ensemblVersionsFile.exists()) {

                // read the Ensembl versions
                FileReader r = new FileReader(ensemblVersionsFile);
                BufferedReader br = new BufferedReader(r);

                String line = br.readLine();

                while (line != null) {
                    String[] elements = line.split("\\t");
                    ensemblVersionsMap.put(elements[0], elements[1]);
                    line = br.readLine();
                }

                br.close();
                r.close();
            }


            if (!speciesFile.exists()) {
                JOptionPane.showMessageDialog(peptideShakerGUI, "GO species file \"" + speciesFile.getName() + "\" not found!\n"
                        + "GO Analysis Canceled.", "File Not Found", JOptionPane.ERROR_MESSAGE);
            } else {

                // read the species list
                FileReader r = new FileReader(speciesFile);
                BufferedReader br = new BufferedReader(r);

                String line = br.readLine();

                species.add("-- Select Species --");
                species.add(SPECIES_SEPARATOR);

                while (line != null) {
                    String[] elements = line.split("\\t");
                    speciesMap.put(elements[0].trim(), elements[1].trim());

                    if (species.size() == 5) {
                        species.add(SPECIES_SEPARATOR);
                    }

                    if (ensemblVersionsMap.containsKey(elements[1].trim())) {
                        species.add(elements[0].trim() + " [" + ensemblVersionsMap.get(elements[1].trim()) + "]");
                    } else {
                        species.add(elements[0].trim() + " [N/A]");
                    }

                    line = br.readLine();
                }

                br.close();
                r.close();

                peptideShakerGUI.getGOPanel().setSpecies(species);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(peptideShakerGUI, "An error occured when loading the species and GO domain file.\n"
                    + "GO Analysis Canceled.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns the GO domain map, e.g., key: GO term: GO:0007568, element:
     * biological_process.
     *
     * @return the goDomainMap
     */
    public HashMap<String, String> getGoDomainMap() {
        return goDomainMap;
    }

    /**
     * Returns the species map. Key: latin name, element: ensembl database name,
     * e.g., key: Homo sapiens, element: hsapiens.
     *
     * @return the speciesMap
     */
    public HashMap<String, String> getSpeciesMap() {
        return speciesMap;
    }

    /**
     * Returns the Ensembl versions map.
     *
     * @return the ensemblVersionsMap
     */
    public HashMap<String, String> getEnsemblVersionsMap() {
        return ensemblVersionsMap;
    }

    /**
     * Return the species list. NB: also contains species separators.
     *
     * @return the species
     */
    public Vector<String> getSpecies() {
        return species;
    }
}
