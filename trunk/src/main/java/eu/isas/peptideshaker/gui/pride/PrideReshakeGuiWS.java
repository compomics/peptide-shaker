package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import com.compomics.util.experiment.identification.SearchParameters;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import org.jfree.chart.plot.PlotOrientation;
import no.uib.jsparklines.renderers.*;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.compomics.util.gui.DummyFrame;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.searchsettings.EnzymeSelectionDialog;
import com.compomics.util.io.FTPDownloader;
import eu.isas.peptideshaker.gui.WelcomeDialog;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.text.SimpleDateFormat;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetail;
import uk.ac.ebi.pride.archive.web.service.model.assay.AssayDetailList;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetail;
import uk.ac.ebi.pride.archive.web.service.model.file.FileDetailList;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.web.service.model.project.ProjectDetailList;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

/**
 * Dialog for talking to the PRIDE Archive web service to select projects for 
 * reshaking. (Work in progress...)
 *
 * @author Harald Barsnes
 */
public class PrideReshakeGuiWS extends javax.swing.JDialog {

    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of species for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> speciesForProject;
    /**
     * The list of PTMs for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> ptmsForProject;
    /**
     * The list of taxonomies for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> taxonomyForProject;
    /**
     * Total number of PRIDE projects.
     */
    private int totalNumberOfPrideProjects;
    /**
     * The list of currently selected species.
     */
    private ArrayList<String> currentSpecies;
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
    private URL currentPrideProjectUrl;
    /**
     * The current zipped PRIDE XML file.
     */
    private File currentZippedPrideXmlFile;
    /**
     * The current PRIDE XML file.
     */
    private File currentPrideXmlFile;
    /**
     * The current mgf file.
     */
    private File currentMgfFile;
    /**
     * The current URL content length.
     */
    private int currentUrlContentLength;
    /**
     * True of a file is currently being downloaded.
     */
    private boolean isFileBeingDownloaded = false;
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
    /**
     * A dummy parent frame to be able to show an icon in the task bar.
     */
    private DummyFrame dummyParentFrame;
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
     * The web service URL.
     */
    private static final String projectServiceURL = "http://wwwdev.ebi.ac.uk/pride/ws/archive/";

    /**
     * Creates a new PrideReshakeGui dialog.
     *
     * @param peptideShakerGUI
     * @param welcomeDialog a reference to the welcome dialog
     * @param dummyParentFrame dummy parent frame to be able to show an icon in
     * the task bar, can be null
     * @param modal
     */
    public PrideReshakeGuiWS(PeptideShakerGUI peptideShakerGUI, WelcomeDialog welcomeDialog, DummyFrame dummyParentFrame, boolean modal) {
        super(peptideShakerGUI, modal);
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        this.welcomeDialog = welcomeDialog;
        this.dummyParentFrame = dummyParentFrame;
        setUpGui();
        insertData();

        if (peptideShakerGUI.isVisible()) {
            this.setSize(peptideShakerGUI.getWidth() - 200, peptideShakerGUI.getHeight() - 200);
        }

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        projectsTable.getColumn("Accession").setMaxWidth(90);
        projectsTable.getColumn("Accession").setMinWidth(90);
        projectsTable.getColumn(" ").setMaxWidth(50);
        projectsTable.getColumn(" ").setMinWidth(50);
        projectsTable.getColumn("Details").setMaxWidth(55);
        projectsTable.getColumn("Details").setMinWidth(55);

        searchTable.getColumn("Accession").setMaxWidth(90);
        searchTable.getColumn("Accession").setMinWidth(90);
        searchTable.getColumn(" ").setMaxWidth(50);
        searchTable.getColumn(" ").setMinWidth(50);
        searchTable.getColumn("Details").setMaxWidth(55);
        searchTable.getColumn("Details").setMinWidth(55);

        assaysTable.getColumn("Accession").setMaxWidth(90);
        assaysTable.getColumn("Accession").setMinWidth(90);
        assaysTable.getColumn(" ").setMaxWidth(50);
        assaysTable.getColumn(" ").setMinWidth(50);
        assaysTable.getColumn("Details").setMaxWidth(55);
        assaysTable.getColumn("Details").setMinWidth(55);

        filesTable.getColumn("Assay").setMaxWidth(90);
        filesTable.getColumn("Assay").setMinWidth(90);
        filesTable.getColumn(" ").setMaxWidth(50);
        filesTable.getColumn(" ").setMinWidth(50);
        filesTable.getColumn("  ").setMaxWidth(30);
        filesTable.getColumn("  ").setMinWidth(30);
        filesTable.getColumn("Type").setMaxWidth(90);
        filesTable.getColumn("Type").setMinWidth(90);
        filesTable.getColumn("Size").setMaxWidth(90);
        filesTable.getColumn("Size").setMinWidth(90);

        // make sure that the scroll panes are see-through
        projectsScrollPane.getViewport().setOpaque(false);
        searchScrollPane.getViewport().setOpaque(false);
        assayTableScrollPane.getViewport().setOpaque(false);
        filesTableScrollPane.getViewport().setOpaque(false);

        projectsTable.setAutoCreateRowSorter(true);
        assaysTable.setAutoCreateRowSorter(true);
        filesTable.setAutoCreateRowSorter(true);

        projectsTable.getTableHeader().setReorderingAllowed(false);
        searchTable.getTableHeader().setReorderingAllowed(false);
        assaysTable.getTableHeader().setReorderingAllowed(false);
        filesTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel projectsCorner = new JPanel();
        projectsCorner.setBackground(projectsTable.getTableHeader().getBackground());
        projectsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, projectsCorner);

        JPanel searchCorner = new JPanel();
        searchCorner.setBackground(searchTable.getTableHeader().getBackground());
        searchScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, searchCorner);

        JPanel assayCorner = new JPanel();
        assayCorner.setBackground(assaysTable.getTableHeader().getBackground());
        assayTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, assayCorner);

        JPanel filesCorner = new JPanel();
        filesCorner.setBackground(filesTable.getTableHeader().getBackground());
        filesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, filesCorner);

        projectsTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        assaysTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        projectsTable.getColumn("Details").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        assaysTable.getColumn("Details").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        filesTable.getColumn("Assay").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        filesTable.getColumn("File").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        filesTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());

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
        projectsTableToolTips.add("Details");

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
        assaysTableToolTips.add("Details");

        filesTableToolTips = new ArrayList<String>();
        filesTableToolTips.add(null);
        filesTableToolTips.add("Assay Accession Numbers");
        filesTableToolTips.add("File Type");
        filesTableToolTips.add("File Name and Link");
        filesTableToolTips.add("File Size");
        filesTableToolTips.add("Reshake");
    }

    /**
     * Insert the PRIDE project data.
     */
    private void insertData() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel projectsTableModel = (DefaultTableModel) projectsTable.getModel();
        projectsTableModel.getDataVector().removeAllElements();
        projectsTableModel.fireTableDataChanged();

        RestTemplate template = new RestTemplate();

        // get the project count
        ResponseEntity<Integer> projectCountResult = template.getForEntity(projectServiceURL + "project/count", Integer.class);
        Integer projectCount = projectCountResult.getBody();

        // get the list of projects
        ResponseEntity<ProjectDetailList> projectList = template.getForEntity(projectServiceURL + "project/list?show=" + projectCount + "&page=1&sort=publication_date&order=desc", ProjectDetailList.class);

        double maxNumAssays = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // iterate the project and add them to the table
        for (ProjectDetail projectDetail : projectList.getBody().getList()) {

            if (!hideNonValidProjectsCheckBox.isSelected() || (hideNonValidProjectsCheckBox.isSelected() && projectDetail.getNumAssays() > 0)) {

                ((DefaultTableModel) projectsTable.getModel()).addRow(new Object[]{
                    (projectsTable.getRowCount() + 1),
                    "<html><a href=\"" + DisplayFeaturesGenerator.getPrideProjectArchiveLink("" + projectDetail.getAccession())
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + projectDetail.getAccession() + "</font></a><html>",
                    projectDetail.getTitle(),
                    setToString(projectDetail.getSpecies()),
                    setToString(projectDetail.getTissues()),
                    setToString(projectDetail.getPtmNames()),
                    setToString(projectDetail.getInstrumentNames()),
                    projectDetail.getNumAssays(),
                    projectDetail.getSubmissionType(),
                    dateFormat.format(projectDetail.getPublicationDate()),
                    "<html><a href=\"" + "dummy link"
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + "more..." + "</font></a><html>"});

                if (projectDetail.getNumAssays() > maxNumAssays) {
                    maxNumAssays = projectDetail.getNumAssays();
                }
            }
        }

        totalNumberOfPrideProjects = projectsTable.getRowCount();

        ((TitledBorder) projectsPanel.getBorder()).setTitle("PRIDE Projects (" + projectsTable.getRowCount() + ")");
        projectsPanel.repaint();

        // update the sparklines with the max values
        projectsTable.getColumn("#Assays").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumAssays, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setLogScale(true);
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setMinimumChartValue(2.0);

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Convert a set of strings to a single string.
     *
     * @param set
     * @return
     */
    private String setToString(Set<String> set) {
        Object[] elements = set.toArray();

        String result = "";
        for (Object object : elements) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.toString();
        }

        return result;
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
        localProjectsFolderLabel = new javax.swing.JLabel();
        projectNotFoundLabel = new javax.swing.JLabel();
        hideNonValidProjectsCheckBox = new javax.swing.JCheckBox();
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
        searchPanel = new javax.swing.JPanel();
        searchScrollPane = new javax.swing.JScrollPane();
        searchTable = new JTable() {
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
        reshakeButton = new javax.swing.JButton();
        searchFiltersLabel = new javax.swing.JLabel();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PRIDE - Public Projects");
        setMinimumSize(new java.awt.Dimension(1280, 750));

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        projectsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PRIDE Projects"));
        projectsPanel.setOpaque(false);

        projectsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Title", "Species", "Tissue", "PTMs", "Instrument", "#Assays", "Type", "Date", "Details"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
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
                projectsTablKeyReleased(evt);
            }
        });
        projectsScrollPane.setViewportView(projectsTable);

        localProjectsFolderLabel.setText("<html><a href>Edit Local Projects Folder</html>");
        localProjectsFolderLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                localProjectsFolderLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                localProjectsFolderLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                localProjectsFolderLabelMouseReleased(evt);
            }
        });

        projectNotFoundLabel.setText("<html><a href>Project not found?</html>");
        projectNotFoundLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                projectNotFoundLabelMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                projectNotFoundLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                projectNotFoundLabelMouseExited(evt);
            }
        });

        hideNonValidProjectsCheckBox.setSelected(true);
        hideNonValidProjectsCheckBox.setText("Hide projects without assays");
        hideNonValidProjectsCheckBox.setIconTextGap(10);
        hideNonValidProjectsCheckBox.setOpaque(false);
        hideNonValidProjectsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideNonValidProjectsCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout projectsPanelLayout = new javax.swing.GroupLayout(projectsPanel);
        projectsPanel.setLayout(projectsPanelLayout);
        projectsPanelLayout.setHorizontalGroup(
            projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(projectsPanelLayout.createSequentialGroup()
                        .addComponent(projectsScrollPane)
                        .addContainerGap())
                    .addGroup(projectsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(projectNotFoundLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(hideNonValidProjectsCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(localProjectsFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))))
        );
        projectsPanelLayout.setVerticalGroup(
            projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(localProjectsFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectNotFoundLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hideNonValidProjectsCheckBox))
                .addGap(4, 4, 4))
        );

        assaysPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Assays"));
        assaysPanel.setOpaque(false);

        assaysTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Title", "Species", "Sample", "PTMs", "Diseases", "#Proteins", "#Peptides", "#Spectra", "Details"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
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

        javax.swing.GroupLayout assaysPanelLayout = new javax.swing.GroupLayout(assaysPanel);
        assaysPanel.setLayout(assaysPanelLayout);
        assaysPanelLayout.setHorizontalGroup(
            assaysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(assaysPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(assayTableScrollPane)
                .addContainerGap())
        );
        assaysPanelLayout.setVerticalGroup(
            assaysPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(assaysPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(assayTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                .addContainerGap())
        );

        searchPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search PRIDE *"));
        searchPanel.setOpaque(false);

        searchTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                " ", "Accession", "Title", "Species", "Tissue", "PTMs", "Instrument", "#Assays", "Type", "Date", "Details"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, true, true, true, true, true, true, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        searchTable.setSelectionBackground(new java.awt.Color(255, 255, 255));
        searchTable.setSelectionForeground(new java.awt.Color(0, 0, 0));
        searchTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                searchTableMouseClicked(evt);
            }
        });
        searchTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchTableKeyReleased(evt);
            }
        });
        searchScrollPane.setViewportView(searchTable);

        javax.swing.GroupLayout searchPanelLayout = new javax.swing.GroupLayout(searchPanel);
        searchPanel.setLayout(searchPanelLayout);
        searchPanelLayout.setHorizontalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchScrollPane)
                .addContainerGap())
        );
        searchPanelLayout.setVerticalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        reshakeButton.setBackground(new java.awt.Color(0, 153, 0));
        reshakeButton.setFont(reshakeButton.getFont().deriveFont(reshakeButton.getFont().getStyle() | java.awt.Font.BOLD));
        reshakeButton.setForeground(new java.awt.Color(255, 255, 255));
        reshakeButton.setText("Reshake PRIDE Projects");
        reshakeButton.setEnabled(false);
        reshakeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reshakeButtonActionPerformed(evt);
            }
        });

        searchFiltersLabel.setFont(searchFiltersLabel.getFont().deriveFont((searchFiltersLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        searchFiltersLabel.setText("* Supported filter options: partial (case sensitive) text; and number, >number and <number for the spectra, peptides and proteins columns.");

        filesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Files"));
        filesPanel.setOpaque(false);

        filesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Assay", "Type", "File", "Size", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, true
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

        javax.swing.GroupLayout filesPanelLayout = new javax.swing.GroupLayout(filesPanel);
        filesPanel.setLayout(filesPanelLayout);
        filesPanelLayout.setHorizontalGroup(
            filesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesTableScrollPane)
                .addContainerGap())
        );
        filesPanelLayout.setVerticalGroup(
            filesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(searchFiltersLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 393, Short.MAX_VALUE)
                        .addComponent(reshakeButton)
                        .addGap(14, 14, 14))
                    .addComponent(assaysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(projectsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(filesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(projectsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(assaysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reshakeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchFiltersLabel))
                .addContainerGap())
        );

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
     * Filters the project table according to the current filter settings.
     *
     * @param evt
     */
    private void searchTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchTableKeyReleased
        filter();
    }//GEN-LAST:event_searchTableKeyReleased

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
            } else if (column == projectsTable.getColumn("Details").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) projectsTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {
                // @TODO: open dialog with more project details
            }
        }
    }//GEN-LAST:event_projectsTableMouseReleased

    /**
     * Update the info about the selected project.
     *
     * @param evt
     */
    private void projectsTablKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectsTablKeyReleased
        updateProjectFileList();
        updateAssayList();
    }//GEN-LAST:event_projectsTablKeyReleased

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
                && (column == projectsTable.getColumn("Accession").getModelIndex() || column == projectsTable.getColumn("Details").getModelIndex())
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
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void projectsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_projectsTableMouseExited

    /**
     * Reshake the selected PRIDE experiments.
     *
     * @param evt
     */
    private void reshakeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reshakeButtonActionPerformed

        File selectedFolder = Util.getUserSelectedFolder(this, "Select Output Folder", peptideShakerGUI.getLastSelectedFolder(), "Output Folder", "Select", false);

        if (selectedFolder != null) {
            peptideShakerGUI.setLastSelectedFolder(selectedFolder.getAbsolutePath());
            outputFolder = selectedFolder.getAbsolutePath();
            ArrayList<Integer> selectedProjects = new ArrayList<Integer>();
            currentSpecies = new ArrayList<String>();

            for (int i = 0; i < projectsTable.getRowCount(); i++) {
                if ((Boolean) projectsTable.getValueAt(i, projectsTable.getColumn(" ").getModelIndex())) {
                    selectedProjects.add((Integer) projectsTable.getValueAt(i, projectsTable.getColumn("Accession").getModelIndex()));
                }
            }

            boolean download = true;

            // check if multiple projects are selected
            if (selectedProjects.size() > 1) {
                int value = JOptionPane.showConfirmDialog(this,
                        "Note that if multiple projects are selected the search\n"
                        + "parameters from the first project in the list is used.",
                        "Search Parameters", JOptionPane.OK_CANCEL_OPTION);

                if (value == JOptionPane.CANCEL_OPTION) {
                    download = false;
                }
            }

            if (download) {
                downloadPrideDatasets(selectedProjects);
            }
        }
    }//GEN-LAST:event_reshakeButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void localProjectsFolderLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_localProjectsFolderLabelMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_localProjectsFolderLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void localProjectsFolderLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_localProjectsFolderLabelMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_localProjectsFolderLabelMouseExited

    /**
     * Edit the local projects folder.
     *
     * @param evt
     */
    private void localProjectsFolderLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_localProjectsFolderLabelMouseReleased
        File selectedFolder = Util.getUserSelectedFolder(this, "Select Local PRIDE Projects Folder", peptideShakerGUI.getLastSelectedFolder(), "PRIDE XML Folder", "Select", false);

        if (selectedFolder != null) {

            peptideShakerGUI.setLastSelectedFolder(selectedFolder.getAbsolutePath());

            // reload the user preferences as these may have been changed by other tools
            try {
                peptideShakerGUI.setUtilitiesUserPreferences(UtilitiesUserPreferences.loadUserPreferences());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "An error occured when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

            peptideShakerGUI.getUtilitiesUserPreferences().setLocalPrideFolder(selectedFolder.getAbsolutePath());
            insertData();
        }
    }//GEN-LAST:event_localProjectsFolderLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void projectNotFoundLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectNotFoundLabelMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_projectNotFoundLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void projectNotFoundLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectNotFoundLabelMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_projectNotFoundLabelMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void projectNotFoundLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectNotFoundLabelMouseReleased
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(this, getClass().getResource("/helpFiles/PrideReshakeDialog.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_projectNotFoundLabelMouseReleased

    /**
     * Show/hide the non-validated projects.
     *
     * @param evt
     */
    private void hideNonValidProjectsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideNonValidProjectsCheckBoxActionPerformed
        insertData();
    }//GEN-LAST:event_hideNonValidProjectsCheckBoxActionPerformed

    /**
     * Filter the projects.
     *
     * @param evt
     */
    private void searchTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchTableMouseClicked
        filter();
    }//GEN-LAST:event_searchTableMouseClicked

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
                && (column == assaysTable.getColumn("Accession").getModelIndex() || column == assaysTable.getColumn("Details").getModelIndex())
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
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void assaysTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_assaysTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_assaysTableMouseExited

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
            } else if (column == assaysTable.getColumn("Details").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) assaysTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {
                // @TODO: open dialog with more assay details
            }
        }
    }//GEN-LAST:event_assaysTableMouseReleased

    /**
     * Show the files for the given assay
     *
     * @param evt
     */
    private void assaysTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_assaysTableKeyReleased
        updateAssayFileList();
    }//GEN-LAST:event_assaysTableKeyReleased

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
            } else if (column == filesTable.getColumn("File").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) filesTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {
                String link = (String) filesTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_filesTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
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
                && (column == filesTable.getColumn("Assay").getModelIndex() || column == filesTable.getColumn("File").getModelIndex())
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane assayTableScrollPane;
    private javax.swing.JPanel assaysPanel;
    private javax.swing.JTable assaysTable;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JPanel filesPanel;
    private javax.swing.JTable filesTable;
    private javax.swing.JScrollPane filesTableScrollPane;
    private javax.swing.JCheckBox hideNonValidProjectsCheckBox;
    private javax.swing.JLabel localProjectsFolderLabel;
    private javax.swing.JLabel projectNotFoundLabel;
    private javax.swing.JPanel projectsPanel;
    private javax.swing.JScrollPane projectsScrollPane;
    private javax.swing.JTable projectsTable;
    private javax.swing.JButton reshakeButton;
    private javax.swing.JLabel searchFiltersLabel;
    private javax.swing.JPanel searchPanel;
    private javax.swing.JScrollPane searchScrollPane;
    private javax.swing.JTable searchTable;
    // End of variables declaration//GEN-END:variables

    /**
     * Download and convert a PRIDE project to mgf.
     *
     * @param accession the accession numbers of the PRIDE projects
     */
    private void downloadPrideDatasets(ArrayList<Integer> aSelectedProjects) {

        final ArrayList<Integer> selectedProjects = aSelectedProjects;
        final PrideReshakeGuiWS finalRef = this;
        maxPrecursorCharge = null;
        minPrecursorCharge = null;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);

        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        progressDialog.setTitle("Downloading PRIDE Project. Please Wait...");
        isFileBeingDownloaded = true;

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
                    String prideSearchParametersReport = null;
                    ArrayList<File> mgfFiles = new ArrayList<File>();
                    boolean mgfConversionOk = true;

                    for (int i = 0; i < selectedProjects.size() && mgfConversionOk; i++) {

                        if (progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            if (dummyParentFrame != null) {
                                dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            }
                            return;
                        }

                        final Integer prideAccession = selectedProjects.get(i);
                        final int counter = i;

                        if (selectedProjects.size() > 1) {
                            progressDialog.setTitle("Downloading PRIDE Project (" + (i + 1) + "/" + selectedProjects.size() + "). Please Wait...");
                        } else {
                            progressDialog.setTitle("Downloading PRIDE Project. Please Wait...");
                        }

                        try {
                            currentPrideProjectUrl = new URL("ftp://ftp.ebi.ac.uk/pub/databases/pride/PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml.gz");
                            currentZippedPrideXmlFile = new File(outputFolder, "PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml.gz");
                            currentPrideXmlFile = new File(outputFolder, "PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml");
                            currentMgfFile = new File(outputFolder, "PRIDE_Exp_Complete_Ac_" + prideAccession + ".mgf");
                            mgfFiles.add(currentMgfFile);
                            URLConnection conn = currentPrideProjectUrl.openConnection();
                            currentUrlContentLength = conn.getContentLength();
                            // currentUrlContentLength = conn.getContentLengthLong(): // @TODO: requires Java 7...

                        } catch (MalformedURLException ex) {
                            JOptionPane.showMessageDialog(finalRef, JOptionEditorPane.getJOptionEditorPane("The PRIDE XML file could not be downloaded:<br>"
                                    + ex.getMessage() + ".<br>"
                                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                                    "Download Error", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                            currentPrideProjectUrl = null;
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(finalRef, JOptionEditorPane.getJOptionEditorPane("The PRIDE XML file could not be downloaded:<br>"
                                    + ex.getMessage() + ".<br>"
                                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                                    "Download Error", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                            currentPrideProjectUrl = null;
                        }

                        if (progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            if (dummyParentFrame != null) {
                                dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            }
                            return;
                        }

                        if (currentPrideProjectUrl != null) {
                            if (currentUrlContentLength != -1) {
                                progressDialog.setPrimaryProgressCounterIndeterminate(false);
                                progressDialog.setValue(0);
                                progressDialog.setMaxPrimaryProgressCounter(currentUrlContentLength);
                            } else {
                                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                            }

                            isFileBeingDownloaded = true;

                            new Thread("DownloadMonitorThread") {
                                @Override
                                public void run() {

                                    long start = System.currentTimeMillis();

                                    while (isFileBeingDownloaded) {

                                        if (progressDialog.isRunCanceled()) {
                                            progressDialog.setRunFinished();
                                            if (dummyParentFrame != null) {
                                                dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                            }
                                            return;
                                        }

                                        long now = System.currentTimeMillis();

                                        // update the progress dialog every 100 millisecond or so
                                        if ((now - start) > 100 && progressDialog != null) {
                                            long length = currentZippedPrideXmlFile.length();

                                            if (currentUrlContentLength != -1) {
                                                progressDialog.setValue((int) length);
                                            }

                                            if (selectedProjects.size() > 1) {
                                                progressDialog.setTitle("Downloading PRIDE Project (" + (counter + 1) + "/" + selectedProjects.size()
                                                        + "). Please Wait... (" + (length / (1024L * 1024L)) + " MB)");
                                            } else {
                                                progressDialog.setTitle("Downloading PRIDE Project. Please Wait... (" + (length / (1024L * 1024L)) + " MB)");
                                            }

                                            start = System.currentTimeMillis();
                                        }
                                    }
                                }
                            }.start();

                            if (!new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(),
                                    "PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml").exists()) {

                                boolean downloadFile = true;

                                if (currentPrideXmlFile.exists()) {
                                    int option = JOptionPane.showConfirmDialog(PrideReshakeGuiWS.this,
                                            "The PRIDE file \'" + currentPrideXmlFile.getName() + "\' already exists locally.\nUse local copy?",
                                            "Use Local File?", JOptionPane.YES_NO_OPTION);

                                    downloadFile = (option == JOptionPane.NO_OPTION);
                                }

                                if (downloadFile) {

                                    // download the pride xml file
                                    FTPDownloader ftpDownloader = new FTPDownloader("ftp.ebi.ac.uk", false);
                                    ftpDownloader.downloadFile(currentPrideProjectUrl.getPath(), currentZippedPrideXmlFile);
                                    ftpDownloader.disconnect();

                                    isFileBeingDownloaded = false;

                                    // file downloaded, unzip file
                                    if (selectedProjects.size() > 1) {
                                        progressDialog.setTitle("Unzipping PRIDE Project (" + (i + 1) + "/" + selectedProjects.size() + "). Please Wait...");
                                    } else {
                                        progressDialog.setTitle("Unzipping PRIDE Project. Please Wait...");
                                    }
                                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                                    unzipProject();
                                } else {
                                    isFileBeingDownloaded = false;
                                }
                            } else {
                                isFileBeingDownloaded = false;
                                currentPrideXmlFile = new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(),
                                        "PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml");
                            }

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
                                if (dummyParentFrame != null) {
                                    dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                }
                                return;
                            }

                            // file unzipped, time to start the conversion to mgf
                            if (selectedProjects.size() > 1) {
                                progressDialog.setTitle("Converting PRIDE Project (" + (i + 1) + "/" + selectedProjects.size() + "). Please Wait...");
                            } else {
                                progressDialog.setTitle("Converting PRIDE Project. Please Wait...");
                            }

                            mgfConversionOk = convertPrideXmlToMgf();

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
                                if (dummyParentFrame != null) {
                                    dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                }
                                return;
                            }

                            if (mgfConversionOk) {
                                // get the search params from the pride xml file
                                if (i == 0) {
                                    progressDialog.setTitle("Getting Search Settings. Please Wait...");
                                    prideSearchParametersReport = getSearchParams(prideAccession, prideSearchParameters);
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

                    if (dummyParentFrame != null) {
                        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    }

                    if (mgfConversionOk) {

                        // @TODO: support more species, but then the terms might have to be downloaded as well...
                        String selectedSpecies = null;
                        String selectedSpeciesType = null;
                        if (currentSpecies.size() == 1 && currentSpecies.get(0).equalsIgnoreCase("Homo sapiens (Human)")) {
                            selectedSpecies = "Homo sapiens";
                            selectedSpeciesType = "Vertebrates";
                        }

                        if (welcomeDialog != null) {
                            welcomeDialog.setVisible(false);
                        }

                        PrideReshakeGuiWS.this.setVisible(false);

                        // display the detected search parameters to the user
                        new PrideSearchParametersDialog(peptideShakerGUI,
                                new File(outputFolder, "pride.parameters"), prideSearchParametersReport, mgfFiles, selectedSpecies, selectedSpeciesType, true);
                    }

                } catch (Exception e) {
                    if (dummyParentFrame != null) {
                        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    }
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef,
                            "An error occured when trying to convert the PRIDE project:\n"
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
    private String getSearchParams(Integer prideAccession, SearchParameters prideSearchParameters) throws Exception {

        progressDialog.setPrimaryProgressCounterIndeterminate(true);

        Double fragmentIonMassTolerance = null;
        Double peptideIonMassTolerance = null;
        Integer maxMissedCleavages = null;
        ArrayList<String> enzymes = new ArrayList<String>();

        PrideXmlReader prideXmlReader = new PrideXmlReader(currentPrideXmlFile);

        Description description = prideXmlReader.getDescription();
        DataProcessing dataProcessing = description.getDataProcessing();

        if (dataProcessing != null && dataProcessing.getProcessingMethod() != null) {
            List<CvParam> processingMethods = dataProcessing.getProcessingMethod().getCvParam();

            for (CvParam cvParam : processingMethods) {
                if (cvParam.getAccession().equalsIgnoreCase("PRIDE:0000161")) { // fragment mass tolerance
                    fragmentIonMassTolerance = new Double(cvParam.getValue());
                } else if (cvParam.getAccession().equalsIgnoreCase("PRIDE:0000078")) { // peptide mass tolerance
                    peptideIonMassTolerance = new Double(cvParam.getValue());
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

        for (String id : ids) {

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

        prideParametersReport = "";
        prideParametersReport += "<html><br><b><u>Extracted Search Parameters</u></b><br>";

        // set the fragment ion accuracy
        prideParametersReport += "<br><b>Fragment Ion Mass Tolerance:</b> ";
        if (fragmentIonMassTolerance != null) {
            prideSearchParameters.setFragmentIonAccuracy(fragmentIonMassTolerance);
            prideParametersReport += fragmentIonMassTolerance + " Da";
        } else {
            prideParametersReport += prideSearchParameters.getFragmentIonAccuracy() + " Da (default)";
        }

        // set the precuros ion accuracy
        prideParametersReport += "<br><b>Precursor Ion Mass Tolerance:</b> ";
        if (peptideIonMassTolerance != null) {
            prideSearchParameters.setPrecursorAccuracy(peptideIonMassTolerance); // @TODO: ppm assumed?
            prideParametersReport += peptideIonMassTolerance + " ppm";
        } else {
            prideParametersReport += prideSearchParameters.getPrecursorAccuracy() + " ppm (default)";
        }

        // set the max missed cleavages
        prideParametersReport += "<br><b>Maximum Missed Cleavages:</b> ";
        if (maxMissedCleavages != null) {
            prideSearchParameters.setnMissedCleavages(maxMissedCleavages);
            prideParametersReport += maxMissedCleavages;
        } else {
            prideParametersReport += prideSearchParameters.getnMissedCleavages() + " (default)";
        }

        // taxonomy and species
        prideParametersReport += "<br><br><b>Species:</b> ";
        if (speciesForProject.get(prideAccession) == null || speciesForProject.get(prideAccession).length() == 0) {
            prideParametersReport += "unknown";
            currentSpecies.add(null);
        } else {
            prideParametersReport += speciesForProject.get(prideAccession);
            currentSpecies.add(speciesForProject.get(prideAccession));
        }
        prideParametersReport += "<br><b>Taxonomy:</b> ";
        if (taxonomyForProject.get(prideAccession) == null || taxonomyForProject.get(prideAccession).length() == 0) {
            prideParametersReport += "unknown";
        } else {
            prideParametersReport += taxonomyForProject.get(prideAccession);
        }

        // help the user get the correct database
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        new DatabaseHelpDialog(peptideShakerGUI, prideSearchParameters, true, speciesForProject.get(prideAccession), taxonomyForProject.get(prideAccession));
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        // map the ptms to utilities ptms
        String allPtms = ptmsForProject.get(prideAccession);
        ArrayList<String> unknownPtms = new ArrayList<String>();

        ModificationProfile modProfile = new ModificationProfile();

        prideParametersReport += "<br><br><b>Post-Translational Modifications:</b>";

        if (allPtms != null) {

            if (allPtms.trim().length() > 0) {

                String[] tempPtms = allPtms.split(";");

                for (String pridePtmName : tempPtms) {
                    prideParametersReport += ptmFactory.convertPridePtm(pridePtmName, modProfile, unknownPtms, isFixedPtm(pridePtmName));
                }
            } else {
                prideParametersReport += "<br>(none detected)";
            }
        } else {
            prideParametersReport += "<br>(none detected)";
        }

        // handle the unknown ptms
        if (!unknownPtms.isEmpty()) {

//            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
//            dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            for (String unknownPtm : unknownPtms) {
                prideParametersReport += "<br>" + unknownPtm + " (unknown ptm) *"; // @TODO: have the user select them!!
            }

            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
            dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        }

        // set the modification profile
        prideSearchParameters.setModificationProfile(modProfile);

        prideParametersReport += "<br>";

        // set the enzyme
        prideParametersReport += "<br><b>Enzyme:</b> ";

        if (!enzymes.isEmpty()) {
            if (enzymes.size() == 1) {

                Enzyme mappedEnzyme = EnzymeFactory.getUtilitiesEnzyme(enzymes.get(0));

                // unknown enzyme
                if (mappedEnzyme == null) {

                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    // have the user select the enzyme
                    EnzymeSelectionDialog enzymeSelectionDialog = new EnzymeSelectionDialog(this, true, enzymes.get(0));

                    Enzyme selectedEnzyme = enzymeSelectionDialog.getEnzyme();
                    if (selectedEnzyme != null) {
                        mappedEnzyme = selectedEnzyme;
                        prideParametersReport += selectedEnzyme.getName();
                    } else {
                        prideParametersReport += enzymes.get(0) + " (unknown)";
                    }
                } else {
                    prideParametersReport += mappedEnzyme.getName();
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
                dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                // have the user select the enzyme
                EnzymeSelectionDialog enzymeSelectionDialog = new EnzymeSelectionDialog(this, true, enzymesAsText);
                Enzyme selectedEnzyme = enzymeSelectionDialog.getEnzyme();
                if (selectedEnzyme != null) {
                    prideParametersReport += selectedEnzyme.getName();
                    prideSearchParameters.setEnzyme(selectedEnzyme);
                } else {
                    prideSearchParameters.setEnzyme(null);
                    prideParametersReport += enzymesAsText + " (unknown)";
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
            prideParametersReport += "Trypsin (assumed)";
        }

        // set the ion types
        // @TODO: implement me!
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

        prideParametersReport += "<br><br><b>MGF File Location:</b> " + new File(outputFolder).getAbsolutePath();

        if (!unknownPtms.isEmpty()) {
            prideParametersReport += "<br><br>* Remember to add these PTMs manually in SearchGUI."; // @TODO: this warning should be stronger!!
        }

        prideParametersReport += "<br></html>";

        boolean debugOutput = false;

        // debug output
        if (debugOutput) {
            System.out.println("\nfragmentIonMassTolerance: " + fragmentIonMassTolerance);
            System.out.println("peptideIonMassTolerance: " + peptideIonMassTolerance);
            System.out.println("maxMissedCleavages: " + maxMissedCleavages);

            System.out.println("taxonomy: " + taxonomyForProject.get(prideAccession));
            System.out.println("species: " + speciesForProject.get(prideAccession));
            System.out.println("ptms: " + ptmsForProject.get(prideAccession));

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
        if (pridePtmName.equalsIgnoreCase("Carbamidomethyl")) {
            fixedPtm = true;
        }

        return fixedPtm;
    }

    /**
     * Filters the project table according to the current filter settings.
     */
    public void filter() {

        List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

        // accession filter
        String text = (String) searchTable.getValueAt(0, searchTable.getColumn("Accession").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Accession").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for accession!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // project title filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Title").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Title").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for title!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // species filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Species").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Species").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for species!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // tissue filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Tissue").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Tissue").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for tissue!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // ptm filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("PTMs").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("PTMs").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for PTMs!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // instruments filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Instrument").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Instrument").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for instruments!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // number of spectra filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("#Assays").getModelIndex());

        if (text != null && text.trim().length() > 0) {

            try {
                if (text.startsWith(">")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, projectsTable.getColumn("#Assays").getModelIndex()));
                } else if (text.startsWith("<")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, projectsTable.getColumn("#Assays").getModelIndex()));
                } else {
                    Integer value = new Integer(text);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, projectsTable.getColumn("#Assays").getModelIndex()));
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Support values for number of assays are:\nintegers, >integer or <integer.", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // submission type filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Type").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Type").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for submission type!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // date filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Date").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Date").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for date!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);

        if (projectsTable.getRowSorter() != null) {
            ((TableRowSorter) projectsTable.getRowSorter()).setRowFilter(allFilters);

            if (projectsTable.getRowCount() > 0) {
                projectsTable.setRowSelectionInterval(0, 0);
            }
        }

        updateAssayList();

        ((TitledBorder) projectsPanel.getBorder()).setTitle("PRIDE Projects (" + projectsTable.getRowCount() + ")");
        projectsPanel.repaint();
    }

    /**
     * Update the file list based on the selected project.
     */
    private void updateProjectFileList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();

        String projectAccession = null;
        int selectedRow = projectsTable.getSelectedRow();

        if (selectedRow != -1) {

            projectAccession = (String) projectsTable.getValueAt(selectedRow, 1);
            projectAccession = projectAccession.substring(projectAccession.lastIndexOf("\">") + 2, projectAccession.lastIndexOf("</font"));

            RestTemplate template = new RestTemplate();
            ResponseEntity<FileDetailList> fileDetailListResult = template.getForEntity(projectServiceURL + "file/list/project/" + projectAccession, FileDetailList.class);

            // @TODO: sort based on assay accession and then on type
            for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {

                ((DefaultTableModel) filesTable.getModel()).addRow(new Object[]{
                    (filesTable.getRowCount() + 1),
                    "<html><a href=\"" + DisplayFeaturesGenerator.getPrideAssayArchiveLink(fileDetail.getProjectAccession(), fileDetail.getAssayAccession())
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + fileDetail.getAssayAccession() + "</font></a><html>",
                    fileDetail.getFileType(),
                    "<html><a href=\"" + fileDetail.getDownloadLink().toExternalForm()
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + fileDetail.getFileName() + "</font></a><html>",
                    Util.roundDouble(((float) fileDetail.getFileSize()) / 1048576, 2), // @TODO: better formatting!!
                    false});
            }
        }

        if (projectAccession != null) {
            ((TitledBorder) filesPanel.getBorder()).setTitle("Files for " + projectAccession + " (" + filesTable.getRowCount() + ")");
        } else {
            ((TitledBorder) filesPanel.getBorder()).setTitle("Files (" + filesTable.getRowCount() + ")");
        }
        filesPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the file list based on the selected assay.
     */
    private void updateAssayFileList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();

        String assayAccession = null;
        int selectedRow = assaysTable.getSelectedRow();

        if (selectedRow != -1) {

            assayAccession = (String) assaysTable.getValueAt(selectedRow, 1);
            assayAccession = assayAccession.substring(assayAccession.lastIndexOf("\">") + 2, assayAccession.lastIndexOf("</font"));

            RestTemplate template = new RestTemplate();
            ResponseEntity<FileDetailList> fileDetailListResult = template.getForEntity(projectServiceURL + "file/list/assay/" + assayAccession, FileDetailList.class);

            // @TODO: sort based on assay accession and then on type
            for (FileDetail fileDetail : fileDetailListResult.getBody().getList()) {

                ((DefaultTableModel) filesTable.getModel()).addRow(new Object[]{
                    (filesTable.getRowCount() + 1),
                    "<html><a href=\"" + DisplayFeaturesGenerator.getPrideAssayArchiveLink(fileDetail.getProjectAccession(), fileDetail.getAssayAccession())
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + fileDetail.getAssayAccession() + "</font></a><html>",
                    fileDetail.getFileType(),
                    "<html><a href=\"" + fileDetail.getDownloadLink().toExternalForm()
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + fileDetail.getFileName() + "</font></a><html>",
                    Util.roundDouble(((float) fileDetail.getFileSize()) / 1048576, 2), // @TODO: better formatting!!
                    false});
            }
        }

        if (assayAccession != null) {
            ((TitledBorder) filesPanel.getBorder()).setTitle("Files for " + assayAccession + " (" + filesTable.getRowCount() + ")");
        } else {
            ((TitledBorder) filesPanel.getBorder()).setTitle("Files (" + filesTable.getRowCount() + ")");
        }
        filesPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the assay list for the selected project.
     */
    private void updateAssayList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel assaysTableModel = (DefaultTableModel) assaysTable.getModel();
        assaysTableModel.getDataVector().removeAllElements();
        assaysTableModel.fireTableDataChanged();

        String projectAccession = null;
        int selectedRow = projectsTable.getSelectedRow();

        if (selectedRow != -1) {

            projectAccession = (String) projectsTable.getValueAt(selectedRow, 1);
            projectAccession = projectAccession.substring(projectAccession.lastIndexOf("\">") + 2, projectAccession.lastIndexOf("</font"));

            RestTemplate template = new RestTemplate();
            ResponseEntity<AssayDetailList> assayDetailList = template.getForEntity(projectServiceURL + "assay/list/project/" + projectAccession, AssayDetailList.class);

            double maxNumProteins = 0, maxNumPeptides = 0, maxNumSpectra = 0;

            for (AssayDetail assayDetail : assayDetailList.getBody().getList()) {

                ((DefaultTableModel) assaysTable.getModel()).addRow(new Object[]{
                    (assaysTable.getRowCount() + 1),
                    "<html><a href=\"" + DisplayFeaturesGenerator.getPrideAssayArchiveLink(assayDetail.getProjectAccession(), assayDetail.getAssayAccession())
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + assayDetail.getAssayAccession() + "</font></a><html>",
                    assayDetail.getTitle(),
                    setToString(assayDetail.getSpecies()),
                    setToString(assayDetail.getSampleDetails()),
                    setToString(assayDetail.getPtmNames()),
                    setToString(assayDetail.getDiseases()),
                    assayDetail.getProteinCount(),
                    assayDetail.getPeptideCount(),
                    assayDetail.getTotalSpectrumCount(),
                    "<html><a href=\"" + "dummy link"
                    + "\"><font color=\"" + TableProperties.getNotSelectedRowHtmlTagFontColor() + "\">"
                    + "more..." + "</font></a><html>"});

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

        if (projectAccession != null) {
            ((TitledBorder) assaysPanel.getBorder()).setTitle("Assays for " + projectAccession + " (" + assaysTable.getRowCount() + ")");
        } else {
            ((TitledBorder) assaysPanel.getBorder()).setTitle("Assays (" + assaysTable.getRowCount() + ")");
        }
        assaysPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Convert the PRIDE XML file to mgf.
     */
    private boolean convertPrideXmlToMgf() {

        boolean conversionOk = true;

        try {
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            PrideXmlReader prideXmlReader = new PrideXmlReader(currentPrideXmlFile);
            FileWriter w = new FileWriter(currentMgfFile);
            BufferedWriter bw = new BufferedWriter(w);
            List<String> spectra = prideXmlReader.getSpectrumIds();
            int spectraCount = spectra.size();

            if (spectraCount == 0) {
                bw.close();
                w.close();
                progressDialog.setRunFinished();
                JOptionPane.showMessageDialog(this, "The project contains no spectra! Conversion canceled.", "No Spectra Found", JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "The project contains no valid spectra! Conversion canceled.", "No Spectra Found", JOptionPane.WARNING_MESSAGE);
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
     * Unzip the mzData file.
     *
     * @throws IOException
     */
    private void unzipProject() throws IOException {

        FileInputStream instream = new FileInputStream(currentZippedPrideXmlFile);
        GZIPInputStream ginstream = new GZIPInputStream(instream);
        FileOutputStream outstream = new FileOutputStream(currentPrideXmlFile);
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
}
