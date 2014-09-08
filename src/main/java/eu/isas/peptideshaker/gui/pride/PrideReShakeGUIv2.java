package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.io.identifications.MzIdentMLIdfileSearchParametersConverter;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.searchsettings.EnzymeSelectionDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.ModificationProfile;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.WelcomeDialog;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.plot.PlotOrientation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetail;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetailList;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetail;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetailList;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetailList;
import uk.ac.ebi.pride.jaxb.model.CvParam;
import uk.ac.ebi.pride.jaxb.model.DataProcessing;
import uk.ac.ebi.pride.jaxb.model.Description;
import uk.ac.ebi.pride.jaxb.model.FragmentIon;
import uk.ac.ebi.pride.jaxb.model.Identification;
import uk.ac.ebi.pride.jaxb.model.Param;
import uk.ac.ebi.pride.jaxb.model.PeptideItem;
import uk.ac.ebi.pride.jaxb.model.Precursor;
import uk.ac.ebi.pride.jaxb.model.Spectrum;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

/**
 * Frame for talking to the PRIDE Archive web service to select projects for
 * reshaking.
 *
 * @author Harald Barsnes
 */
public class PrideReShakeGUIv2 extends javax.swing.JFrame {

    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The currently selected species.
     */
    private String currentSpecies;
    /**
     * The project table column header tooltips.
     */
    private ArrayList<String> projectsTableToolTips;
    /**
     * The assay table column header tooltips.
     */
    private ArrayList<String> assaysTableToolTips;
    /**
     * The files table column header tooltips.
     */
    private ArrayList<String> filesTableToolTips;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The URL of the current PRIDE project.
     */
    private URL currentPrideDataFileUrl;
    /**
     * The current zipped PRIDE XML file.
     */
    private File currentZippedPrideDataFile;
    /**
     * The current PRIDE XML file.
     */
    private File currentPrideDataFile;
    /**
     * The current mgf file.
     */
    private File currentMgfFile;
    /**
     * The output folder for the mgfs and spectrum properties.
     */
    private String outputFolder = "user.home";
    /**
     * The maximum precursor charge detected in the PRIDE XML files.
     */
    private Integer maxPrecursorCharge = null;
    /**
     * The minimum precursor charge detected in the PRIDE XML files.
     */
    private Integer minPrecursorCharge = null;
    /*
     * The welcome dialog parent, can be null.
     */
    private WelcomeDialog welcomeDialog;
    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The PRIDE search parameters report.
     */
    private String prideParametersReport = "";
    /**
     * The list of all found species.
     */
    private ArrayList<String> speciesAll;
    /**
     * The list of all found instruments.
     */
    private ArrayList<String> instrumentsAll;
    /**
     * The list of all found tissues.
     */
    private ArrayList<String> tissuesAll;
    /**
     * The list of all found PTMs.
     */
    private ArrayList<String> ptmsAll;
    /**
     * The current filter values.
     */
    private String[] currentFilterValues = new String[8];
    /**
     * The assay number filter type. True means greater than, false means
     * smaller than.
     */
    private boolean assaysGreaterThanFiler = true;
    /**
     * The list of reshakeable files.
     */
    private HashMap<String, ArrayList<String>> reshakeableFiles;
    /**
     * The list of files where search settings can be extracted.
     */
    private HashMap<String, ArrayList<String>> searchSettingsFiles;
    /**
     * The web service URL.
     */
    private static final String projectServiceURL = "http://www.ebi.ac.uk/pride/ws/archive/"; // "http://wwwdev.ebi.ac.uk:80/pride/ws/archive/";
    /**
     * The data format.
     */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * The user name.
     */
    private String userName = null;
    /**
     * The password.
     */
    private String password = null;
    /**
     * The Reshake setup dialog.
     */
    private PrideReshakeSetupDialog prideReshakeSetupDialog;

    /**
     * Creates a new PrideReShakeGUI2 frame.
     *
     * @param peptideShakerGUI
     */
    public PrideReShakeGUIv2(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();
        setUpGui();
        this.setExtendedState(MAXIMIZED_BOTH);
        setVisible(true);

        PrideDataTypeSelectionDialog dataTypeSelectionDialog = new PrideDataTypeSelectionDialog(this, true);

        if (dataTypeSelectionDialog.isPublic()) {
            loadPublicProjects();
        } else {
            getPrivateProjectDetails();
        }
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        clearProjectFiltersLabel.setVisible(false);

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

        reshakeableFiles = new HashMap<String, ArrayList<String>>();
        reshakeableFiles.put("RESULT", new ArrayList<String>());
        reshakeableFiles.get("RESULT").add(".xml");
        reshakeableFiles.get("RESULT").add(".xml.gz");
        reshakeableFiles.put("PEAK", new ArrayList<String>());
        reshakeableFiles.get("PEAK").add(".mgf");
        reshakeableFiles.get("PEAK").add(".mgf.gz");

        // then check for incorrect labeling...
        reshakeableFiles.put("OTHER", new ArrayList<String>());
        reshakeableFiles.get("OTHER").add(".mgf");
        reshakeableFiles.get("OTHER").add(".mgf.gz");
        reshakeableFiles.put("RAW", new ArrayList<String>());
        reshakeableFiles.get("RAW").add(".mgf");
        reshakeableFiles.get("RAW").add(".mgf.gz");

        searchSettingsFiles = new HashMap<String, ArrayList<String>>();
        searchSettingsFiles.put("RESULT", new ArrayList<String>());
        searchSettingsFiles.get("RESULT").add(".xml");
        searchSettingsFiles.get("RESULT").add(".xml.gz");
        searchSettingsFiles.get("RESULT").add(".mzid");
        searchSettingsFiles.get("RESULT").add(".mzid.gz");

        projectsTable.getColumn("Accession").setMaxWidth(90);
        projectsTable.getColumn("Accession").setMinWidth(90);
        projectsTable.getColumn(" ").setMaxWidth(50);
        projectsTable.getColumn(" ").setMinWidth(50);

        assaysTable.getColumn("Accession").setMaxWidth(90);
        assaysTable.getColumn("Accession").setMinWidth(90);
        assaysTable.getColumn(" ").setMaxWidth(50);
        assaysTable.getColumn(" ").setMinWidth(50);

        filesTable.getColumn("Assay").setMaxWidth(90);
        filesTable.getColumn("Assay").setMinWidth(90);
        filesTable.getColumn(" ").setMaxWidth(50);
        filesTable.getColumn(" ").setMinWidth(50);
        filesTable.getColumn("  ").setMaxWidth(30);
        filesTable.getColumn("  ").setMinWidth(30);
        filesTable.getColumn("Download").setMaxWidth(90);
        filesTable.getColumn("Download").setMinWidth(90);
        filesTable.getColumn("Type").setMaxWidth(90);
        filesTable.getColumn("Type").setMinWidth(90);
        filesTable.getColumn("Size").setMaxWidth(90);
        filesTable.getColumn("Size").setMinWidth(90);

        // make sure that the scroll panes are see-through
        projectsScrollPane.getViewport().setOpaque(false);
        assayTableScrollPane.getViewport().setOpaque(false);
        filesTableScrollPane.getViewport().setOpaque(false);

        projectsTable.setAutoCreateRowSorter(true);
        assaysTable.setAutoCreateRowSorter(true);
        filesTable.setAutoCreateRowSorter(true);

        projectsTable.getTableHeader().setReorderingAllowed(false);
        assaysTable.getTableHeader().setReorderingAllowed(false);
        filesTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel projectsCorner = new JPanel();
        projectsCorner.setBackground(projectsTable.getTableHeader().getBackground());
        projectsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, projectsCorner);

        JPanel assayCorner = new JPanel();
        assayCorner.setBackground(assaysTable.getTableHeader().getBackground());
        assayTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, assayCorner);

        JPanel filesCorner = new JPanel();
        filesCorner.setBackground(filesTable.getTableHeader().getBackground());
        filesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, filesCorner);

        projectsTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        assaysTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        filesTable.getColumn("Assay").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        filesTable.getColumn("Download").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        filesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                null,
                "Reshakeable", null));

        projectsTableToolTips = new ArrayList<String>();
        projectsTableToolTips.add(null);
        projectsTableToolTips.add("Project Accession Number");
        projectsTableToolTips.add("Project Title");
        projectsTableToolTips.add("Species");
        projectsTableToolTips.add("Tissue Types");
        projectsTableToolTips.add("Post-Translational Modifications");
        projectsTableToolTips.add("Instruments");
        projectsTableToolTips.add("Number of Assays");
        projectsTableToolTips.add("Project Type");
        projectsTableToolTips.add("Publication Date (yyyy-mm-dd)");

        assaysTableToolTips = new ArrayList<String>();
        assaysTableToolTips.add(null);
        assaysTableToolTips.add("Assay Accession Number");
        assaysTableToolTips.add("Assay Title");
        assaysTableToolTips.add("Species");
        assaysTableToolTips.add("Sample Details");
        assaysTableToolTips.add("Post-Translational Modifications");
        assaysTableToolTips.add("Diseases");
        assaysTableToolTips.add("Number of Proteins");
        assaysTableToolTips.add("Number of Peptides");
        assaysTableToolTips.add("Number of Spectra");

        filesTableToolTips = new ArrayList<String>();
        filesTableToolTips.add(null);
        filesTableToolTips.add("Assay Accession Numbers");
        filesTableToolTips.add("File Type");
        filesTableToolTips.add("File");
        filesTableToolTips.add("Download File");
        filesTableToolTips.add("File Size");
        filesTableToolTips.add("Reshakeable");

        ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects");
        ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays");
        ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files");

        reshakableCheckBoxActionPerformed(null);
    }

    /**
     * Returns the projects table.
     *
     * @return the projects table
     */
    public JTable getProjectsTable() {
        return projectsTable;
    }

    /**
     * Returns a reference to the main GUI.
     *
     * @return a reference to the main GUI
     */
    public PeptideShakerGUI getPeptideShakerGUI() {
        return peptideShakerGUI;
    }

    /**
     * Get the private project details from the user.
     */
    private void getPrivateProjectDetails() {
        PridePrivateDataDialog pridePrivateDataDialog = new PridePrivateDataDialog(this, true);
        if (pridePrivateDataDialog.getProjectAccession() != null) {
            userName = pridePrivateDataDialog.getUserName();
            if (userName.lastIndexOf("@") == -1) {
                userName += "@ebi.ac.uk"; // reviewer account
            }
            password = pridePrivateDataDialog.getPassword();
            loadPrivateProject(pridePrivateDataDialog.getProjectAccession());
        }
    }

    /**
     * Loads a private project.
     *
     * @param projectAccession
     */
    private void loadPrivateProject(String projectAccession) {

        try {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

            DefaultTableModel projectsTableModel = (DefaultTableModel) projectsTable.getModel();
            projectsTableModel.getDataVector().removeAllElements();
            projectsTableModel.fireTableDataChanged();

            DefaultTableModel assaysTableModel = (DefaultTableModel) assaysTable.getModel();
            assaysTableModel.getDataVector().removeAllElements();
            assaysTableModel.fireTableDataChanged();

            DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
            filesTableModel.getDataVector().removeAllElements();
            filesTableModel.fireTableDataChanged();

            ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects");
            projectsPanel.repaint();

            ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays");
            projectsPanel.repaint();

            ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files");
            projectsPanel.repaint();

            // load the project information
            RestTemplate template = new RestTemplate();
            ResponseEntity<ProjectDetail> entity = template.exchange("http://www.ebi.ac.uk/pride/ws/archive/project/" + projectAccession, HttpMethod.GET, getHttpEntity(), ProjectDetail.class);

            if (entity.getStatusCode() != null && entity.getStatusCode().equals(HttpStatus.OK)) {

                ProjectDetail projectDetail = entity.getBody();

                ((DefaultTableModel) projectsTable.getModel()).addRow(new Object[]{
                    (projectsTable.getRowCount() + 1),
                    projectDetail.getAccession(),
                    projectDetail.getTitle(),
                    setToString(projectDetail.getSpecies(), ", "),
                    setToString(projectDetail.getTissues(), ", "),
                    setToString(projectDetail.getPtmNames(), "; "),
                    setToString(projectDetail.getInstrumentNames(), ", "),
                    projectDetail.getNumAssays(),
                    projectDetail.getSubmissionType(),
                    null
                });

                ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects (" + projectsTable.getRowCount() + ")");
                projectsPanel.repaint();

                // update the sparklines with the max values
                projectsTable.getColumn("#Assays").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, (double) projectDetail.getNumAssays(), peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
                ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setLogScale(true);
                ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setMinimumChartValue(2.0);

                projectsTable.repaint();

                this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            } else {
                // @TODO: what to do here..?
                JOptionPane.showMessageDialog(this, "Cannot access " + projectAccession + " with the given user details.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            }

        } catch (HttpClientErrorException e) {

            this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            if (e.getMessage().trim().equalsIgnoreCase("401 Unauthorized")) {
                JOptionPane.showMessageDialog(this, "Cannot access " + projectAccession + " with the given user details.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Cannot access " + projectAccession + ": \n" + e.getMessage() + ".", "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
        }

        enableReshake();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        projectsPanel = new javax.swing.JPanel();
        projectsScrollPane = new javax.swing.JScrollPane();
        projectsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) projectsTableToolTips.get(realIndex);
                    }
                };
            }
        };
        accessPrivateDataLabel = new javax.swing.JLabel();
        projectHelpLabel = new javax.swing.JLabel();
        browsePublicDataLabel = new javax.swing.JLabel();
        dataTypeSeparatorLabel = new javax.swing.JLabel();
        clearProjectFiltersLabel = new javax.swing.JLabel();
        assaysPanel = new javax.swing.JPanel();
        assayTableScrollPane = new javax.swing.JScrollPane();
        assaysTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) assaysTableToolTips.get(realIndex);
                    }
                };
            }
        };
        assayHelpLabel = new javax.swing.JLabel();
        reshakeButton = new javax.swing.JButton();
        filesPanel = new javax.swing.JPanel();
        filesTableScrollPane = new javax.swing.JScrollPane();
        filesTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) filesTableToolTips.get(realIndex);
                    }
                };
            }
        };
        filesHelpLabel = new javax.swing.JLabel();
        reshakableCheckBox = new javax.swing.JCheckBox();
        aboutButton = new javax.swing.JButton();
        peptideShakerHomePageLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        findMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PRIDE Reshake");

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        projectsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PRIDE Projects"));
        projectsPanel.setOpaque(false);

        projectsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Title", "Species", "Tissues", "PTMs", "Instruments", "#Assays", "Type", "Date"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        projectsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        projectsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                projectsTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                projectsTableMouseExited(evt);
            }
        });
        projectsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                projectsTableMouseMoved(evt);
            }
        });
        projectsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                projectsTableKeyReleased(evt);
            }
        });
        projectsScrollPane.setViewportView(projectsTable);

        accessPrivateDataLabel.setText("<html><a href=\"dummy\">Access Private Data</a></html>\n\n");
        accessPrivateDataLabel.setToolTipText("Access private data");
        accessPrivateDataLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                accessPrivateDataLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                accessPrivateDataLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                accessPrivateDataLabelMouseExited(evt);
            }
        });

        projectHelpLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        projectHelpLabel.setText("Select a projects to see the project details. For more details click the Accession links.");

        browsePublicDataLabel.setText("<html><a href=\"dummy\">Browse Public Data</a></html>  ");
        browsePublicDataLabel.setToolTipText("Browse all public data");
        browsePublicDataLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                browsePublicDataLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                browsePublicDataLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                browsePublicDataLabelMouseExited(evt);
            }
        });

        dataTypeSeparatorLabel.setText("/");

        clearProjectFiltersLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Error_3.png"))); // NOI18N
        clearProjectFiltersLabel.setText("<html><a href=\"dummy\">Clear Project Filters</a></html>  ");
        clearProjectFiltersLabel.setToolTipText("Clear all project filters");
        clearProjectFiltersLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clearProjectFiltersLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                clearProjectFiltersLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                clearProjectFiltersLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout projectsPanelLayout = new javax.swing.GroupLayout(projectsPanel);
        projectsPanel.setLayout(projectsPanelLayout);
        projectsPanelLayout.setHorizontalGroup(
            projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1061, Short.MAX_VALUE)
                    .addGroup(projectsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(projectHelpLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(clearProjectFiltersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(browsePublicDataLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dataTypeSeparatorLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(accessPrivateDataLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)))
                .addContainerGap())
        );
        projectsPanelLayout.setVerticalGroup(
            projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(accessPrivateDataLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectHelpLabel)
                    .addComponent(browsePublicDataLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dataTypeSeparatorLabel)
                    .addComponent(clearProjectFiltersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        assaysPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Assays"));
        assaysPanel.setOpaque(false);

        assaysTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Title", "Species", "Sample", "PTMs", "Diseases", "#Proteins", "#Peptides", "#Spectra"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        assaysTable.setOpaque(false);
        assaysTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        assaysTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                assaysTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                assaysTableMouseExited(evt);
            }
        });
        assaysTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                assaysTableMouseMoved(evt);
            }
        });
        assaysTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                assaysTableKeyReleased(evt);
            }
        });
        assayTableScrollPane.setViewportView(assaysTable);

        assayHelpLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        assayHelpLabel.setText("Select an assay to see the corresponding files. For more details click the Assay links.");

        javax.swing.GroupLayout assaysPanelLayout = new javax.swing.GroupLayout(assaysPanel);
        assaysPanel.setLayout(assaysPanelLayout);
        assaysPanelLayout.setHorizontalGroup(
            assaysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(assaysPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(assaysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(assayTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1061, Short.MAX_VALUE)
                    .addGroup(assaysPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(assayHelpLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        assaysPanelLayout.setVerticalGroup(
            assaysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(assaysPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(assayTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(assayHelpLabel)
                .addContainerGap())
        );

        reshakeButton.setBackground(new java.awt.Color(0, 153, 0));
        reshakeButton.setFont(reshakeButton.getFont().deriveFont(reshakeButton.getFont().getStyle() | java.awt.Font.BOLD));
        reshakeButton.setForeground(new java.awt.Color(255, 255, 255));
        reshakeButton.setText("Reshake PRIDE Data");
        reshakeButton.setEnabled(false);
        reshakeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reshakeButtonActionPerformed(evt);
            }
        });

        filesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Files"));
        filesPanel.setOpaque(false);

        filesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Assay", "Type", "File", "Download", "Size", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        filesTable.setOpaque(false);
        filesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        filesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                filesTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                filesTableMouseExited(evt);
            }
        });
        filesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                filesTableMouseMoved(evt);
            }
        });
        filesTableScrollPane.setViewportView(filesTable);

        filesHelpLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        filesHelpLabel.setText("When you have found the wanted files click Reshake PRIDE Data to start re-analyzing. Supported formats: mgf and PRIDE XML.");

        reshakableCheckBox.setSelected(true);
        reshakableCheckBox.setText("Reshakeable Files Only");
        reshakableCheckBox.setToolTipText("Show only files that can be re-analyzed");
        reshakableCheckBox.setIconTextGap(10);
        reshakableCheckBox.setOpaque(false);
        reshakableCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reshakableCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout filesPanelLayout = new javax.swing.GroupLayout(filesPanel);
        filesPanel.setLayout(filesPanelLayout);
        filesPanelLayout.setHorizontalGroup(
            filesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(filesPanelLayout.createSequentialGroup()
                        .addComponent(filesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1061, Short.MAX_VALUE)
                        .addGap(10, 10, 10))
                    .addGroup(filesPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(filesHelpLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reshakableCheckBox)
                        .addGap(20, 20, 20))))
        );
        filesPanelLayout.setVerticalGroup(
            filesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(filesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filesHelpLabel)
                    .addComponent(reshakableCheckBox))
                .addContainerGap())
        );

        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/peptide-shaker-medium-orange-shadow.png"))); // NOI18N
        aboutButton.setToolTipText("Open the PeptideShaker web page");
        aboutButton.setBorder(null);
        aboutButton.setBorderPainted(false);
        aboutButton.setContentAreaFilled(false);
        aboutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                aboutButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                aboutButtonMouseExited(evt);
            }
        });
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        peptideShakerHomePageLabel.setText("<html>Please cite PeptideShaker as <a href=\"http://peptide-shaker.googlecode.com\">PeptideShaker (http://peptide-shaker.googlecode.com)</a>.</html>\n\n");
        peptideShakerHomePageLabel.setToolTipText("Open the PeptideShaker web page");
        peptideShakerHomePageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptideShakerHomePageLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptideShakerHomePageLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideShakerHomePageLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(aboutButton)
                        .addGap(48, 48, 48)
                        .addComponent(peptideShakerHomePageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 527, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reshakeButton)
                        .addGap(14, 14, 14))
                    .addComponent(assaysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(projectsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(filesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(assaysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(reshakeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(peptideShakerHomePageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(aboutButton))
                .addContainerGap())
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        findMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        findMenuItem.setMnemonic('F');
        findMenuItem.setText("Find...");
        findMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(findMenuItem);

        menuBar.add(editMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        helpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpMenuItem.setMnemonic('H');
        helpMenuItem.setText("Help");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the info about the selected project.
     *
     * @param evt
     */
    private void projectsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectsTableMouseReleased
        updateAssayList();
        updateProjectFileList();

        if (evt != null) {

            int row = projectsTable.getSelectedRow();
            int column = projectsTable.getSelectedColumn();

            // open pride project link in web browser
            if (column == projectsTable.getColumn("Accession").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) projectsTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) projectsTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_projectsTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void projectsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_projectsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
     * link.
     *
     * @param evt
     */
    private void projectsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectsTableMouseMoved
        int row = projectsTable.rowAtPoint(evt.getPoint());
        int column = projectsTable.columnAtPoint(evt.getPoint());

        projectsTable.setToolTipText(null);

        if (row != -1 && column != -1
                && (column == projectsTable.getColumn("Accession").getModelIndex())
                && projectsTable.getValueAt(row, column) != null) {

            String tempValue = (String) projectsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_projectsTableMouseMoved

    /**
     * Update the info about the selected project.
     *
     * @param evt
     */
    private void projectsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectsTableKeyReleased
        if (evt.getModifiers() != KeyEvent.CTRL_MASK) {
            updateProjectFileList();
            updateAssayList();
        }
    }//GEN-LAST:event_projectsTableKeyReleased

    /**
     * Show the files for the given assay.
     *
     * @param evt
     */
    private void assaysTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_assaysTableMouseReleased

        updateAssayFileList();

        if (evt != null) {

            int row = assaysTable.getSelectedRow();
            int column = assaysTable.getSelectedColumn();

            // open pride project link in web browser
            if (column == assaysTable.getColumn("Accession").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) assaysTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) assaysTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_assaysTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void assaysTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_assaysTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_assaysTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
     * link.
     *
     * @param evt
     */
    private void assaysTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_assaysTableMouseMoved
        int row = assaysTable.rowAtPoint(evt.getPoint());
        int column = assaysTable.columnAtPoint(evt.getPoint());

        assaysTable.setToolTipText(null);

        if (row != -1 && column != -1
                && (column == assaysTable.getColumn("Accession").getModelIndex())
                && assaysTable.getValueAt(row, column) != null) {

            String tempValue = (String) assaysTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_assaysTableMouseMoved

    /**
     * Show the files for the given assay
     *
     * @param evt
     */
    private void assaysTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_assaysTableKeyReleased
        updateAssayFileList();
    }//GEN-LAST:event_assaysTableKeyReleased

    /**
     * Reshake the selected PRIDE experiments.
     *
     * @param evt
     */
    private void reshakeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reshakeButtonActionPerformed
        prideReshakeSetupDialog = new PrideReshakeSetupDialog(this, true);
    }//GEN-LAST:event_reshakeButtonActionPerformed

    /**
     * Open the assay link or start downloading the file.
     *
     * @param evt
     */
    private void filesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_filesTableMouseReleased
        if (evt != null) {

            int row = filesTable.getSelectedRow();
            int column = filesTable.getSelectedColumn();

            // open pride project link in web browser
            if (column == filesTable.getColumn("Assay").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) filesTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) filesTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            } else if (column == filesTable.getColumn("Download").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) filesTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String tempLink = (String) filesTable.getValueAt(row, column);
                tempLink = tempLink.substring(tempLink.indexOf("\"") + 1);
                final String link = tempLink.substring(0, tempLink.indexOf("\""));
                final Double fileSize = (Double) filesTable.getValueAt(row, filesTable.getColumn("Size").getModelIndex());

                final File downloadFolder = Util.getUserSelectedFolder(this, "Select Download Folder", peptideShakerGUI.getLastSelectedFolder(), "Download Folder", "Select", false);

                if (downloadFolder != null) {

                    peptideShakerGUI.setLastSelectedFolder(downloadFolder.getAbsolutePath());

                    progressDialog = new ProgressDialogX(this,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);

                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Downloading File. Please Wait...");

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                progressDialog.setVisible(true);
                            } catch (IndexOutOfBoundsException e) {
                                // ignore
                            }
                        }
                    }, "ProgressDialog").start();

                    Thread thread = new Thread("DownloadThread") {
                        @Override
                        public void run() {
                            try {
                                File downLoadLocation = new File(downloadFolder, new File(link).getName());
                                File savedFile = saveUrl(downLoadLocation, link, fileSize, progressDialog);

                                boolean canceled = progressDialog.isRunCanceled();
                                progressDialog.setRunFinished();

                                if (!canceled) {
                                    JOptionPane.showMessageDialog(PrideReShakeGUIv2.this, savedFile.getName() + " downloaded to\n"
                                            + savedFile + ".", "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    if (downLoadLocation.exists()) {
                                        downLoadLocation.delete();
                                    }
                                }

                            } catch (MalformedURLException e) {
                                progressDialog.setRunFinished();
                                peptideShakerGUI.catchException(e);
                            } catch (IOException e) {
                                progressDialog.setRunFinished();
                                peptideShakerGUI.catchException(e);
                            }
                        }
                    };
                    thread.start();
                }
            }
        }
    }//GEN-LAST:event_filesTableMouseReleased

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void filesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_filesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_filesTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
     * link.
     *
     * @param evt
     */
    private void filesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_filesTableMouseMoved
        int row = filesTable.rowAtPoint(evt.getPoint());
        int column = filesTable.columnAtPoint(evt.getPoint());

        filesTable.setToolTipText(null);

        if (row != -1 && column != -1
                && (column == filesTable.getColumn("Assay").getModelIndex() || column == filesTable.getColumn("Download").getModelIndex())
                && filesTable.getValueAt(row, column) != null) {

            String tempValue = (String) filesTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_filesTableMouseMoved

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void aboutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void aboutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseExited

    /**
     * Open the PeptideShaker web page.
     *
     * @param evt
     */
    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the PeptideShaker web page.
     *
     * @param evt
     */
    private void peptideShakerHomePageLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseClicked

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void peptideShakerHomePageLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void peptideShakerHomePageLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseExited

    /**
     * Close the dialog.
     *
     * @param evt
     */
    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed

        // closeFiles the jvm
        System.exit(0);

        // @TODO: close the frame and show the Welcome Dialog again?
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * Open the search/filter dialog.
     *
     * @param evt
     */
    private void findMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMenuItemActionPerformed
        new ProjectsFilterDialog(this, false, currentFilterValues, assaysGreaterThanFiler, true, speciesAll, tissuesAll, instrumentsAll, ptmsAll);
    }//GEN-LAST:event_findMenuItemActionPerformed

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void accessPrivateDataLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_accessPrivateDataLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_accessPrivateDataLabelMouseExited

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void accessPrivateDataLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_accessPrivateDataLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_accessPrivateDataLabelMouseEntered

    /**
     * Open the private data login screen.
     *
     * @param evt
     */
    private void accessPrivateDataLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_accessPrivateDataLabelMouseClicked
        getPrivateProjectDetails();
        clearProjectFiltersLabelMouseClicked(null);
    }//GEN-LAST:event_accessPrivateDataLabelMouseClicked

    /**
     * Reload the public projects list.
     *
     * @param evt
     */
    private void browsePublicDataLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_browsePublicDataLabelMouseClicked
        userName = null;
        password = null;
        loadPublicProjects();
        clearProjectFiltersLabelMouseClicked(null);
    }//GEN-LAST:event_browsePublicDataLabelMouseClicked

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void browsePublicDataLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_browsePublicDataLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_browsePublicDataLabelMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void browsePublicDataLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_browsePublicDataLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_browsePublicDataLabelMouseExited

    /**
     * Update the file table.
     *
     * @param evt
     */
    private void reshakableCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reshakableCheckBoxActionPerformed

        List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

        // reshakeble filter
        RowFilter<Object, Object> reshakeableFilter = new RowFilter<Object, Object>() {
            public boolean include(Entry<? extends Object, ? extends Object> entry) {
                if (!reshakableCheckBox.isSelected()) {
                    return true;
                } else {
                    return (Boolean) entry.getValue(filesTable.getColumn("  ").getModelIndex());
                }
            }
        };

        filters.add(reshakeableFilter);

        RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);

        if (filesTable.getRowSorter() != null) {
            ((TableRowSorter) filesTable.getRowSorter()).setRowFilter(allFilters);

            if (filesTable.getRowCount() > 0) {
                filesTable.setRowSelectionInterval(0, 0);
            }
        }

        // fix the index column
        for (int i = 0; i < filesTable.getRowCount(); i++) {
            filesTable.setValueAt(i + 1, i, 0);
        }
    }//GEN-LAST:event_reshakableCheckBoxActionPerformed

    /**
     * Opens the help dialog.
     *
     * @param evt
     */
    private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/helpFiles/PrideReshake.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PRIDE Reshake - Help");
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Clear the project filters.
     *
     * @param evt
     */
    private void clearProjectFiltersLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clearProjectFiltersLabelMouseClicked
        ((TableRowSorter) projectsTable.getRowSorter()).setRowFilter(null);
        showProjectFilterRemovalOption(false);
        clearProjectFiltersLabel.setVisible(false);
        updateProjectTableSelection();
        String[] tempFilterValues = new String[8];
        setCurrentFilterValues(tempFilterValues, true);
    }//GEN-LAST:event_clearProjectFiltersLabelMouseClicked

    /**
     * Change the cursor to a hand icon.
     *
     * @param evt
     */
    private void clearProjectFiltersLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clearProjectFiltersLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_clearProjectFiltersLabelMouseEntered

    /**
     * Change the cursor back to the default icon.
     *
     * @param evt
     */
    private void clearProjectFiltersLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clearProjectFiltersLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_clearProjectFiltersLabelMouseExited

    /**
     * Update the file list based on the selected project.
     */
    private void updateProjectFileList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();
        reshakeButton.setEnabled(false);

        String projectAccession = null;
        int selectedRow = projectsTable.getSelectedRow();

        if (selectedRow != -1) {

            projectAccession = (String) projectsTable.getValueAt(selectedRow, 1);
            if (password == null) {
                projectAccession = projectAccession.substring(projectAccession.lastIndexOf("\">") + 2, projectAccession.lastIndexOf("</font"));
            }

            RestTemplate template = new RestTemplate();
            String url = projectServiceURL + "file/list/project/" + projectAccession;

            try {
                ResponseEntity<FileDetailList> fileDetailListResult;

                if (password != null) {
                    fileDetailListResult = template.exchange(url, HttpMethod.GET, getHttpEntity(), FileDetailList.class);
                } else {
                    fileDetailListResult = template.getForEntity(url, FileDetailList.class);
                }

                int reshakeableCounter = 0;
                int prideMissingFiles = 0;

                for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {

                    String fileDownloadLink = null;

                    if (fileDetail.getDownloadLink() != null) {
                        fileDownloadLink = "<html><a href=\"" + fileDetail.getDownloadLink().toExternalForm()
                                + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + "download" + "</font></a><html>";
                    } else {
                        if (password != null) {
                            fileDownloadLink = "<html><a href=\"" + "http://www.ebi.ac.uk/pride/ws/archive/file/" + projectAccession + "/" + fileDetail.getFileName()
                                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + "download" + "</font></a><html>";
                        }
                    }

                    String assayAccession = fileDetail.getAssayAccession();

                    if (password == null) {
                        assayAccession = "<html><a href=\"" + DisplayFeaturesGenerator.getPrideAssayArchiveLink(fileDetail.getProjectAccession(), fileDetail.getAssayAccession())
                                + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + fileDetail.getAssayAccession() + "</font></a><html>";
                    }

                    boolean reshakeable = false;

                    // check if the file is reshakeable
                    if (reshakeableFiles.containsKey(fileDetail.getFileType().getName())) {
                        for (String fileEnding : reshakeableFiles.get(fileDetail.getFileType().getName())) {
                            if (fileDetail.getFileName().toLowerCase().endsWith(".pride.mgf.gz")
                                    || fileDetail.getFileName().toLowerCase().endsWith(".pride.mztab.gz")) { // @TODO: allow the pride files as soon as they can be downloaded
                                prideMissingFiles++;
                                reshakeable = false;
                                break;
                            } else if (fileDetail.getFileName().toLowerCase().endsWith(fileEnding)) {
                                reshakeable = true;
                                reshakeableCounter++;
                                break;
                            }
                        }
                    }

                    if (!fileDetail.getFileName().toLowerCase().endsWith(".pride.mgf.gz")
                            && !fileDetail.getFileName().toLowerCase().endsWith(".pride.mztab.gz")) { // @TODO: allow the pride files as soon as they can be downloaded

                        ((DefaultTableModel) filesTable.getModel()).addRow(new Object[]{
                            (filesTable.getRowCount() + 1),
                            assayAccession,
                            fileDetail.getFileType().getName(),
                            fileDetail.getFileName(),
                            fileDownloadLink,
                            Util.roundDouble(((float) fileDetail.getFileSize()) / 1048576, 2), // @TODO: better formatting!!
                            reshakeable});
                    }
                }

                // update the border title
                if (projectAccession != null) {
                    ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files for "
                            + projectAccession + " (" + reshakeableCounter + "/" + (fileDetailListResult.getBody().getList().size() - prideMissingFiles) + ")");
                } else {
                    ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files ("
                            + reshakeableCounter + "/" + (fileDetailListResult.getBody().getList().size() - prideMissingFiles) + ")");
                }
                filesPanel.repaint();

            } catch (HttpServerErrorException e) {
                System.out.println(url);
                e.printStackTrace();
                this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                        "PRIDE web service access error. Cannot open:<br>"
                        + url + "<br>"
                        + "Please contact the <a href=\"http://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
                        "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, "PRIDE web service could not be reached.\n Please make sure that you are online.", "Network Error", JOptionPane.WARNING_MESSAGE);
            } catch (HttpMessageNotReadableException e) {
                System.out.println(url);
                e.printStackTrace();
                this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                        "PRIDE web service access error. Cannot open:<br>"
                        + url + "<br>"
                        + "Please contact the <a href=\"http://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
                        "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
            } catch (Exception e) {
                System.out.println(url);
                e.printStackTrace();
                this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                        "PRIDE web service access error. Cannot open:<br>"
                        + url + "<br>"
                        + "Please contact the <a href=\"http://groups.google.com/group/peptide-shaker\">PeptideShaker developers</a>."),
                        "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            // update the border title
            if (projectAccession != null) {
                ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files for "
                        + projectAccession + " (0)");
            } else {
                ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files (0)");
            }
            filesPanel.repaint();
        }

        if (filesTable.getRowCount() > 0) {
            filesTable.scrollRectToVisible(filesTable.getCellRect(0, 0, false));
        }

        enableReshake();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the file list based on the selected assay.
     */
    private void updateAssayFileList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();
        reshakeButton.setEnabled(false);

        String assayAccession = null;
        int selectedRow = assaysTable.getSelectedRow();

        if (selectedRow != -1) {

            assayAccession = (String) assaysTable.getValueAt(selectedRow, 1);
            if (password == null) {
                assayAccession = assayAccession.substring(assayAccession.lastIndexOf("\">") + 2, assayAccession.lastIndexOf("</font"));
            }

            RestTemplate template = new RestTemplate();
            String url = projectServiceURL + "file/list/assay/" + assayAccession;

            try {
                ResponseEntity<FileDetailList> fileDetailListResult;

                if (password != null) {
                    fileDetailListResult = template.exchange(url, HttpMethod.GET, getHttpEntity(), FileDetailList.class);
                } else {
                    fileDetailListResult = template.getForEntity(url, FileDetailList.class);
                }

                int reshakeableCounter = 0;
                int prideMissingFiles = 0;

                for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {

                    String fileDownloadLink = null;

                    if (fileDetail.getDownloadLink() != null) {
                        fileDownloadLink = "<html><a href=\"" + fileDetail.getDownloadLink().toExternalForm()
                                + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + "download" + "</font></a><html>";
                    } else {
                        if (password != null) {
                            fileDownloadLink = "<html><a href=\"" + "http://www.ebi.ac.uk/pride/ws/archive/file/" + fileDetail.getProjectAccession() + "/" + fileDetail.getFileName()
                                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + "download" + "</font></a><html>";
                        }
                    }

                    String assayAccessionLink = fileDetail.getAssayAccession();

                    if (password == null) {
                        assayAccessionLink = "<html><a href=\"" + DisplayFeaturesGenerator.getPrideAssayArchiveLink(fileDetail.getProjectAccession(), fileDetail.getAssayAccession())
                                + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + fileDetail.getAssayAccession() + "</font></a><html>";
                    }

                    boolean reshakeable = false;

                    // check if the file is reshakeable
                    if (reshakeableFiles.containsKey(fileDetail.getFileType().getName())) {
                        for (String fileEnding : reshakeableFiles.get(fileDetail.getFileType().getName())) {
                            if (fileDetail.getFileName().toLowerCase().endsWith(".pride.mgf.gz")
                                    || fileDetail.getFileName().toLowerCase().endsWith(".pride.mztab.gz")) { // @TODO: allow the pride files as soon as they can be downloaded
                                prideMissingFiles++;
                                reshakeable = false;
                                break;
                            } else if (fileDetail.getFileName().toLowerCase().endsWith(fileEnding)) {
                                reshakeable = true;
                                reshakeableCounter++;
                                break;
                            }
                        }
                    }

                    if (!fileDetail.getFileName().toLowerCase().endsWith(".pride.mgf.gz")
                            && !fileDetail.getFileName().toLowerCase().endsWith(".pride.mztab.gz")) { // @TODO: allow the pride files as soon as they can be downloaded

                        ((DefaultTableModel) filesTable.getModel()).addRow(new Object[]{
                            (filesTable.getRowCount() + 1),
                            assayAccessionLink,
                            fileDetail.getFileType().getName(),
                            fileDetail.getFileName(),
                            fileDownloadLink,
                            Util.roundDouble(((float) fileDetail.getFileSize()) / 1048576, 2), // @TODO: better formatting!!
                            reshakeable});
                    }
                }

                // update the border title
                if (assayAccession != null) {
                    ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files for Assay "
                            + assayAccession + " (" + reshakeableCounter + "/" + (fileDetailListResult.getBody().getList().size() - prideMissingFiles) + ")");
                } else {
                    ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files ("
                            + reshakeableCounter + "/" + (fileDetailListResult.getBody().getList().size() - prideMissingFiles) + ")");
                }
                filesPanel.repaint();

            } catch (HttpServerErrorException e) {
                System.out.println(url);
                e.printStackTrace();
                this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                        "PRIDE web service access error. Cannot open:<br>"
                        + url + "<br>"
                        + "Please contact the <a href=\"http://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
                        "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, "PRIDE web service could not be reached.\n Please make sure that you are online.", "Network Error", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            // update the border title
            if (assayAccession != null) {
                ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files for Assay "
                        + assayAccession + " (0)");
            } else {
                ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files (0)");
            }
            filesPanel.repaint();
        }

        if (filesTable.getRowCount() > 0) {
            filesTable.scrollRectToVisible(filesTable.getCellRect(0, 0, false));
        }

        enableReshake();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the assay list for the selected project.
     */
    private void updateAssayList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel assaysTableModel = (DefaultTableModel) assaysTable.getModel();
        assaysTableModel.getDataVector().removeAllElements();
        assaysTableModel.fireTableDataChanged();

        String projectAccession = null;
        int selectedRow = projectsTable.getSelectedRow();

        if (selectedRow != -1) {

            projectAccession = (String) projectsTable.getValueAt(selectedRow, 1);
            if (password == null) {
                projectAccession = projectAccession.substring(projectAccession.lastIndexOf("\">") + 2, projectAccession.lastIndexOf("</font"));
            }

            double maxNumProteins = 0, maxNumPeptides = 0, maxNumSpectra = 0;

            RestTemplate template = new RestTemplate();
            String url = projectServiceURL + "assay/list/project/" + projectAccession;

            try {
                ResponseEntity<AssayDetailList> assayDetailList;

                if (password != null) {
                    assayDetailList = template.exchange(url, HttpMethod.GET, getHttpEntity(), AssayDetailList.class);
                } else {
                    assayDetailList = template.getForEntity(url, AssayDetailList.class);
                }

                for (AssayDetail assayDetail : assayDetailList.getBody().getList()) {

                    String assayAccession = assayDetail.getAssayAccession();

                    if (password == null) {
                        assayAccession = "<html><a href=\"" + DisplayFeaturesGenerator.getPrideAssayArchiveLink(assayDetail.getProjectAccession(), assayDetail.getAssayAccession())
                                + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + assayDetail.getAssayAccession() + "</font></a><html>";
                    }

                    ((DefaultTableModel) assaysTable.getModel()).addRow(new Object[]{
                        (assaysTable.getRowCount() + 1),
                        assayAccession,
                        assayDetail.getTitle(),
                        setToString(assayDetail.getSpecies(), ", "),
                        setToString(assayDetail.getSampleDetails(), ", "),
                        setToString(assayDetail.getPtmNames(), "; "),
                        setToString(assayDetail.getDiseases(), ", "),
                        assayDetail.getProteinCount(),
                        assayDetail.getPeptideCount(),
                        assayDetail.getTotalSpectrumCount()
                    });

                    if (assayDetail.getProteinCount() > maxNumProteins) {
                        maxNumProteins = assayDetail.getProteinCount();
                    }
                    if (assayDetail.getPeptideCount() > maxNumPeptides) {
                        maxNumPeptides = assayDetail.getPeptideCount();
                    }
                    if (assayDetail.getTotalSpectrumCount() > maxNumSpectra) {
                        maxNumSpectra = assayDetail.getTotalSpectrumCount();
                    }
                }
            } catch (HttpServerErrorException e) {
                System.out.println(url);
                e.printStackTrace();
                this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                        "PRIDE web service access error. Cannot open:<br>"
                        + url + "<br>"
                        + "Please contact the <a href=\"http://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
                        "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, "PRIDE web service could not be reached.\n Please make sure that you are online.", "Network Error", JOptionPane.WARNING_MESSAGE);
            }

            // update the sparklines with the max values
            assaysTable.getColumn("#Proteins").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumProteins, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Proteins").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Proteins").getCellRenderer()).setLogScale(true);
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Proteins").getCellRenderer()).setMinimumChartValue(2.0);

            assaysTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumPeptides, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Peptides").getCellRenderer()).setLogScale(true);
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Peptides").getCellRenderer()).setMinimumChartValue(2.0);

            assaysTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumSpectra, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Spectra").getCellRenderer()).setLogScale(true);
            ((JSparklinesBarChartTableCellRenderer) assaysTable.getColumn("#Spectra").getCellRenderer()).setMinimumChartValue(2.0);
        }

        if (assaysTable.getRowCount() > 0) {
            assaysTable.scrollRectToVisible(assaysTable.getCellRect(0, 0, false));
        }

        if (projectAccession != null) {
            ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays for " + projectAccession + " (" + assaysTable.getRowCount() + ")");
        } else {
            ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays (" + assaysTable.getRowCount() + ")");
        }
        assaysPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Insert the public PRIDE project data.
     */
    private void loadPublicProjects() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel projectsTableModel = (DefaultTableModel) projectsTable.getModel();
        projectsTableModel.getDataVector().removeAllElements();
        projectsTableModel.fireTableDataChanged();

        DefaultTableModel assayTableModel = (DefaultTableModel) assaysTable.getModel();
        assayTableModel.getDataVector().removeAllElements();
        assayTableModel.fireTableDataChanged();

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();

        double maxNumAssays = 0;

        RestTemplate template = new RestTemplate();

        // get the project count
        try {
            ResponseEntity<Integer> projectCountResult = template.getForEntity(projectServiceURL + "project/count", Integer.class); // can also use project/count/?q=*
            Integer projectCount = projectCountResult.getBody();

            // get the list of projects
            ResponseEntity<ProjectDetailList> projectList = template.getForEntity(projectServiceURL 
                    + "project/list?show=" + projectCount + "&page=1&sort=publication_date&order=desc", ProjectDetailList.class);

            speciesAll = new ArrayList<String>();
            instrumentsAll = new ArrayList<String>();
            ptmsAll = new ArrayList<String>();
            tissuesAll = new ArrayList<String>();

            // iterate the project and add them to the table
            for (ProjectDetail projectDetail : projectList.getBody().getList()) {

                ((DefaultTableModel) projectsTable.getModel()).addRow(new Object[]{
                    (projectsTable.getRowCount() + 1),
                    "<html><a href=\"" + DisplayFeaturesGenerator.getPrideProjectArchiveLink("" + projectDetail.getAccession())
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + projectDetail.getAccession() + "</font></a><html>",
                    projectDetail.getTitle(),
                    setToString(projectDetail.getSpecies(), ", "),
                    setToString(projectDetail.getTissues(), ", "),
                    setToString(projectDetail.getPtmNames(), "; "),
                    setToString(projectDetail.getInstrumentNames(), ", "),
                    projectDetail.getNumAssays(),
                    projectDetail.getSubmissionType(),
                    dateFormat.format(projectDetail.getPublicationDate())
                });

                if (projectDetail.getNumAssays() > maxNumAssays) {
                    maxNumAssays = projectDetail.getNumAssays();
                }

                for (String species : projectDetail.getSpecies()) {
                    if (!speciesAll.contains(species)) {
                        speciesAll.add(species);
                    }
                }
                for (String instrument : projectDetail.getInstrumentNames()) {
                    if (!instrumentsAll.contains(instrument)) {
                        instrumentsAll.add(instrument);
                    }
                }
                for (String tissue : projectDetail.getTissues()) {
                    if (!tissuesAll.contains(tissue)) {
                        tissuesAll.add(tissue);
                    }
                }
                for (String ptm : projectDetail.getPtmNames()) {
                    if (!ptmsAll.contains(ptm)) {
                        ptmsAll.add(ptm);
                    }
                }
            }

            // sort the lists
            Collections.sort(speciesAll);
            Collections.sort(instrumentsAll);
            Collections.sort(tissuesAll);
            Collections.sort(ptmsAll);

            speciesAll.add(0, "");
            tissuesAll.add(0, "");
            instrumentsAll.add(0, "");
            ptmsAll.add(0, "");
        } catch (HttpServerErrorException e) {
            System.out.println("project/count or project/list");
            e.printStackTrace();
            this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                    "PRIDE web service access error. Cannot open:<br>"
                    + "project/count or project/list<br>"
                    + "Please contact the <a href=\"http://www.ebi.ac.uk/support/index.php?query=pride\">PRIDE team</a>."),
                    "PRIDE Access Error", JOptionPane.WARNING_MESSAGE);
        } catch (ResourceAccessException e) {
            JOptionPane.showMessageDialog(this, "PRIDE web service could not be reached.\n Please make sure that you are online.", "Network Error", JOptionPane.WARNING_MESSAGE);
        }

        ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects (" + projectsTable.getRowCount() + ")");
        projectsPanel.repaint();

        ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays");
        projectsPanel.repaint();

        ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files");
        projectsPanel.repaint();

        // update the sparklines with the max values
        projectsTable.getColumn("#Assays").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumAssays, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setLogScale(true);
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setMinimumChartValue(2.0);

        projectsTable.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Convert a set of strings to a single string.
     *
     * @param set
     * @param the separator to use
     * @return the set as a single string
     */
    private String setToString(Set<String> set, String separator) {
        Object[] elements = set.toArray();

        String result = "";
        for (Object object : elements) {
            if (!result.isEmpty()) {
                result += separator;
            }
            result += object.toString();
        }

        return result;
    }

    /**
     * Download and convert a PRIDE project.
     *
     * @param aWorkingFolder the working folder
     * @param selectedSpectrumFiles the selected spectrum files
     * @param searchSettingsProjectFile the selected search settings file, can
     * be null
     * @param database the database
     * @param aSpecies the current species
     * @param fileSizes the file sizes
     */
    public void downloadPrideDatasets(String aWorkingFolder, final ArrayList<String> selectedSpectrumFiles,
            final String searchSettingsProjectFile, final String database, String aSpecies, final ArrayList<Double> fileSizes) {

        outputFolder = aWorkingFolder;
        currentSpecies = aSpecies;

        ArrayList<String> tempFiles = new ArrayList<String>();
        tempFiles.addAll(selectedSpectrumFiles);
        if (searchSettingsProjectFile != null && !tempFiles.contains(searchSettingsProjectFile)) {
            tempFiles.add(searchSettingsProjectFile);
        }
        final ArrayList<String> allFiles = tempFiles;

        maxPrecursorCharge = null;
        minPrecursorCharge = null;

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        progressDialog.setTitle("Downloading Files. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DownloadThread") {
            @Override
            public void run() {

                try {
                    // set up the identification parameters
                    SearchParameters prideSearchParameters = new SearchParameters();

                    // set the database
                    prideSearchParameters.setFastaFile(new File(database));

                    // default settings to be used, set the enzyme to trypsin
                    if (searchSettingsProjectFile == null) {
                        prideSearchParameters.setEnzyme(EnzymeFactory.getUtilitiesEnzyme("Trypsin"));
                    }

                    String prideSearchParametersReport = null;
                    ArrayList<File> mgfFiles = new ArrayList<File>();
                    boolean mgfConversionOk = true;
                    Boolean useLocalFiles = null;

                    for (int i = 0; i < allFiles.size() && mgfConversionOk; i++) {

                        String currentFile = allFiles.get(i);
                        String currentFileName = currentFile.substring(currentFile.lastIndexOf("/")).toLowerCase();
                        boolean unzipped = true;

                        if (currentFileName.lastIndexOf(".gz") != -1) {
                            currentFileName = currentFileName.substring(0, currentFileName.lastIndexOf(".gz"));
                            unzipped = false;
                        }
                        if (progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            PrideReShakeGUIv2.this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            return;
                        }

                        progressDialog.setTitle("Downloading Files (" + (i + 1) + "/" + allFiles.size() + "). Please Wait...");

                        try {
                            currentPrideDataFileUrl = new URL(currentFile);
                            currentZippedPrideDataFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/")));
                            if (unzipped) {
                                currentPrideDataFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/")));
                                if (i < selectedSpectrumFiles.size()) {
                                    if (currentFile.toLowerCase().endsWith(".mgf")) {
                                        currentMgfFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/")));
                                    } else {
                                        currentMgfFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".xml")) + ".mgf");
                                    }
                                }
                            } else {
                                currentPrideDataFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".gz")));
                                if (i < selectedSpectrumFiles.size()) {
                                    if (currentFile.toLowerCase().endsWith(".mgf.gz")) {
                                        currentMgfFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".mgf.gz")) + ".mgf");
                                    } else {
                                        currentMgfFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".xml.gz")) + ".mgf");
                                    }
                                }
                            }
                            if (i < selectedSpectrumFiles.size()) {
                                mgfFiles.add(currentMgfFile);
                            }
                        } catch (MalformedURLException ex) {
                            JOptionPane.showMessageDialog(PrideReShakeGUIv2.this, JOptionEditorPane.getJOptionEditorPane("The file could not be downloaded:<br>"
                                    + ex.getMessage() + ".<br>"
                                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                                    "Download Error", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                            currentPrideDataFileUrl = null;
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(PrideReShakeGUIv2.this, JOptionEditorPane.getJOptionEditorPane("The file could not be downloaded:<br>"
                                    + ex.getMessage() + ".<br>"
                                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                                    "Download Error", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                            currentPrideDataFileUrl = null;
                        }

                        if (progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            PrideReShakeGUIv2.this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            return;
                        }

                        if (currentPrideDataFileUrl != null) {

                            if (!new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(), currentFileName).exists()) {

                                boolean downloadFile = true;

                                if (currentPrideDataFile.exists()) {

                                    if (useLocalFiles == null) {

                                        int option = JOptionPane.showConfirmDialog(PrideReShakeGUIv2.this,
                                                "The file \'" + currentPrideDataFile.getName() + "\' already exists locally.\nUse local copy?",
                                                "Use Local File?", JOptionPane.YES_NO_OPTION);

                                        if (allFiles.size() > 1) {
                                            int option2 = JOptionPane.showConfirmDialog(PrideReShakeGUIv2.this,
                                                    "Do this for all following files?",
                                                    "Use Local Files?", JOptionPane.YES_NO_OPTION);

                                            if (option2 == JOptionPane.YES_OPTION) {
                                                useLocalFiles = (option == JOptionPane.YES_OPTION);
                                            }
                                        }

                                        downloadFile = (option == JOptionPane.NO_OPTION);
                                    } else {
                                        downloadFile = !useLocalFiles;
                                    }
                                }

                                if (downloadFile) {

                                    // download the pride data file
                                    saveUrl(currentZippedPrideDataFile, currentFile, fileSizes.get(i), progressDialog);

                                    // file downloaded, unzip file
                                    progressDialog.setTitle("Unzipping Files (" + (i + 1) + "/" + allFiles.size() + "). Please Wait...");

                                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                                    if (!unzipped) {
                                        unzipProject();
                                    }
                                }
                            } else {
                                currentPrideDataFile = new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(), currentFileName);
                            }

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
                                PrideReShakeGUIv2.this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                return;
                            }

                            // file unzipped, time to start the conversion to mgf
                            if (i < selectedSpectrumFiles.size()) {
                                if (currentFile.toLowerCase().endsWith(".mgf")
                                        || currentFile.toLowerCase().endsWith(".mgf.gz")) {
                                    // already mgf, no conversion needed
                                } else {
                                    progressDialog.setTitle("Converting Spectrum Data (" + (i + 1) + "/" + allFiles.size() + "). Please Wait...");
                                    mgfConversionOk = convertPrideXmlToMgf();
                                }
                            }

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
                                PrideReShakeGUIv2.this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                return;
                            }

                            // get the search params from the pride xml or mzid file
                            if (mgfConversionOk) {
                                if (searchSettingsProjectFile != null
                                        && currentFile.equalsIgnoreCase(searchSettingsProjectFile)) {
                                    progressDialog.setTitle("Extracting Search Settings. Please Wait...");

                                    if (currentFile.toLowerCase().endsWith(".xml")
                                            || currentFile.toLowerCase().endsWith(".xml.gz")) {
                                        prideSearchParametersReport = getSearchParams(prideSearchParameters);
                                    } else { // mzid

                                        progressDialog.setPrimaryProgressCounterIndeterminate(true); // @TODO: better display of progress

                                        // convert the parameters from the assay
                                        prideSearchParametersReport = MzIdentMLIdfileSearchParametersConverter.getSearchParameters(currentPrideDataFile, prideSearchParameters, currentSpecies, progressDialog);

                                        // add the ptms from the project/assay
                                        String allPtms;
                                        if (assaysTable.getSelectedRow() != -1) {
                                            allPtms = (String) assaysTable.getValueAt(assaysTable.getSelectedRow(), assaysTable.getColumn("PTMs").getModelIndex());
                                        } else {
                                            allPtms = (String) projectsTable.getValueAt(projectsTable.getSelectedRow(), projectsTable.getColumn("PTMs").getModelIndex());
                                        }

                                        prideSearchParametersReport += convertPtms(allPtms, prideSearchParameters.getModificationProfile());

                                        // save the report to disk
                                        File searchSettingsReportFile = new File(outputFolder, "search_settings_report.html");
                                        String tempReport = "<html>" + prideSearchParametersReport;
                                        tempReport += "<br></html>";
                                        FileWriter fw = new FileWriter(searchSettingsReportFile);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        bw.write(tempReport);
                                        bw.close();
                                        fw.close();

                                        prideSearchParametersReport += "<br><br>Report saved to <a href=\"" + searchSettingsReportFile.getAbsolutePath() + "\">" + searchSettingsReportFile.getAbsolutePath() + "</a><br>";
                                        prideSearchParametersReport += "<br></html>";
                                        prideSearchParametersReport = "<html>" + prideSearchParametersReport;
                                    }
                                }
                            }
                        } else {
                            mgfConversionOk = false;
                        }
                    }

                    if (mgfConversionOk) {
                        // save the search params
                        prideSearchParameters.setParametersFile(new File(outputFolder, "pride.parameters"));
                        SearchParameters.saveIdentificationParameters(prideSearchParameters, new File(outputFolder, "pride.parameters"));
                    }

                    progressDialog.setRunFinished();
                    PrideReShakeGUIv2.this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    if (mgfConversionOk) {

                        // @TODO: support more species, but then the terms might have to be downloaded as well...
                        String selectedSpecies = null;
                        String selectedSpeciesType = null;
                        if (currentSpecies.equalsIgnoreCase("Homo sapiens (Human)")) {
                            selectedSpecies = "Human (Homo sapiens)";
                            selectedSpeciesType = "Vertebrates";
                        }

                        if (welcomeDialog != null) {
                            welcomeDialog.setVisible(false);
                        }

                        if (prideReshakeSetupDialog != null) {
                            prideReshakeSetupDialog.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        }

                        if (prideSearchParametersReport == null) {
                            prideSearchParametersReport = "<html><br><b><u>Extracted Search Parameters</u></b><br><br>"
                                    + "(No search parameters extracted)<br></html>";
                        }

                        // display the detected search parameters to the user
                        new PrideSearchParametersDialog(PrideReShakeGUIv2.this,
                                new File(outputFolder, "pride.parameters"), prideSearchParametersReport, mgfFiles, selectedSpecies, selectedSpeciesType, true);
                    }

                } catch (Exception e) {
                    PrideReShakeGUIv2.this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(PrideReShakeGUIv2.this,
                            "An error occured when processing the PRIDE project: \n"
                            + e.getMessage() + "."
                            + "See resources/PeptideShaker.log for details.",
                            "PRIDE Error", JOptionPane.INFORMATION_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Get the search parameters from the PRIDE project. Returns the PRIDE
     * search parameters as a string.
     *
     * @param prideAccession the pride accession number
     * @param prideSearchParameters the search parameters object to add the
     * search settings to
     * @return the PRIDE search parameters report
     * @throws Exception
     */
    private String getSearchParams(SearchParameters prideSearchParameters) throws Exception {

        progressDialog.setPrimaryProgressCounterIndeterminate(true);

        Double fragmentIonMassTolerance = null;
        Double peptideIonMassTolerance = null;
        Integer maxMissedCleavages = null;
        ArrayList<String> enzymes = new ArrayList<String>();

        PrideXmlReader prideXmlReader = new PrideXmlReader(currentPrideDataFile);

        Description description = prideXmlReader.getDescription();
        DataProcessing dataProcessing = description.getDataProcessing();

        if (dataProcessing != null && dataProcessing.getProcessingMethod() != null) {
            List<CvParam> processingMethods = dataProcessing.getProcessingMethod().getCvParam();

            for (CvParam cvParam : processingMethods) {
                if (cvParam.getAccession().equalsIgnoreCase("PRIDE:0000161")) { // fragment mass tolerance
                    String value = cvParam.getValue().trim();
                    if (value.indexOf(" ") != -1) { // escape Da or ppm
                        value = value.substring(0, value.indexOf(" "));
                    }
                    fragmentIonMassTolerance = new Double(value);
                } else if (cvParam.getAccession().equalsIgnoreCase("PRIDE:0000078")) { // peptide mass tolerance
                    String value = cvParam.getValue().trim();
                    if (value.indexOf(" ") != -1) { // escape Da or ppm
                        value = value.substring(0, value.indexOf(" "));
                    }
                    peptideIonMassTolerance = new Double(value);
                } else if (cvParam.getAccession().equalsIgnoreCase("PRIDE:0000162")) { // allowed missed cleavages
                    maxMissedCleavages = new Integer(cvParam.getValue());
                }
            }
        }

        if (prideXmlReader.getProtocol() != null && prideXmlReader.getProtocol().getProtocolSteps() != null) {

            List<Param> protocolStepDescription = prideXmlReader.getProtocol().getProtocolSteps().getStepDescription();

            for (Param stepDescription : protocolStepDescription) {
                List<CvParam> stepCvParams = stepDescription.getCvParam();

                for (CvParam stepCvParam : stepCvParams) {
                    if (stepCvParam.getAccession().equalsIgnoreCase("PRIDE:0000160") || stepCvParam.getAccession().equalsIgnoreCase("PRIDE:0000024")) { // enzyme
                        if (stepCvParam.getValue() != null) {
                            enzymes.add(stepCvParam.getValue());
                        }
                    }
                }
            }
        }

        HashMap<String, Integer> ionTypes = new HashMap<String, Integer>();
        HashMap<String, Integer> peptideLastResidues = new HashMap<String, Integer>();

        // get the fragment ion types used
        List<String> ids = prideXmlReader.getIdentIds();

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(ids.size());
        progressDialog.setValue(0);

        // set the ion types
        for (String id : ids) {

            // @TODO: implement me!
            progressDialog.increasePrimaryProgressCounter();

            Identification identification = prideXmlReader.getIdentById(id);
            List<PeptideItem> peptides = identification.getPeptideItem();

            for (PeptideItem peptide : peptides) {
                String residue = peptide.getSequence().substring(peptide.getSequence().length() - 1);

                if (!peptideLastResidues.containsKey(residue)) {
                    peptideLastResidues.put(residue, 1);
                } else {
                    peptideLastResidues.put(residue, peptideLastResidues.get(residue) + 1);
                }

                List<FragmentIon> fragmentIons = peptide.getFragmentIon();
                for (FragmentIon fragmentIon : fragmentIons) {

                    List<CvParam> stepCvParams = fragmentIon.getCvParam();

                    for (CvParam stepCvParam : stepCvParams) {
                        if (stepCvParam.getName().lastIndexOf("a ion") != -1) {
                            if (!ionTypes.containsKey("a-ion")) {
                                ionTypes.put("a-ion", 1);
                            } else {
                                ionTypes.put("a-ion", ionTypes.get("a-ion") + 1);
                            }
                        } else if (stepCvParam.getName().lastIndexOf("b ion") != -1) {
                            if (!ionTypes.containsKey("b-ion")) {
                                ionTypes.put("b-ion", 1);
                            } else {
                                ionTypes.put("b-ion", ionTypes.get("b-ion") + 1);
                            }
                        } else if (stepCvParam.getName().lastIndexOf("c ion") != -1) {
                            if (!ionTypes.containsKey("c-ion")) {
                                ionTypes.put("c-ion", 1);
                            } else {
                                ionTypes.put("c-ion", ionTypes.get("c-ion") + 1);
                            }
                        } else if (stepCvParam.getName().lastIndexOf("x ion") != -1) {
                            if (!ionTypes.containsKey("x-ion")) {
                                ionTypes.put("x-ion", 1);
                            } else {
                                ionTypes.put("x-ion", ionTypes.get("x-ion") + 1);
                            }
                        } else if (stepCvParam.getName().lastIndexOf("y ion") != -1) {
                            if (!ionTypes.containsKey("y-ion")) {
                                ionTypes.put("y-ion", 1);
                            } else {
                                ionTypes.put("y-ion", ionTypes.get("y-ion") + 1);
                            }
                        } else if (stepCvParam.getName().lastIndexOf("z ion") != -1) {
                            if (!ionTypes.containsKey("z-ion")) {
                                ionTypes.put("z-ion", 1);
                            } else {
                                ionTypes.put("z-ion", ionTypes.get("z-ion") + 1);
                            }
                        }
                    }
                }
            }
        }

        prideParametersReport = "<html><br><b><u>Extracted Search Parameters</u></b><br>";

        // set the fragment ion accuracy
        prideParametersReport += "<br><b>Fragment Ion Mass Tolerance:</b> ";
        if (fragmentIonMassTolerance != null) {
            prideSearchParameters.setFragmentIonAccuracy(fragmentIonMassTolerance);
            prideParametersReport += fragmentIonMassTolerance + " Da";
        } else {
            prideParametersReport += prideSearchParameters.getFragmentIonAccuracy() + " Da (default)"; // @TODO: what about accuracy in ppm
        }

        // set the precuros ion accuracy
        prideParametersReport += "<br><b>Precursor Ion Mass Tolerance:</b> ";
        if (peptideIonMassTolerance != null) {
            prideSearchParameters.setPrecursorAccuracy(peptideIonMassTolerance); // @TODO: ppm assumed?
            prideParametersReport += peptideIonMassTolerance + " ppm";
        } else {
            prideParametersReport += prideSearchParameters.getPrecursorAccuracy() + " ppm (default)"; // @TODO: what about accuracy in Dalton
        }

        // set the enzyme
        prideParametersReport += "<br><br><b>Enzyme:</b> ";

        if (!enzymes.isEmpty()) {
            if (enzymes.size() == 1) {

                Enzyme mappedEnzyme = EnzymeFactory.getUtilitiesEnzyme(enzymes.get(0));

                // unknown enzyme
                if (mappedEnzyme == null) {

                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    // have the user select the enzyme
                    EnzymeSelectionDialog enzymeSelectionDialog = new EnzymeSelectionDialog(this, true, enzymes.get(0));

                    Enzyme selectedEnzyme = enzymeSelectionDialog.getEnzyme();
                    if (selectedEnzyme != null) {
                        mappedEnzyme = selectedEnzyme;
                        prideParametersReport += selectedEnzyme.getName() + "<br>";
                    } else {
                        prideParametersReport += enzymes.get(0) + " (unknown)<br>";
                    }
                } else {
                    prideParametersReport += mappedEnzyme.getName() + "<br>";
                }

                prideSearchParameters.setEnzyme(mappedEnzyme);
            } else {

                // more than one enzyme given
                String enzymesAsText = "";
                for (int i = 0; i < enzymes.size(); i++) {
                    if (i > 0) {
                        enzymesAsText += " + ";
                    }
                    enzymesAsText += enzymes.get(i);
                }

                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                // have the user select the enzyme
                EnzymeSelectionDialog enzymeSelectionDialog = new EnzymeSelectionDialog(this, true, enzymesAsText);
                Enzyme selectedEnzyme = enzymeSelectionDialog.getEnzyme();
                if (selectedEnzyme != null) {
                    prideParametersReport += selectedEnzyme.getName() + "<br>";
                    prideSearchParameters.setEnzyme(selectedEnzyme);
                } else {
                    prideSearchParameters.setEnzyme(null);
                    prideParametersReport += enzymesAsText + " (unknown)<br>";
                }
            }
        } else {
            // try to guess enzyme from the ion types and peptide endings?
            // @TODO: implement me!
            //for (String ionType : ionTypes.keySet()) {
            //}
            //
            //for (String residues : peptideLastResidues.keySet()) {
            //}

            prideSearchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
            prideParametersReport += "Trypsin (assumed)<br>";
        }

        // set the max missed cleavages
        prideParametersReport += "<b>Maximum Missed Cleavages:</b> ";
        if (maxMissedCleavages != null) {
            prideSearchParameters.setnMissedCleavages(maxMissedCleavages);
            prideParametersReport += maxMissedCleavages;
        } else {
            prideParametersReport += prideSearchParameters.getnMissedCleavages() + " (default)";
        }

        // set the min/max precursor charge
        prideParametersReport += "<br><br><b>Min Precusor Charge:</b> ";
        if (minPrecursorCharge != null) {
            prideSearchParameters.setMinChargeSearched(new Charge(Charge.PLUS, minPrecursorCharge));
            prideParametersReport += minPrecursorCharge;
        } else {
            prideParametersReport += prideSearchParameters.getMinChargeSearched().value + " (default)";
        }
        prideParametersReport += "<br><b>Max Precusor Charge:</b> ";
        if (maxPrecursorCharge != null) {
            prideSearchParameters.setMaxChargeSearched(new Charge(Charge.PLUS, maxPrecursorCharge));
            prideParametersReport += maxPrecursorCharge;
        } else {
            prideParametersReport += prideSearchParameters.getMaxChargeSearched().value + " (default)";
        }

        // taxonomy and species
        prideParametersReport += "<br><br><b>Species:</b> ";
        String species = (String) projectsTable.getValueAt(projectsTable.getSelectedRow(), projectsTable.getColumn("Species").getModelIndex());
        if (species == null || species.length() == 0) {
            prideParametersReport += "unknown";
            currentSpecies = species;
        } else {
            prideParametersReport += species;
            currentSpecies = species;
        }

        // map the ptms to utilities ptms
        String allPtms = (String) projectsTable.getValueAt(projectsTable.getSelectedRow(), projectsTable.getColumn("PTMs").getModelIndex());
        prideParametersReport += convertPtms(allPtms, prideSearchParameters.getModificationProfile());

        // save the report to disk
        File searchSettingsReportFile = new File(outputFolder, "search_settings_report.html");
        String tempReport = prideParametersReport;
        tempReport += "<br></html>";
        FileWriter fw = new FileWriter(searchSettingsReportFile);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(tempReport);
        bw.close();
        fw.close();

        prideParametersReport += "<br><br>Report saved to <a href=\"" + searchSettingsReportFile.getAbsolutePath() + "\">" + searchSettingsReportFile.getAbsolutePath() + "</a><br>";
        prideParametersReport += "<br></html>";

        boolean debugOutput = false;

        // debug output
        if (debugOutput) {
            System.out.println("\nfragmentIonMassTolerance: " + fragmentIonMassTolerance);
            System.out.println("peptideIonMassTolerance: " + peptideIonMassTolerance);
            System.out.println("maxMissedCleavages: " + maxMissedCleavages);

            System.out.println("species: " + species);
            System.out.println("ptms: " + allPtms);

            System.out.print("enzyme(s): ");
            String enzymesAsText = "";
            for (int i = 0; i < enzymes.size(); i++) {
                if (i > 0) {
                    enzymesAsText += " + ";
                }
                enzymesAsText += enzymes.get(i);
            }
            System.out.println(enzymesAsText);
            System.out.println("minPrecursorCharge: " + minPrecursorCharge);
            System.out.println("maxPrecursorCharge: " + maxPrecursorCharge);

            System.out.print("ion types: ");
            for (String ionType : ionTypes.keySet()) {
                System.out.print(ionType + ": " + ionTypes.get(ionType) + ", ");
            }
            System.out.println();

            System.out.print("peptide endings: ");
            for (String residues : peptideLastResidues.keySet()) {
                System.out.print(residues + ": " + peptideLastResidues.get(residues) + ", ");
            }
            System.out.println();
        }

        return prideParametersReport;
    }

    /**
     * Returns true if the PTM is assumed to be fixed, false otherwise.
     *
     * @param pridePtmName the PTM to check
     * @return true if the PTM is assumed to be fixed, false otherwise.
     */
    private boolean isFixedPtm(String pridePtmName) {

        boolean fixedPtm = false;

        // @TODO: improve/extend guess!
        // guess fixed/variable
        if (pridePtmName.equalsIgnoreCase("Carbamidomethyl")
                || pridePtmName.equalsIgnoreCase("S-carboxamidomethyl-L-cysteine")
                || pridePtmName.equalsIgnoreCase("iodoacetamide - site C")
                || pridePtmName.equalsIgnoreCase("iodoacetamide derivatized residue")
                || pridePtmName.equalsIgnoreCase("Iodoacetamide derivative")
                || pridePtmName.equalsIgnoreCase("Carboxymethyl")
                || pridePtmName.equalsIgnoreCase("S-carboxymethyl-L-cysteine")
                || pridePtmName.equalsIgnoreCase("iodoacetic acid derivatized residue")) {
            fixedPtm = true;
        }

        return fixedPtm;
    }

    /**
     * Unzip the mzData file.
     *
     * @throws IOException
     */
    private void unzipProject() throws IOException {

        FileInputStream instream = new FileInputStream(currentZippedPrideDataFile);
        GZIPInputStream ginstream = new GZIPInputStream(instream);
        FileOutputStream outstream = new FileOutputStream(currentPrideDataFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = ginstream.read(buf)) > 0) {

            if (progressDialog.isRunCanceled()) {
                ginstream.close();
                outstream.close();
                instream.close();
                progressDialog.setRunFinished();
                return;
            }

            outstream.write(buf, 0, len);
        }
        ginstream.close();
        outstream.close();
        instream.close();
    }

    /**
     * Convert the PRIDE XML file to mgf.
     */
    private boolean convertPrideXmlToMgf() {

        boolean conversionOk = true;

        try {
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            PrideXmlReader prideXmlReader = new PrideXmlReader(currentPrideDataFile);
            FileWriter w = new FileWriter(currentMgfFile);
            BufferedWriter bw = new BufferedWriter(w);
            List<String> spectra = prideXmlReader.getSpectrumIds();
            int spectraCount = spectra.size();

            if (spectraCount == 0) {
                bw.close();
                w.close();
                progressDialog.setRunFinished();
                JOptionPane.showMessageDialog(this, "The file " + currentPrideDataFile.getName() + " contains no valid spectra!"
                        + "\nConversion canceled.", "No Spectra Found", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(false);
            progressDialog.setMaxPrimaryProgressCounter(spectraCount);
            progressDialog.setValue(0);

            int validSpectrumCount = 0;

            for (String spectrumId : spectra) {

                if (progressDialog.isRunCanceled()) {
                    bw.close();
                    w.close();
                    progressDialog.setRunFinished();
                    return false;
                }

                Spectrum spectrum = prideXmlReader.getSpectrumById(spectrumId);
                boolean valid = asMgf(spectrum, bw);

                if (valid) {
                    validSpectrumCount++;
                }

                progressDialog.increasePrimaryProgressCounter();
            }

            if (validSpectrumCount == 0) {
                progressDialog.setRunFinished();
                JOptionPane.showMessageDialog(this, "The file " + currentPrideDataFile.getName() + " contains no valid spectra!"
                        + "\nConversion canceled.", "No Spectra Found", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            bw.close();
            w.close();
        } catch (IOException ex) {
            ex.printStackTrace(); // @TODO: add better error handling!!!
        }

        return conversionOk;
    }

    /**
     * Writes the given spectrum to the buffered writer.
     *
     * @param spectrum
     * @param bw
     * @return true of the spectrum could be converted to mgf
     * @throws IOException
     */
    public boolean asMgf(Spectrum spectrum, BufferedWriter bw) throws IOException {

        boolean valid = true;

        int msLevel = spectrum.getSpectrumDesc().getSpectrumSettings().getSpectrumInstrument().getMsLevel();

        // ignore ms levels other than 2
        if (msLevel == 2) {

            // add precursor details
            if (spectrum.getSpectrumDesc().getPrecursorList() != null
                    && spectrum.getSpectrumDesc().getPrecursorList().getPrecursor() != null
                    && spectrum.getSpectrumDesc().getPrecursorList().getPrecursor().size() > 0) {

                bw.write("BEGIN IONS" + System.getProperty("line.separator"));
                bw.write("TITLE=" + spectrum.getId() + System.getProperty("line.separator"));

                Precursor precursor = spectrum.getSpectrumDesc().getPrecursorList().getPrecursor().get(0); // get the first precursor
                List<CvParam> precursorCvParams = precursor.getIonSelection().getCvParam();
                Double precursorMz = null, precursorIntensity = null;
                String precursorRt = null;
                Integer precursorCharge = null;

                for (CvParam cvParam : precursorCvParams) {
                    if (cvParam.getAccession().equalsIgnoreCase("MS:1000744") || cvParam.getAccession().equalsIgnoreCase("PSI:1000040")) { // precursor m/z
                        precursorMz = new Double(cvParam.getValue());
                    } else if (cvParam.getAccession().equalsIgnoreCase("MS:1000042") || cvParam.getAccession().equalsIgnoreCase("PSI:1000042")) { // precursor intensity
                        precursorIntensity = new Double(cvParam.getValue());
                    } else if (cvParam.getAccession().equalsIgnoreCase("MS:1000041") || cvParam.getAccession().equalsIgnoreCase("PSI:1000041")) { // precursor charge
                        precursorCharge = new Integer(cvParam.getValue());
                    } else if (cvParam.getAccession().equalsIgnoreCase("PRIDE:0000203") || cvParam.getAccession().equalsIgnoreCase("MS:1000894")) { // precursor retention time
                        precursorRt = cvParam.getValue();
                    }
                }

                if (precursorMz != null) {
                    bw.write("PEPMASS=" + precursorMz);
                } else {
                    valid = false; // @TODO: cancel conversion??
                }

                if (precursorIntensity != null) {
                    bw.write("\t" + precursorIntensity);
                }

                bw.write(System.getProperty("line.separator"));

                if (precursorRt != null) {
                    bw.write("RTINSECONDS=" + precursorRt + System.getProperty("line.separator")); // @TODO: improve the retention time mapping, e.g., support rt windows
                }

                if (precursorCharge != null) {
                    bw.write("CHARGE=" + precursorCharge + System.getProperty("line.separator"));

                    if (maxPrecursorCharge == null || precursorCharge > maxPrecursorCharge) {
                        maxPrecursorCharge = precursorCharge;
                    }
                    if (minPrecursorCharge == null || precursorCharge < minPrecursorCharge) {
                        minPrecursorCharge = precursorCharge;
                    }
                } else {
                    //valid = false; // @TODO: can we use spectra without precursor charge??
                }

                // process all peaks by iterating over the m/z values
                Number[] mzBinaryArray = spectrum.getMzNumberArray();
                Number[] intensityArray = spectrum.getIntentArray();

                for (int i = 0; i < mzBinaryArray.length; i++) {
                    bw.write(mzBinaryArray[i].toString());
                    bw.write(" ");
                    bw.write(intensityArray[i] + System.getProperty("line.separator"));
                }

                bw.write("END IONS" + System.getProperty("line.separator") + System.getProperty("line.separator"));

            } else {
                valid = false;
            }
        } else {
            valid = false;
        }

        return valid;
    }

    /**
     * Show/hide the clear projects filters option.
     *
     * @param show if the option is to be shown or not
     */
    public void showProjectFilterRemovalOption(boolean show) {
        clearProjectFiltersLabel.setVisible(show);
    }

    /**
     * Updates the project table selection.
     */
    public void updateProjectTableSelection() {

        if (projectsTable.getRowCount() > 0) {
            projectsTableMouseReleased(null);
        } else {
            // @TODO: clear the other tables
        }

        ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects (" + projectsTable.getRowCount() + ")");
        projectsPanel.repaint();
    }

    /**
     * Set the current filter values.
     *
     * @param currentFilterValues
     * @param assaysGreaterThanFiler
     */
    public void setCurrentFilterValues(String[] currentFilterValues, boolean assaysGreaterThanFiler) {
        this.currentFilterValues = currentFilterValues;
        this.assaysGreaterThanFiler = assaysGreaterThanFiler;
    }

    /**
     * Enable or disable the Reshake button.
     */
    private void enableReshake() {

        boolean filesSelected = false;

        for (int i = 0; i < filesTable.getRowCount() && !filesSelected; i++) {
            if (((Boolean) filesTable.getValueAt(i, filesTable.getColumn("  ").getModelIndex())).booleanValue()) {
                filesSelected = true;
            }
        }

        reshakeButton.setEnabled(filesSelected);
    }

    /**
     * Save a file from a URL.
     *
     * @param saveFile
     * @param targetUrlAsString
     * @param fileSize the file size
     * @param progressDialog
     * @return the saved file
     * @throws MalformedURLException
     * @throws IOException
     * @throws java.io.FileNotFoundException
     */
    public File saveUrl(File saveFile, String targetUrlAsString, Double fileSize, ProgressDialogX progressDialog)
            throws MalformedURLException, IOException, FileNotFoundException {

        BufferedInputStream in = null;
        FileOutputStream fout = null;

        try {
            boolean urlExists = checkIfURLExists(targetUrlAsString);

            if (!urlExists) {
                if (targetUrlAsString.endsWith(".gz")) {
                    targetUrlAsString = targetUrlAsString.substring(0, targetUrlAsString.length() - 3);
                    saveFile = new File(saveFile.getAbsolutePath().substring(0, saveFile.getAbsolutePath().length() - 3));
                }
            }

            URL targetUrl = new URL(targetUrlAsString);
            URLConnection urlConnection = targetUrl.openConnection();

            if (password != null) {
                String userpass = userName + ":" + password;
                String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }

            int contentLength = urlConnection.getContentLength();

            if (contentLength != -1) {
                progressDialog.resetPrimaryProgressCounter();
                progressDialog.setMaxPrimaryProgressCounter(contentLength);
            } else {
                progressDialog.resetPrimaryProgressCounter();
                contentLength = (int) (fileSize * 1024 * 1024);
                progressDialog.setMaxPrimaryProgressCounter(contentLength);
            }

            in = new BufferedInputStream(urlConnection.getInputStream());

            fout = new FileOutputStream(saveFile);

            long start = System.currentTimeMillis();

            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1 && !progressDialog.isRunCanceled()) {
                fout.write(data, 0, count);

                if (contentLength != -1) {

                    long now = System.currentTimeMillis();

                    if ((now - start) > 100) {
                        progressDialog.setPrimaryProgressCounter((int) saveFile.length());
                        start = System.currentTimeMillis();
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }

        return saveFile;
    }

    /**
     * Check if a given URL exists.
     *
     * @param targetUrlAsString the URL to check
     * @return true of it exists
     */
    public boolean checkIfURLExists(String targetUrlAsString) {

        try {
            URL targetUrl = new URL(targetUrlAsString);
            URLConnection urlConnection = targetUrl.openConnection();

            if (password != null) {
                String userpass = userName + ":" + password;
                String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }

            InputStream inputStream = urlConnection.getInputStream();
            inputStream.close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the authorization details.
     *
     * @return the authorization details
     */
    private HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        String authString = userName + ":" + password;
        byte[] encodedAuthorisation = Base64.encodeBase64(authString.getBytes());
        headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
        return new HttpEntity<String>(headers);
    }

    /**
     * Returns the files table.
     *
     * @return the files table
     */
    public JTable getFilesTable() {
        return filesTable;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JLabel accessPrivateDataLabel;
    private javax.swing.JLabel assayHelpLabel;
    private javax.swing.JScrollPane assayTableScrollPane;
    private javax.swing.JPanel assaysPanel;
    private javax.swing.JTable assaysTable;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel browsePublicDataLabel;
    private javax.swing.JLabel clearProjectFiltersLabel;
    private javax.swing.JLabel dataTypeSeparatorLabel;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel filesHelpLabel;
    private javax.swing.JPanel filesPanel;
    private javax.swing.JTable filesTable;
    private javax.swing.JScrollPane filesTableScrollPane;
    private javax.swing.JMenuItem findMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel peptideShakerHomePageLabel;
    private javax.swing.JLabel projectHelpLabel;
    private javax.swing.JPanel projectsPanel;
    private javax.swing.JScrollPane projectsScrollPane;
    private javax.swing.JTable projectsTable;
    private javax.swing.JCheckBox reshakableCheckBox;
    private javax.swing.JButton reshakeButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the list of reshakable file types.
     *
     * @return the reshakeableFiles
     */
    public HashMap<String, ArrayList<String>> getReshakeableFiles() {
        return reshakeableFiles;
    }

    /**
     * Returns the list of files that search settings can be extracted from.
     *
     * @return the searchSettingsFiles
     */
    public HashMap<String, ArrayList<String>> getSearchSettingsFiles() {
        return searchSettingsFiles;
    }

    /**
     * Returns the list of species for the currently selected assay or project.
     * Null if no assay or project is currently selected.
     *
     * @return the list of species for the currently selected assay or project
     */
    public String getCurrentSpeciesList() {

        int assayRow = assaysTable.getSelectedRow();

        if (assayRow != -1 && assaysTable.getValueAt(assayRow, assaysTable.getColumn("Species").getModelIndex()) != null) {
            return (String) assaysTable.getValueAt(assayRow, assaysTable.getColumn("Species").getModelIndex());
        } else {
            int projectRow = projectsTable.getSelectedRow();

            if (projectRow != -1) {
                return (String) projectsTable.getValueAt(projectRow, projectsTable.getColumn("Species").getModelIndex());
            } else {
                return null;
            }
        }
    }

    /**
     * Convert a list of PRIDE PTM names to utilities modifications.
     *
     * @param allPtms the PTMs to convert
     * @param modProfile the modification profile to add the PTMs to
     * @return a string with the conversion details
     */
    private String convertPtms(String allPtms, ModificationProfile modProfile) {

        ArrayList<String> unknownPtms = new ArrayList<String>();
        String report = "<br><br><b>Post-Translational Modifications:</b>";

        if (allPtms != null) {

            if (allPtms.trim().length() > 0) {

                String[] tempPtms = allPtms.split("; ");

                for (String pridePtmName : tempPtms) {
                    report += ptmFactory.convertPridePtm(pridePtmName, modProfile, unknownPtms, isFixedPtm(pridePtmName));
                }
            } else {
                report += "<br>(none detected)";
            }
        } else {
            report += "<br>(none detected)";
        }

        // handle the unknown ptms
        if (!unknownPtms.isEmpty()) {

//            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            for (String unknownPtm : unknownPtms) {
                report += "<br>" + unknownPtm + " (unknown ptm) *"; // @TODO: have the user select them!!
            }

            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        }

        report += "<br>";

        if (!unknownPtms.isEmpty()) {
            report += "<br>* Remember to add these PTMs manually in SearchGUI."; // @TODO: this warning should be stronger!!
        }

        return report;
    }
}
