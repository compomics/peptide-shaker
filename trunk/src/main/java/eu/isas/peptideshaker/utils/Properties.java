package eu.isas.peptideshaker.utils;

import java.io.InputStream;

/**
 * This class contains many of the properties that are used during the
 * use of the tool, but that are not stored in the UserProperties.prop
 * file between each run of the program.
 *
 * @author  Harald Barsnes
 */
public class Properties {
    /**
     * Creates a new empty Properties object.
     */
    public Properties() {
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of the PeptideShaker
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("peptide-shaker.properties");
            p.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return p.getProperty("peptide-shaker.version");
    }
}

