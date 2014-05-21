package eu.isas.peptideshaker.gui.pride;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.searchsettings.EnzymeSelectionDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.FTPDownloader;
import com.compomics.util.preferences.ModificationProfile;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.WelcomeDialog;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.springframework.http.ResponseEntity;
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
 * reshaking. (Work in progress...)
 *
 * @author Harald Barsnes
 */
public class PrideReShakeGUIv2 extends javax.swing.JFrame {

    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
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
     * The web service URL.
     */
    private static final String projectServiceURL = "http://wwwdev.ebi.ac.uk/pride/ws/archive/";

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

        // @TODO: ask for public or private data...
        insertData();
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

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
        filesTableToolTips.add("File Name and Link");
        filesTableToolTips.add("File Size");
        filesTableToolTips.add("ReShake");

        ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects");
        ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays");
        ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files");
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
        setTitle("PRIDE ReShake");

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

        javax.swing.GroupLayout projectsPanelLayout = new javax.swing.GroupLayout(projectsPanel);
        projectsPanel.setLayout(projectsPanelLayout);
        projectsPanelLayout.setHorizontalGroup(
            projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane)
                .addContainerGap())
        );
        projectsPanelLayout.setVerticalGroup(
            projectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
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
                .addComponent(assayTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                .addContainerGap())
        );

        reshakeButton.setBackground(new java.awt.Color(0, 153, 0));
        reshakeButton.setFont(reshakeButton.getFont().deriveFont(reshakeButton.getFont().getStyle() | java.awt.Font.BOLD));
        reshakeButton.setForeground(new java.awt.Color(255, 255, 255));
        reshakeButton.setText("ReShake PRIDE Projects");
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
                .addComponent(filesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
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
                        .addGap(71, 71, 71)
                        .addComponent(peptideShakerHomePageLabel)
                        .addGap(342, 342, 342)
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
                    .addComponent(reshakeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(aboutButton)
                        .addComponent(peptideShakerHomePageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
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

        File selectedFolder = Util.getUserSelectedFolder(this, "Select Output Folder", peptideShakerGUI.getLastSelectedFolder(), "Output Folder", "Select", false);

        if (selectedFolder != null) {
            peptideShakerGUI.setLastSelectedFolder(selectedFolder.getAbsolutePath());
            outputFolder = selectedFolder.getAbsolutePath();
            ArrayList<String> selectedFiles = new ArrayList<String>();
            currentSpecies = new ArrayList<String>();

            for (int i = 0; i < filesTable.getRowCount(); i++) {
                if ((Boolean) filesTable.getValueAt(i, filesTable.getColumn("  ").getModelIndex())) {

                    String link = (String) filesTable.getValueAt(i, filesTable.getColumn("File").getModelIndex());
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    selectedFiles.add(link);
                    System.out.println("link: " + link);
                }
            }

            boolean download = true;

            // check if multiple projects are selected
            if (selectedFiles.size() > 1) {
//                int value = JOptionPane.showConfirmDialog(this,
//                        "Note that if multiple projects are selected the search\n"
//                        + "parameters from the first project in the list is used.",
//                        "Search Parameters", JOptionPane.OK_CANCEL_OPTION);
//
//                if (value == JOptionPane.CANCEL_OPTION) {
//                    download = false;
//                }
            }

            if (download) {
                downloadPrideDatasets(selectedFiles); // @TODO: reimplement me!!!
                //JOptionPane.showMessageDialog(null, "Not yet reimplemented...");
            }
        }
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

        enableReshake();
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

    private void aboutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseEntered

    private void aboutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseExited

    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    private void peptideShakerHomePageLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseClicked

    private void peptideShakerHomePageLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseEntered

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
     * Update the file list based on the selected project.
     */
    private void updateProjectFileList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();
        reshakeButton.setEnabled(false);

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
            ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files for " + projectAccession + " (" + filesTable.getRowCount() + ")");
        } else {
            ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files (" + filesTable.getRowCount() + ")");
        }
        filesPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the file list based on the selected assay.
     */
    private void updateAssayFileList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel filesTableModel = (DefaultTableModel) filesTable.getModel();
        filesTableModel.getDataVector().removeAllElements();
        filesTableModel.fireTableDataChanged();
        reshakeButton.setEnabled(false);

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
            ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files for " + assayAccession + " (" + filesTable.getRowCount() + ")");
        } else {
            ((TitledBorder) filesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Files (" + filesTable.getRowCount() + ")");
        }
        filesPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the assay list for the selected project.
     */
    private void updateAssayList() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
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
            ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays for " + projectAccession + " (" + assaysTable.getRowCount() + ")");
        } else {
            ((TitledBorder) assaysPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Assays (" + assaysTable.getRowCount() + ")");
        }
        assaysPanel.repaint();

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Insert the PRIDE project data.
     */
    private void insertData() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        DefaultTableModel projectsTableModel = (DefaultTableModel) projectsTable.getModel();
        projectsTableModel.getDataVector().removeAllElements();
        projectsTableModel.fireTableDataChanged();

        RestTemplate template = new RestTemplate();

        // get the project count
        ResponseEntity<Integer> projectCountResult = template.getForEntity(projectServiceURL + "project/count", Integer.class); // @TODO: catch connection issues
        Integer projectCount = projectCountResult.getBody();

        // get the list of projects
        ResponseEntity<ProjectDetailList> projectList = template.getForEntity(projectServiceURL + "project/list?show=" + projectCount + "&page=1&sort=publication_date&order=desc", ProjectDetailList.class);

        double maxNumAssays = 0;
        speciesAll = new ArrayList<String>();
        instrumentsAll = new ArrayList<String>();
        ptmsAll = new ArrayList<String>();
        tissuesAll = new ArrayList<String>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

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

        ((TitledBorder) projectsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "PRIDE Projects (" + projectsTable.getRowCount() + ")");
        projectsPanel.repaint();

        // update the sparklines with the max values
        projectsTable.getColumn("#Assays").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumAssays, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setLogScale(true);
        ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Assays").getCellRenderer()).setMinimumChartValue(2.0);

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
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
     * Download and convert a PRIDE project to mgf.
     *
     * @param accession the accession numbers of the PRIDE projects
     */
    private void downloadPrideDatasets(ArrayList<String> aSelectedFiles) {

        final ArrayList<String> selectedFiles = aSelectedFiles;
        final PrideReShakeGUIv2 finalRef = this;
        maxPrecursorCharge = null;
        minPrecursorCharge = null;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);

        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        progressDialog.setTitle("Downloading Files. Please Wait...");
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

                    for (int i = 0; i < selectedFiles.size() && mgfConversionOk; i++) {
                        
                        String currentFile = selectedFiles.get(i);
                        String currentFileName = currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".gz"));

                        if (progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            return;
                        }

                        //final Integer prideAccession = selectedFiles.get(i);
                        final int counter = i;

                        if (selectedFiles.size() > 1) {
                            progressDialog.setTitle("Downloading Files (" + (i + 1) + "/" + selectedFiles.size() + "). Please Wait...");
                        } else {
                            progressDialog.setTitle("Downloading Files. Please Wait...");
                        }

                        try {
                            currentPrideProjectUrl = new URL(currentFile);
                            currentZippedPrideXmlFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/")));
                            currentPrideXmlFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".gz")));
                            currentMgfFile = new File(outputFolder, currentFile.substring(currentFile.lastIndexOf("/"), currentFile.lastIndexOf(".xml.gz")) + ".mgf");
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
                            finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
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
                                            finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                            return;
                                        }

                                        long now = System.currentTimeMillis();

                                        // update the progress dialog every 100 millisecond or so
                                        if ((now - start) > 100 && progressDialog != null) {
                                            long length = currentZippedPrideXmlFile.length();

                                            if (currentUrlContentLength != -1) {
                                                progressDialog.setValue((int) length);
                                            }

                                            if (selectedFiles.size() > 1) {
                                                progressDialog.setTitle("Downloading PRIDE Project (" + (counter + 1) + "/" + selectedFiles.size()
                                                        + "). Please Wait... (" + (length / (1024L * 1024L)) + " MB)");
                                            } else {
                                                progressDialog.setTitle("Downloading PRIDE Project. Please Wait... (" + (length / (1024L * 1024L)) + " MB)");
                                            }

                                            start = System.currentTimeMillis();
                                        }
                                    }
                                }
                            }.start();

                            if (!new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(), currentFileName).exists()) {

                                boolean downloadFile = true;

                                if (currentPrideXmlFile.exists()) {
                                    int option = JOptionPane.showConfirmDialog(PrideReShakeGUIv2.this,
                                            "The PRIDE file \'" + currentPrideXmlFile.getName() + "\' already exists locally.\nUse local copy?",
                                            "Use Local File?", JOptionPane.YES_NO_OPTION);

                                    downloadFile = (option == JOptionPane.NO_OPTION);
                                }

                                if (downloadFile) {

                                    // download the pride xml file
                                    FTPDownloader ftpDownloader = new FTPDownloader("ftp.pride.ebi.ac.uk", false);
                                    ftpDownloader.downloadFile(currentPrideProjectUrl.getPath(), currentZippedPrideXmlFile);
                                    ftpDownloader.disconnect();

                                    isFileBeingDownloaded = false;

                                    // file downloaded, unzip file
                                    if (selectedFiles.size() > 1) {
                                        progressDialog.setTitle("Unzipping PRIDE Project (" + (i + 1) + "/" + selectedFiles.size() + "). Please Wait...");
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
                                currentPrideXmlFile = new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(), currentFileName);
                            }

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
                                finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                return;
                            }

                            // file unzipped, time to start the conversion to mgf
                            if (selectedFiles.size() > 1) {
                                progressDialog.setTitle("Converting PRIDE Project (" + (i + 1) + "/" + selectedFiles.size() + "). Please Wait...");
                            } else {
                                progressDialog.setTitle("Converting PRIDE Project. Please Wait...");
                            }

                            mgfConversionOk = convertPrideXmlToMgf();

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
                                finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                return;
                            }

                            if (mgfConversionOk) {
                                // get the search params from the pride xml file
                                if (i == 0) {
                                    progressDialog.setTitle("Getting Search Settings. Please Wait...");
                                    prideSearchParametersReport = getSearchParams(prideSearchParameters); // @TODO: reimplement me!!!
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
                    finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

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

                        PrideReShakeGUIv2.this.setVisible(false);

                        // display the detected search parameters to the user
                        new PrideSearchParametersDialog(peptideShakerGUI,
                                new File(outputFolder, "pride.parameters"), prideSearchParametersReport, mgfFiles, selectedSpecies, selectedSpeciesType, true);
                    }

                } catch (Exception e) {
                    finalRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(finalRef,
                            "An error occured when trying to convert the PRIDE project: \n"
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

        PrideXmlReader prideXmlReader = new PrideXmlReader(currentPrideXmlFile);

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
        String species = (String) projectsTable.getValueAt(projectsTable.getSelectedRow(), projectsTable.getColumn("Species").getModelIndex());
        if (species == null || species.length() == 0) {
            prideParametersReport += "unknown";
            currentSpecies.add(null);
        } else {
            prideParametersReport += species;
            currentSpecies.add(species);
        }

        // help the user get the correct database
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        new DatabaseHelpDialog(peptideShakerGUI, prideSearchParameters, true, species);
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
        //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        // map the ptms to utilities ptms
        String allPtms = (String) projectsTable.getValueAt(projectsTable.getSelectedRow(), projectsTable.getColumn("PTMs").getModelIndex());
        ArrayList<String> unknownPtms = new ArrayList<String>();

        ModificationProfile modProfile = new ModificationProfile();

        prideParametersReport += "<br><br><b>Post-Translational Modifications:</b>";

        if (allPtms != null) {

            if (allPtms.trim().length() > 0) {

                String[] tempPtms = allPtms.split("; ");

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
            //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
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
                    //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

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
                //dummyParentFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

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
        if (pridePtmName.equalsIgnoreCase("Carbamidomethyl")) {
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
     * Enable or disable the ReShake button.
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JScrollPane assayTableScrollPane;
    private javax.swing.JPanel assaysPanel;
    private javax.swing.JTable assaysTable;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JPanel filesPanel;
    private javax.swing.JTable filesTable;
    private javax.swing.JScrollPane filesTableScrollPane;
    private javax.swing.JMenuItem findMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel peptideShakerHomePageLabel;
    private javax.swing.JPanel projectsPanel;
    private javax.swing.JScrollPane projectsScrollPane;
    private javax.swing.JTable projectsTable;
    private javax.swing.JButton reshakeButton;
    // End of variables declaration//GEN-END:variables
}
