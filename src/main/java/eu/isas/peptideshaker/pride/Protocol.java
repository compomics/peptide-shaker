package eu.isas.peptideshaker.pride;

import java.io.*;
import java.util.ArrayList;

/**
 * An object for storing Protocol details.
 *
 * @author Harald Barsnes
 */
public class Protocol {

    /**
     * The protcol name.
     */
    private String name;
    /**
     * The list of CV terms.
     */
    private ArrayList<CvTerm> cvTerms;

    /**
     * Create a new Protocol object.
     *
     * @param name
     * @param cvTerms
     */
    public Protocol(String name, ArrayList<CvTerm> cvTerms) {
        this.name = name;
        this.cvTerms = cvTerms;
    }

    /**
     * Read a Protocol from file.
     *
     * @param protocolFile
     */
    public Protocol(File protocolFile) {
        try {
            FileReader r = new FileReader(protocolFile);
            BufferedReader br = new BufferedReader(r);

            String tempLine = br.readLine();
            name = tempLine.substring(tempLine.indexOf(":") + 1).trim();
            tempLine = br.readLine();

            int cvTermCounter = new Integer(tempLine);
            cvTerms = new ArrayList<CvTerm>(cvTermCounter);

            for (int i = 0; i < cvTermCounter; i++) {

                String accession = br.readLine();
                accession = accession.substring(accession.indexOf(":") + 1).trim();
                String ontology = br.readLine();
                ontology = ontology.substring(ontology.indexOf(":") + 1).trim();
                String cvName = br.readLine();
                cvName = cvName.substring(cvName.indexOf(":") + 1).trim();
                String value = br.readLine();
                value = value.substring(value.indexOf(":") + 1).trim();
                br.readLine();

                if (value.length() == 0 || value.equalsIgnoreCase("null")) {
                    value = null;
                }

                cvTerms.add(new CvTerm(ontology, accession, cvName, value));
            }

            br.close();
            r.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the Protocol to file.
     * 
     * @param sampleFile
     * @throws IOException 
     */
    public void saveAsFile(File sampleFile) throws IOException {

        FileWriter f = new FileWriter(sampleFile);
        BufferedWriter br = new BufferedWriter(f);

        br.write("Name: " + name + "\n");
        br.write(cvTerms.size() + "\n");

        for (int i = 0; i < cvTerms.size(); i++) {
            br.write("Accession: " + cvTerms.get(i).getAccession() + "\n");
            br.write("Ontology: " + cvTerms.get(i).getOntology() + "\n");
            br.write("Name: " + cvTerms.get(i).getName() + "\n");
            br.write("Value: " + cvTerms.get(i).getValue() + "\n");
            br.write("\n");
        }

        br.close();
        f.close();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the cvTerms
     */
    public ArrayList<CvTerm> getCvTerms() {
        return cvTerms;
    }

    /**
     * @param cvTerms the cvTerms to set
     */
    public void setCvTerms(ArrayList<CvTerm> cvTerms) {
        this.cvTerms = cvTerms;
    }
}
