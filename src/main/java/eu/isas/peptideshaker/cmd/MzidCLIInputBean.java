package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.io.identification.MzIdentMLVersion;
import java.io.File;
import org.apache.commons.cli.CommandLine;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc Vaueel
 * @author Harald Barsnes
 */
public class MzidCLIInputBean {

    /**
     * The PeptideShaker cpsx file.
     */
    private File cpsFile;
    /**
     * The zip file.
     */
    private File zipFile = null;
    /**
     * The contact first name.
     */
    private String contactFirstName;
    /**
     * The contact last name.
     */
    private String contactLastName;
    /**
     * The contact email.
     */
    private String contactEmail;
    /**
     * The contact address.
     */
    private String contactAddress;
    /**
     * The contact URL.
     */
    private String contactUrl;
    /**
     * The organization name.
     */
    private String organizationName;
    /**
     * The organization email.
     */
    private String organizationMail;
    /**
     * The organization address.
     */
    private String organizationAddress;
    /**
     * The organization URL.
     */
    private String organizationUrl;
    /**
     * The file where the mzIdentML export should be output.
     */
    private File outputFile;
    /**
     * If true, the protein sequences are included in the mzid file.
     */
    private Boolean includeProteinSequences;
    /**
     * The path settings.
     */
    private final PathSettingsCLIInputBean pathSettingsCLIInputBean;
    /**
     * The version of mzIdentML file to use, 1.1 by default.
     */
    private MzIdentMLVersion mzIdentMLVersion = MzIdentMLVersion.v1_1;
    /**
     * Boolean indicating whether the export should be gzipped.
     */
    private boolean gzip = false;

    /**
     * Parses a MzidCLI command line and stores the input in the attributes.
     *
     * @param aLine a MzidCLI command line
     */
    public MzidCLIInputBean(CommandLine aLine) {

        if (aLine.hasOption(MzidCLIParams.CPS_FILE.id)) {
            String file = aLine.getOptionValue(FollowUpCLIParams.CPS_FILE.id);
            if (file.toLowerCase().endsWith(".cpsx")) {
                cpsFile = new File(file);
            } else if (file.toLowerCase().endsWith(".zip")) {
                zipFile = new File(file);
            } else {
                throw new IllegalArgumentException("Unknown file format \'" + file + "\' for PeptideShaker project input.");
            }
        }
        if (aLine.hasOption(MzidCLIParams.CONTACT_FIRST_NAME.id)) {
            contactFirstName = aLine.getOptionValue(MzidCLIParams.CONTACT_FIRST_NAME.id);
        }
        if (aLine.hasOption(MzidCLIParams.CONTACT_LAST_NAME.id)) {
            contactLastName = aLine.getOptionValue(MzidCLIParams.CONTACT_LAST_NAME.id);
        }
        if (aLine.hasOption(MzidCLIParams.CONTACT_EMAIL.id)) {
            contactEmail = aLine.getOptionValue(MzidCLIParams.CONTACT_EMAIL.id);
        }
        if (aLine.hasOption(MzidCLIParams.CONTACT_ADDRESS.id)) {
            contactAddress = aLine.getOptionValue(MzidCLIParams.CONTACT_ADDRESS.id);
        }
        if (aLine.hasOption(MzidCLIParams.CONTACT_URL.id)) {
            contactUrl = aLine.getOptionValue(MzidCLIParams.CONTACT_URL.id);
        }
        if (aLine.hasOption(MzidCLIParams.ORGANIZATION_NAME.id)) {
            organizationName = aLine.getOptionValue(MzidCLIParams.ORGANIZATION_NAME.id);
        }
        if (aLine.hasOption(MzidCLIParams.ORGANIZATION_EMAIL.id)) {
            organizationMail = aLine.getOptionValue(MzidCLIParams.ORGANIZATION_EMAIL.id);
        }
        if (aLine.hasOption(MzidCLIParams.ORGANIZATION_ADDRESS.id)) {
            organizationAddress = aLine.getOptionValue(MzidCLIParams.ORGANIZATION_ADDRESS.id);
        }
        if (aLine.hasOption(MzidCLIParams.ORGANIZATION_URL.id)) {
            organizationUrl = aLine.getOptionValue(MzidCLIParams.ORGANIZATION_URL.id);
        }
        if (aLine.hasOption(MzidCLIParams.INCLUDE_PROTEIN_SEQUENCES.id)) {
            String input = aLine.getOptionValue(MzidCLIParams.INCLUDE_PROTEIN_SEQUENCES.id);
            includeProteinSequences = input.trim().equals("1");
        }
        if (aLine.hasOption(MzidCLIParams.VERSION.id)) {
            String input = aLine.getOptionValue(MzidCLIParams.VERSION.id);
            int index = Integer.parseInt(input.trim());
            mzIdentMLVersion = MzIdentMLVersion.getMzIdentMLVersion(index);
        }
        if (aLine.hasOption(MzidCLIParams.OUTPUT_FILE.id)) {
            outputFile = new File(aLine.getOptionValue(MzidCLIParams.OUTPUT_FILE.id));
        }
        if (aLine.hasOption(MzidCLIParams.GZIP.id)) {
            String input = aLine.getOptionValue(MzidCLIParams.GZIP.id);
            int index = Integer.parseInt(input.trim());
            gzip = index == 1;
        }

        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
    }

    /**
     * Returns the cps file.
     *
     * @return the cps file
     */
    public File getCpsFile() {
        return cpsFile;
    }

    /**
     * The zip file selected by the user. Null if not set.
     *
     * @return zip file selected by the user
     */
    public File getZipFile() {
        return zipFile;
    }

    /**
     * Returns the contact first name.
     *
     * @return the contact first name
     */
    public String getContactFirstName() {
        return contactFirstName;
    }

    /**
     * Returns the contact last name.
     *
     * @return the contact last name
     */
    public String getContactLastName() {
        return contactLastName;
    }

    /**
     * Returns the contact email.
     *
     * @return the contact email
     */
    public String getContactEmail() {
        return contactEmail;
    }

    /**
     * Returns the contact address.
     *
     * @return the contact address
     */
    public String getContactAddress() {
        return contactAddress;
    }

    /**
     * Returns the contact URL.
     *
     * @return the contact URL
     */
    public String getContactUrl() {
        return contactUrl;
    }

    /**
     * Returns the organization name.
     *
     * @return the organization name
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Returns the organization mail.
     *
     * @return the organization mail
     */
    public String getOrganizationMail() {
        return organizationMail;
    }

    /**
     * Returns the organization address.
     *
     * @return the organization address
     */
    public String getOrganizationAddress() {
        return organizationAddress;
    }

    /**
     * Returns the organization URL.
     *
     * @return the organization URL
     */
    public String getOrganizationUrl() {
        return organizationUrl;
    }

    /**
     * Returns the file where to mzIdentML export will be stored.
     *
     * @return the file where to mzIdentML export will be stored
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Returns true if the protein sequences are to be included in the mzid
     * export.
     *
     * @return true if the protein sequences are to be included in the mzid
     * export
     */
    public boolean getIncludeProteinSequences() {

        if (includeProteinSequences == null) {

            includeProteinSequences = false;

        }

        return includeProteinSequences;

    }

    /**
     * Returns the mzIdentML version to use for this file.
     *
     * @return the mzIdentML version to use for this file
     */
    public MzIdentMLVersion getMzIdentMLVersion() {
        return mzIdentMLVersion;
    }

    /**
     * Returns the path settings input.
     *
     * @return the path settings input
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }

    /**
     * Returns a boolean indicating whether the export should be gzipped.
     * 
     * @return a boolean indicating whether the export should be gzipped
     */
    public boolean isGzip() {
        return gzip;
    }
}
