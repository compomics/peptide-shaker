package eu.isas.peptideshaker.pride;

import java.io.*;
import java.util.ArrayList;

/**
 * An object for storing Instrument details.
 *
 * @author Harald Barsnes
 */
public class Instrument {

    /**
     * The instrument name.
     */
    private String name;
    /**
     * The list of analyzer CV terms.
     */
    private ArrayList<CvTerm> cvTerms;
    /**
     * The instrument source.
     */
    private CvTerm source;
    /**
     * The instrument detector.
     */
    private CvTerm detector;

    /**
     * Create a new Instrument object.
     *
     * @param name
     * @param source
     * @param detector
     * @param cvTerms
     */
    public Instrument(String name, CvTerm source, CvTerm detector, ArrayList<CvTerm> cvTerms) {
        this.name = name;
        this.source = source;
        this.detector = detector;
        this.cvTerms = cvTerms;
    }

    /**
     * Read an Instrument from file.
     *
     * @param instrumentFile
     */
    public Instrument(File instrumentFile) {
        try {
            FileReader r = new FileReader(instrumentFile);
            BufferedReader br = new BufferedReader(r);

            String tempLine = br.readLine();
            name = tempLine.substring(tempLine.indexOf(":") + 1).trim();
            br.readLine(); // empty line
            br.readLine(); // #instrument source

            // read source
            String accession = br.readLine();
            accession = accession.substring(accession.indexOf(":") + 1).trim();
            String ontology = br.readLine();
            ontology = ontology.substring(ontology.indexOf(":") + 1).trim();
            String cvName = br.readLine();
            cvName = cvName.substring(cvName.indexOf(":") + 1).trim();
            String value = br.readLine();
            value = value.substring(value.indexOf(":") + 1).trim();

            if (value.length() == 0 || value.equalsIgnoreCase("null")) {
                value = null;
            }

            source = new CvTerm(ontology, accession, cvName, value);

            br.readLine(); // empty line
            br.readLine(); // #instrument detector

            // read detector
            accession = br.readLine();
            accession = accession.substring(accession.indexOf(":") + 1).trim();
            ontology = br.readLine();
            ontology = ontology.substring(ontology.indexOf(":") + 1).trim();
            cvName = br.readLine();
            cvName = cvName.substring(cvName.indexOf(":") + 1).trim();
            value = br.readLine();
            value = value.substring(value.indexOf(":") + 1).trim();

            if (value.length() == 0 || value.equalsIgnoreCase("null")) {
                value = null;
            }

            detector = new CvTerm(ontology, accession, cvName, value);

            br.readLine(); // empty line
            br.readLine(); // #analyzers

            // read the analyzer cv terms
            tempLine = br.readLine();
            int cvTermCounter = new Integer(tempLine);
            cvTerms = new ArrayList<CvTerm>(cvTermCounter);

            for (int i = 0; i < cvTermCounter; i++) {

                accession = br.readLine();
                accession = accession.substring(accession.indexOf(":") + 1).trim();
                ontology = br.readLine();
                ontology = ontology.substring(ontology.indexOf(":") + 1).trim();
                cvName = br.readLine();
                cvName = cvName.substring(cvName.indexOf(":") + 1).trim();
                value = br.readLine();
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
     * Save the Instrument to file.
     *
     * @param instrumentFile
     * @throws IOException
     */
    public void saveAsFile(File instrumentFile) throws IOException {

        FileWriter f = new FileWriter(instrumentFile);
        BufferedWriter br = new BufferedWriter(f);

        br.write("Name: " + name + "\n\n#instrument source\n");
        br.write("Accession: " + source.getAccession() + "\n");
        br.write("Ontology: " + source.getOntology() + "\n");
        br.write("Name: " + source.getName() + "\n");
        br.write("Value: " + source.getValue() + "\n");
        br.write("\n");
        
        br.write("#instrument detector\n");
        br.write("Accession: " + detector.getAccession() + "\n");
        br.write("Ontology: " + detector.getOntology() + "\n");
        br.write("Name: " + detector.getName() + "\n");
        br.write("Value: " + detector.getValue() + "\n");
        br.write("\n");
        
        br.write("#analyzers\n" + cvTerms.size() + "\n");

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

    /**
     * @return the source
     */
    public CvTerm getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(CvTerm source) {
        this.source = source;
    }

    /**
     * @return the detector
     */
    public CvTerm getDetector() {
        return detector;
    }

    /**
     * @param detector the detector to set
     */
    public void setDetector(CvTerm detector) {
        this.detector = detector;
    }
}
