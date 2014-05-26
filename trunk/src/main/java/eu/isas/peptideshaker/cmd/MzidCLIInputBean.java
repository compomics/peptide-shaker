package eu.isas.peptideshaker.cmd;

import java.io.File;
import org.apache.commons.cli.CommandLine;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Marc Vaueel
 */
public class MzidCLIInputBean {
    
    /**
     * The PeptideShaker cps file.
     */
    private File cpsFile;
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
     * The path settings.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;
    
    /**
     * Parses a MzidCLI command line and stores the input in the attributes.
     * 
     * @param aLine a MzidCLI command line
     */
    public MzidCLIInputBean(CommandLine aLine) {
        
        if (aLine.hasOption(MzidCLIParams.CPS_FILE.id)) {
            cpsFile = new File(aLine.getOptionValue(MzidCLIParams.CPS_FILE.id));
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
        if (aLine.hasOption(MzidCLIParams.OUTPUT_FILE.id)) {
            outputFile = new File(aLine.getOptionValue(MzidCLIParams.OUTPUT_FILE.id));
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
     * Returns the path settings input.
     * 
     * @return the path settings input
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }
}
