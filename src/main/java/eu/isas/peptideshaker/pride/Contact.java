package eu.isas.peptideshaker.pride;

import java.io.*;

/**
 * An object for storing Contact details.
 *
 * @author Harald Barsnes
 */
public class Contact {

    /**
     * The contact's name.
     */
    private String name;
    /**
     * The contact's e-mail.
     */
    private String eMail;
    /**
     * The contact's institution.
     */
    private String institution;

    /**
     * Create a new Contact objetct.
     *
     * @param name
     * @param eMail
     * @param institution
     */
    public Contact(String name, String eMail, String institution) {
        this.name = name;
        this.eMail = eMail;
        this.institution = institution;
    }

    /**
     * Read a contact from file.
     *
     * @param contactFile
     */
    public Contact(File contactFile) {
        try {
            FileReader r = new FileReader(contactFile);
            BufferedReader br = new BufferedReader(r);

            String tempLine = br.readLine();
            name = tempLine.substring(tempLine.indexOf(":") + 1).trim();
            tempLine = br.readLine();
            eMail = tempLine.substring(tempLine.indexOf(":") + 1).trim();
            tempLine = br.readLine();
            institution = tempLine.substring(tempLine.indexOf(":") + 1).trim();

            br.close();
            r.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the contact to file.
     * 
     * @param contactFile
     * @throws IOException 
     */
    public void saveAsFile(File contactFile) throws IOException {

        String tempInstitution = institution.trim();
        tempInstitution = tempInstitution.replaceAll("\n", ", ");
        tempInstitution = tempInstitution.replaceAll(" , ", "");

        while (tempInstitution.lastIndexOf(",,") != -1) {
            tempInstitution = tempInstitution.replaceAll(",,", ",");
        }

        if (tempInstitution.endsWith(",")) {
            tempInstitution = tempInstitution.substring(0, tempInstitution.length() - 1);
        }


        FileWriter r = new FileWriter(contactFile);
        BufferedWriter bw = new BufferedWriter(r);

        bw.write("Name: " + name + "\n");
        bw.write("E-mail: " + eMail + "\n");
        bw.write("Institution: " + tempInstitution + "\n");

        bw.close();
        r.close();
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
     * @return the eMail
     */
    public String getEMail() {
        return eMail;
    }

    /**
     * @param eMail the eMail to set
     */
    public void setEMail(String eMail) {
        this.eMail = eMail;
    }

    /**
     * @return the institution
     */
    public String getInstitution() {
        return institution;
    }

    /**
     * @param institution the institution to set
     */
    public void setInstitution(String institution) {
        this.institution = institution;
    }
}
