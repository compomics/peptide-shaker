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
import com.compomics.util.gui.searchsettings.EnzymeSelectionDialog;
import eu.isas.peptideshaker.gui.WelcomeDialog;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import org.apache.commons.io.FileUtils;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

/**
 * A simple GUI for getting the mgf and search parameters for a PRIDE dataset.
 *
 * @author Harald Barsnes
 */
public class PrideReshakeGui extends javax.swing.JDialog {

    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of PubMed IDs for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> pumMedIdsForProject;
    /**
     * The list of taxonomies for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> taxonomyForProject;
    /**
     * The list of species for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> speciesForProject;
    /**
     * The list of currently selected species.
     */
    private ArrayList<String> currentSpecies;
    /**
     * The list of PTMs for a given PRIDE project accession number.
     */
    private HashMap<Integer, String> ptmsForProject;
    /**
     * Total number of PRIDE projects.
     */
    private int totalNumberOfPrideProjects;
    /**
     * The project table column header tooltips.
     */
    private ArrayList<String> projectsTableToolTips;
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
     * Creates a new PrideReshakeGui dialog.
     *
     * @param peptideShakerGUI
     * @param welcomeDialog a reference to the welcome dialog
     * @param dummyParentFrame dummy parent frame to be able to show an icon in
     * the task bar, can be null
     * @param modal
     */
    public PrideReshakeGui(PeptideShakerGUI peptideShakerGUI, WelcomeDialog welcomeDialog, DummyFrame dummyParentFrame, boolean modal) {
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

        projectsTable.getColumn("Accession").setMaxWidth(80);
        projectsTable.getColumn("Accession").setMinWidth(80);
        projectsTable.getColumn(" ").setMaxWidth(30);
        projectsTable.getColumn(" ").setMinWidth(30);
        searchTable.getColumn("Accession").setMaxWidth(80);
        searchTable.getColumn("Accession").setMinWidth(80);
        searchTable.getColumn(" ").setMaxWidth(30);
        searchTable.getColumn(" ").setMinWidth(30);

        // make sure that the scroll panes are see-through
        projectsScrollPane.getViewport().setOpaque(false);
        searchScrollPane.getViewport().setOpaque(false);

        projectsTable.setAutoCreateRowSorter(true);

        projectsTable.getTableHeader().setReorderingAllowed(false);
        searchTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel projectsCorner = new JPanel();
        projectsCorner.setBackground(projectsTable.getTableHeader().getBackground());
        projectsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, projectsCorner);

        JPanel searchCorner = new JPanel();
        searchCorner.setBackground(searchTable.getTableHeader().getBackground());
        searchScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, searchCorner);

        projectsTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());

        projectsTable.getColumn("Title").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));

        projectsTableToolTips = new ArrayList<String>();
        projectsTableToolTips.add("Reanalyze PRIDE Project");
        projectsTableToolTips.add("PRIDE Accession Number");
        projectsTableToolTips.add("Project Title");
        projectsTableToolTips.add("Proejct Name");
        projectsTableToolTips.add("Species");
        projectsTableToolTips.add("Tissue Types");
        projectsTableToolTips.add("Post-Translational Modifications");
        projectsTableToolTips.add("Number of Spectra");
        projectsTableToolTips.add("Number of Peptides");
        projectsTableToolTips.add("Number of Proteins");
        projectsTableToolTips.add("References");
    }

    /**
     * Insert the PRIDE project data.
     */
    private void insertData() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        DefaultTableModel projectsTableModel = (DefaultTableModel) projectsTable.getModel();
        projectsTableModel.getDataVector().removeAllElements();
        projectsTableModel.fireTableDataChanged();

        pumMedIdsForProject = new HashMap<Integer, String>();
        taxonomyForProject = new HashMap<Integer, String>();
        speciesForProject = new HashMap<Integer, String>();
        ptmsForProject = new HashMap<Integer, String>();

        // get the local pride projects
        ArrayList<String> localPrideProjects = new ArrayList<String>();

        if (peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder() != null && new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder()).exists()) {
            for (File possiblePrideFile : new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder()).listFiles()) {
                if (possiblePrideFile.getName().lastIndexOf("PRIDE_Exp_Complete_Ac_") != -1
                        && possiblePrideFile.getAbsolutePath().endsWith(".xml")
                        && !possiblePrideFile.getAbsolutePath().endsWith(".t.xml")) {
                    localPrideProjects.add(possiblePrideFile.getAbsolutePath());
                }
            }
        }

        // add the local projects
        for (String localProject : localPrideProjects) {

            String accession = localProject.substring(localProject.indexOf("PRIDE_Exp_Complete_Ac_") + "PRIDE_Exp_Complete_Ac_".length(), localProject.lastIndexOf("."));
            String title = localProject.substring(localProject.indexOf("PRIDE_Exp_Complete_Ac_"), localProject.lastIndexOf("."));

            ((DefaultTableModel) projectsTable.getModel()).addRow(new Object[]{
                false,
                new Integer(accession),
                "<html><a href=\"" + peptideShakerGUI.getDisplayFeaturesGenerator().getPrideAccessionLink("" + accession)
                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                + title + "</font></a><html>",
                "",
                "",
                "",
                "",
                null,
                null,
                null,
                ""
            });
        }

        try {
            File databaseSummaryFile = new File(peptideShakerGUI.getJarFilePath(), "resources/conf/pride/database_summary.tsv");

            FileReader r = new FileReader(databaseSummaryFile);
            BufferedReader br = new BufferedReader(r);
            br.readLine(); // ignore the header
            String line = br.readLine();

            double maxNumSpectra = 0, maxNumPeptides = 0, maxNumProteins = 0;

            while (line != null) {

                String[] values = line.split("\\t");

                int columnCounter = 0;
                Integer accession = new Integer(values[columnCounter++]);
                String title = values[columnCounter++];
                String project = values[columnCounter++];
                String species = values[columnCounter++];
                String taxonomy = values[columnCounter++];
                String tissue = values[columnCounter++];
                String tissueId = values[columnCounter++];
                String ptms = values[columnCounter++];
                Integer numSpectra = new Integer(values[columnCounter++]);
                Integer numProteins = new Integer(values[columnCounter++]);
                Integer numPeptides = new Integer(values[columnCounter++]);
                String references = null;
                if (values.length > columnCounter) {
                    references = values[columnCounter++];
                }
                if (values.length > columnCounter) {
                    String pumMedId = values[columnCounter++];
                    pumMedIdsForProject.put(accession, pumMedId);
                }

                if (numSpectra > maxNumSpectra) {
                    maxNumSpectra = numSpectra;
                }
                if (numPeptides > maxNumPeptides) {
                    maxNumPeptides = numPeptides;
                }
                if (numProteins > maxNumProteins) {
                    maxNumProteins = numProteins;
                }

                taxonomyForProject.put(accession, taxonomy);
                speciesForProject.put(accession, species);
                ptmsForProject.put(accession, ptms);

                if (!hideNonValidProjectsCheckBox.isSelected() || (hideNonValidProjectsCheckBox.isSelected() && numSpectra > 0)) {

                    ((DefaultTableModel) projectsTable.getModel()).addRow(new Object[]{
                        false,
                        accession,
                        "<html><a href=\"" + peptideShakerGUI.getDisplayFeaturesGenerator().getPrideAccessionLink("" + accession)
                        + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                        + title + "</font></a><html>",
                        project,
                        species,
                        tissue,
                        ptms,
                        numSpectra,
                        numPeptides, // note that the order of peptides and proteins is different in the tsv file!
                        numProteins,
                        references
                    });
                }

                line = br.readLine();
            }

            br.close();
            r.close();

            totalNumberOfPrideProjects = projectsTable.getRowCount();

            ((TitledBorder) allProjectsPanel.getBorder()).setTitle("PRIDE Projects (" + projectsTable.getRowCount() + ")");
            allProjectsPanel.repaint();

            // update the sparklines with the max values
            projectsTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumSpectra, peptideShakerGUI.getSparklineColor()));
            projectsTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumPeptides, peptideShakerGUI.getSparklineColor()));
            projectsTable.getColumn("#Proteins").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNumProteins, peptideShakerGUI.getSparklineColor()));

            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Proteins").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Spectra").getCellRenderer()).setLogScale(true);
            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Peptides").getCellRenderer()).setLogScale(true);
            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Proteins").getCellRenderer()).setLogScale(true);

            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Spectra").getCellRenderer()).setMinimumChartValue(2.0);
            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Peptides").getCellRenderer()).setMinimumChartValue(2.0);
            ((JSparklinesBarChartTableCellRenderer) projectsTable.getColumn("#Proteins").getCellRenderer()).setMinimumChartValue(2.0);

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane("The PRIDE overview file was not found.<br>"
                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane("The PRIDE overview file error. "
                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
        allProjectsPanel = new javax.swing.JPanel();
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
        selectedProjectPanel = new javax.swing.JPanel();
        projectTitlePanel = new javax.swing.JPanel();
        accessionLabel = new javax.swing.JLabel();
        titleLabel = new javax.swing.JLabel();
        projectLabel = new javax.swing.JLabel();
        accessionTextField = new javax.swing.JTextField();
        titleScrollPane = new javax.swing.JScrollPane();
        titleTextArea = new javax.swing.JTextArea();
        projectScrollPane = new javax.swing.JScrollPane();
        projectTextArea = new javax.swing.JTextArea();
        numbersLabel = new javax.swing.JLabel();
        speciesTissueAndPtmsPanel = new javax.swing.JPanel();
        speciesLabel = new javax.swing.JLabel();
        speciesTextField = new javax.swing.JTextField();
        tissueLabel = new javax.swing.JLabel();
        tissueTextField = new javax.swing.JTextField();
        ptmsLabel = new javax.swing.JLabel();
        ptmsScrollPane = new javax.swing.JScrollPane();
        ptmsTextArea = new javax.swing.JTextArea();
        referencesPanel = new javax.swing.JPanel();
        referencesLabel = new javax.swing.JLabel();
        pumMedLabel = new javax.swing.JLabel();
        referencesScrollPane = new javax.swing.JScrollPane();
        referencesTextArea = new javax.swing.JTextArea();
        pumMedScrollPane = new javax.swing.JScrollPane();
        pubMedEditorPane = new javax.swing.JEditorPane();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PRIDE - Public Projects");
        setMinimumSize(new java.awt.Dimension(1280, 750));

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        allProjectsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PRIDE Projects"));
        allProjectsPanel.setOpaque(false);

        projectsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Title", "Project", "Species", "Tissue", "PTMs", "#Spectra", "#Peptides", "#Proteins", "References"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false, false, false, false
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
            public void mouseExited(java.awt.event.MouseEvent evt) {
                projectsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                projectsTableMouseReleased(evt);
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
        hideNonValidProjectsCheckBox.setText("Hide projects without spectra");
        hideNonValidProjectsCheckBox.setIconTextGap(10);
        hideNonValidProjectsCheckBox.setOpaque(false);
        hideNonValidProjectsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideNonValidProjectsCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout allProjectsPanelLayout = new javax.swing.GroupLayout(allProjectsPanel);
        allProjectsPanel.setLayout(allProjectsPanelLayout);
        allProjectsPanelLayout.setHorizontalGroup(
            allProjectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(allProjectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(allProjectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(allProjectsPanelLayout.createSequentialGroup()
                        .addComponent(projectsScrollPane)
                        .addContainerGap())
                    .addGroup(allProjectsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(projectNotFoundLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(hideNonValidProjectsCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(localProjectsFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))))
        );
        allProjectsPanelLayout.setVerticalGroup(
            allProjectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(allProjectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(allProjectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(localProjectsFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectNotFoundLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hideNonValidProjectsCheckBox))
                .addGap(4, 4, 4))
        );

        selectedProjectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Project"));
        selectedProjectPanel.setOpaque(false);

        projectTitlePanel.setOpaque(false);

        accessionLabel.setFont(accessionLabel.getFont().deriveFont(accessionLabel.getFont().getStyle() | java.awt.Font.BOLD));
        accessionLabel.setText("Accession");

        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | java.awt.Font.BOLD));
        titleLabel.setText("Title");

        projectLabel.setFont(projectLabel.getFont().deriveFont(projectLabel.getFont().getStyle() | java.awt.Font.BOLD));
        projectLabel.setText("Project");

        accessionTextField.setEditable(false);
        accessionTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        titleTextArea.setEditable(false);
        titleTextArea.setColumns(20);
        titleTextArea.setLineWrap(true);
        titleTextArea.setRows(2);
        titleTextArea.setWrapStyleWord(true);
        titleScrollPane.setViewportView(titleTextArea);

        projectTextArea.setEditable(false);
        projectTextArea.setColumns(20);
        projectTextArea.setLineWrap(true);
        projectTextArea.setRows(2);
        projectTextArea.setWrapStyleWord(true);
        projectScrollPane.setViewportView(projectTextArea);

        numbersLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        numbersLabel.setText("   ");
        numbersLabel.setToolTipText("#Spectra / #Peptides / #Proteins");

        javax.swing.GroupLayout projectTitlePanelLayout = new javax.swing.GroupLayout(projectTitlePanel);
        projectTitlePanel.setLayout(projectTitlePanelLayout);
        projectTitlePanelLayout.setHorizontalGroup(
            projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectTitlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(projectTitlePanelLayout.createSequentialGroup()
                        .addComponent(accessionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(accessionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(numbersLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(projectTitlePanelLayout.createSequentialGroup()
                        .addGroup(projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(projectLabel)
                            .addComponent(titleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(projectScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE)
                            .addComponent(titleScrollPane))))
                .addContainerGap())
        );

        projectTitlePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {accessionLabel, projectLabel, titleLabel});

        projectTitlePanelLayout.setVerticalGroup(
            projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectTitlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(accessionLabel)
                    .addComponent(accessionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numbersLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(titleScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(projectTitlePanelLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(titleLabel)))
                .addGroup(projectTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(projectTitlePanelLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(projectLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(projectTitlePanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(projectScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)))
                .addContainerGap())
        );

        speciesTissueAndPtmsPanel.setOpaque(false);

        speciesLabel.setFont(speciesLabel.getFont().deriveFont(speciesLabel.getFont().getStyle() | java.awt.Font.BOLD));
        speciesLabel.setText("Species");

        speciesTextField.setEditable(false);

        tissueLabel.setFont(tissueLabel.getFont().deriveFont(tissueLabel.getFont().getStyle() | java.awt.Font.BOLD));
        tissueLabel.setText("Tissue");

        tissueTextField.setEditable(false);

        ptmsLabel.setFont(ptmsLabel.getFont().deriveFont(ptmsLabel.getFont().getStyle() | java.awt.Font.BOLD));
        ptmsLabel.setText("PTMs");

        ptmsTextArea.setEditable(false);
        ptmsTextArea.setColumns(20);
        ptmsTextArea.setLineWrap(true);
        ptmsTextArea.setRows(2);
        ptmsTextArea.setWrapStyleWord(true);
        ptmsScrollPane.setViewportView(ptmsTextArea);

        javax.swing.GroupLayout speciesTissueAndPtmsPanelLayout = new javax.swing.GroupLayout(speciesTissueAndPtmsPanel);
        speciesTissueAndPtmsPanel.setLayout(speciesTissueAndPtmsPanelLayout);
        speciesTissueAndPtmsPanelLayout.setHorizontalGroup(
            speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(speciesTissueAndPtmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(speciesLabel)
                    .addComponent(tissueLabel)
                    .addComponent(ptmsLabel))
                .addGap(18, 18, 18)
                .addGroup(speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ptmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE)
                    .addComponent(tissueTextField)
                    .addComponent(speciesTextField))
                .addContainerGap())
        );
        speciesTissueAndPtmsPanelLayout.setVerticalGroup(
            speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(speciesTissueAndPtmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speciesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(speciesLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tissueLabel)
                    .addComponent(tissueTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(speciesTissueAndPtmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(speciesTissueAndPtmsPanelLayout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addComponent(ptmsLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(speciesTissueAndPtmsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ptmsScrollPane)))
                .addContainerGap())
        );

        referencesPanel.setOpaque(false);

        referencesLabel.setFont(referencesLabel.getFont().deriveFont(referencesLabel.getFont().getStyle() | java.awt.Font.BOLD));
        referencesLabel.setText("References");

        pumMedLabel.setFont(pumMedLabel.getFont().deriveFont(pumMedLabel.getFont().getStyle() | java.awt.Font.BOLD));
        pumMedLabel.setText("PubMed");

        referencesTextArea.setEditable(false);
        referencesTextArea.setColumns(20);
        referencesTextArea.setLineWrap(true);
        referencesTextArea.setRows(2);
        referencesTextArea.setWrapStyleWord(true);
        referencesScrollPane.setViewportView(referencesTextArea);

        pubMedEditorPane.setEditable(false);
        pubMedEditorPane.setContentType("text/html"); // NOI18N
        pubMedEditorPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                pubMedEditorPaneHyperlinkUpdate(evt);
            }
        });
        pumMedScrollPane.setViewportView(pubMedEditorPane);

        javax.swing.GroupLayout referencesPanelLayout = new javax.swing.GroupLayout(referencesPanel);
        referencesPanel.setLayout(referencesPanelLayout);
        referencesPanelLayout.setHorizontalGroup(
            referencesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(referencesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(referencesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(referencesLabel)
                    .addComponent(pumMedLabel))
                .addGap(18, 18, 18)
                .addGroup(referencesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(referencesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE)
                    .addComponent(pumMedScrollPane))
                .addContainerGap())
        );
        referencesPanelLayout.setVerticalGroup(
            referencesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(referencesPanelLayout.createSequentialGroup()
                .addGroup(referencesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(referencesPanelLayout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(referencesLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(referencesPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(referencesScrollPane)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(referencesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(referencesPanelLayout.createSequentialGroup()
                        .addComponent(pumMedScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, referencesPanelLayout.createSequentialGroup()
                        .addComponent(pumMedLabel)
                        .addGap(48, 48, 48))))
        );

        jSeparator1.setForeground(new java.awt.Color(210, 210, 210));
        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jSeparator2.setForeground(new java.awt.Color(210, 210, 210));
        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout selectedProjectPanelLayout = new javax.swing.GroupLayout(selectedProjectPanel);
        selectedProjectPanel.setLayout(selectedProjectPanelLayout);
        selectedProjectPanelLayout.setHorizontalGroup(
            selectedProjectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectedProjectPanelLayout.createSequentialGroup()
                .addComponent(projectTitlePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(speciesTissueAndPtmsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(referencesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        selectedProjectPanelLayout.setVerticalGroup(
            selectedProjectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectedProjectPanelLayout.createSequentialGroup()
                .addGroup(selectedProjectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, selectedProjectPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1))
                    .addGroup(selectedProjectPanelLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(jSeparator2)))
                .addContainerGap())
            .addGroup(selectedProjectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectedProjectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectTitlePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(referencesPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(speciesTissueAndPtmsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        searchPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search PRIDE *"));
        searchPanel.setOpaque(false);

        searchTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Boolean(false), null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                " ", "Accession", "Title", "Project", "Species", "Tissue", "PTMs", "#Spectra", "#Peptides", "#Proteins", "References"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
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

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(searchFiltersLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reshakeButton)
                        .addGap(14, 14, 14))
                    .addComponent(selectedProjectPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(allProjectsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(allProjectsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectedProjectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
        updateSelectedProjectInfo();

        if (evt != null) {
            peptideShakerGUI.resetSelectedItems();

            int row = projectsTable.getSelectedRow();
            int column = projectsTable.getSelectedColumn();

            // open pride project link in web browser
            if (column == projectsTable.getColumn("Title").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
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
     * Update the info about the selected project.
     *
     * @param evt
     */
    private void projectsTablKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectsTablKeyReleased
        updateSelectedProjectInfo();
    }//GEN-LAST:event_projectsTablKeyReleased

    /**
     * Makes the links active.
     *
     * @param evt
     */
    private void pubMedEditorPaneHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_pubMedEditorPaneHyperlinkUpdate
        if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ENTERED.toString())) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.EXITED.toString())) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ACTIVATED.toString())) {
            if (evt.getDescription().startsWith("#")) {
                pubMedEditorPane.scrollToReference(evt.getDescription());
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(evt.getDescription());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_pubMedEditorPaneHyperlinkUpdate

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

        if (row != -1 && column != -1 && column == projectsTable.getColumn("Title").getModelIndex() && projectsTable.getValueAt(row, column) != null) {

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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel accessionLabel;
    private javax.swing.JTextField accessionTextField;
    private javax.swing.JPanel allProjectsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JCheckBox hideNonValidProjectsCheckBox;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel localProjectsFolderLabel;
    private javax.swing.JLabel numbersLabel;
    private javax.swing.JLabel projectLabel;
    private javax.swing.JLabel projectNotFoundLabel;
    private javax.swing.JScrollPane projectScrollPane;
    private javax.swing.JTextArea projectTextArea;
    private javax.swing.JPanel projectTitlePanel;
    private javax.swing.JScrollPane projectsScrollPane;
    private javax.swing.JTable projectsTable;
    private javax.swing.JLabel ptmsLabel;
    private javax.swing.JScrollPane ptmsScrollPane;
    private javax.swing.JTextArea ptmsTextArea;
    private javax.swing.JEditorPane pubMedEditorPane;
    private javax.swing.JLabel pumMedLabel;
    private javax.swing.JScrollPane pumMedScrollPane;
    private javax.swing.JLabel referencesLabel;
    private javax.swing.JPanel referencesPanel;
    private javax.swing.JScrollPane referencesScrollPane;
    private javax.swing.JTextArea referencesTextArea;
    private javax.swing.JButton reshakeButton;
    private javax.swing.JLabel searchFiltersLabel;
    private javax.swing.JPanel searchPanel;
    private javax.swing.JScrollPane searchScrollPane;
    private javax.swing.JTable searchTable;
    private javax.swing.JPanel selectedProjectPanel;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JTextField speciesTextField;
    private javax.swing.JPanel speciesTissueAndPtmsPanel;
    private javax.swing.JLabel tissueLabel;
    private javax.swing.JTextField tissueTextField;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JScrollPane titleScrollPane;
    private javax.swing.JTextArea titleTextArea;
    // End of variables declaration//GEN-END:variables

    /**
     * Download and convert a PRIDE project to mgf.
     *
     * @param accession the accession numbers of the PRIDE projects
     */
    private void downloadPrideDatasets(ArrayList<Integer> aSelectedProjects) {

        final ArrayList<Integer> selectedProjects = aSelectedProjects;
        final PrideReshakeGui finalRef = this;
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
                            currentZippedPrideXmlFile = new File(outputFolder, "temp/PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml.gz");
                            currentPrideXmlFile = new File(outputFolder, "temp/PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml");
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

                                // download the pride xml file
                                FileUtils.copyURLToFile(currentPrideProjectUrl, currentZippedPrideXmlFile);

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
                                currentPrideXmlFile = new File(peptideShakerGUI.getUtilitiesUserPreferences().getLocalPrideFolder(),
                                        "PRIDE_Exp_Complete_Ac_" + prideAccession + ".xml");
                            }

                            if (progressDialog.isRunCanceled()) {
                                progressDialog.setRunFinished();
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

                    // clear the temp folder
                    progressDialog.setTitle("Clearing Temp Files. Please Wait...");
                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    Util.deleteDir(currentZippedPrideXmlFile.getParentFile());

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

                        PrideReshakeGui.this.setVisible(false);
                        
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

                Enzyme mappedEnzyme = EnzymeFactory.getInstance().getUtilitiesEnzyme(enzymes.get(0));

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

        // accession number filter
        if (searchTable.getValueAt(0, searchTable.getColumn("Accession").getModelIndex()) != null) {

            try {
                Integer value = (Integer) searchTable.getValueAt(0, searchTable.getColumn("Accession").getModelIndex());
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, projectsTable.getColumn("Accession").getModelIndex()));
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "Accession has to be an integer!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // project title filter
        String text = (String) searchTable.getValueAt(0, searchTable.getColumn("Title").getModelIndex());

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

        // project name filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("Project").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("Project").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for project!", "Filter Error", JOptionPane.ERROR_MESSAGE);
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

        // number of spectra filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("#Spectra").getModelIndex());

        if (text != null && text.trim().length() > 0) {

            try {
                if (text.startsWith(">")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, projectsTable.getColumn("#Spectra").getModelIndex()));
                } else if (text.startsWith("<")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, projectsTable.getColumn("#Spectra").getModelIndex()));
                } else {
                    Integer value = new Integer(text);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, projectsTable.getColumn("#Spectra").getModelIndex()));
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Support values for number of spectra are:\nintegers, >integer or <integer.", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // number of peptides filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("#Peptides").getModelIndex());

        if (text != null && text.trim().length() > 0) {

            try {
                if (text.startsWith(">")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, projectsTable.getColumn("#Peptides").getModelIndex()));
                } else if (text.startsWith("<")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, projectsTable.getColumn("#Peptides").getModelIndex()));
                } else {
                    Integer value = new Integer(text);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, projectsTable.getColumn("#Peptides").getModelIndex()));
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Support values for number of peptides are:\nintegers, >integer or <integer.", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // number of proteins filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("#Proteins").getModelIndex());

        if (text != null && text.trim().length() > 0) {

            try {
                if (text.startsWith(">")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, projectsTable.getColumn("#Proteins").getModelIndex()));
                } else if (text.startsWith("<")) {
                    Integer value = new Integer(text.substring(1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, projectsTable.getColumn("#Proteins").getModelIndex()));
                } else {
                    Integer value = new Integer(text);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, projectsTable.getColumn("#Proteins").getModelIndex()));
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Support values for number of proteins are:\nintegers, >integer or <integer.", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // references filter
        text = (String) searchTable.getValueAt(0, searchTable.getColumn("References").getModelIndex());

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, projectsTable.getColumn("References").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for references!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // selected for analysis filter
        boolean selected = (Boolean) searchTable.getValueAt(0, searchTable.getColumn(" ").getModelIndex());

        if (selected) {
            RowFilter<Object, Object> selectedFilter = new RowFilter<Object, Object>() {
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    return entry.getValue(projectsTable.getColumn(" ").getModelIndex()).equals(true);
                }
            };

            filters.add(selectedFilter);
        }

        RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);

        if (projectsTable.getRowSorter() != null) {
            ((TableRowSorter) projectsTable.getRowSorter()).setRowFilter(allFilters);

            if (projectsTable.getRowCount() > 0) {
                projectsTable.setRowSelectionInterval(0, 0);
            }
        }

        updateSelectedProjectInfo();

        ((TitledBorder) allProjectsPanel.getBorder()).setTitle("PRIDE Projects (" + projectsTable.getRowCount() + ")");
        allProjectsPanel.repaint();
    }

    /**
     * Update the info about the selected project.
     */
    private void updateSelectedProjectInfo() {

        // clear the old data
        ((TitledBorder) selectedProjectPanel.getBorder()).setTitle("Selected Project");
        numbersLabel.setText("  ");
        accessionTextField.setText(null);
        titleTextArea.setText(null);
        projectTextArea.setText(null);
        speciesTextField.setText(null);
        tissueTextField.setText(null);
        ptmsTextArea.setText(null);
        referencesTextArea.setText(null);
        pubMedEditorPane.setText(null);
        selectedProjectPanel.repaint();

        int selectedRow = projectsTable.getSelectedRow();

        // iterate the pride table and see if any projects are selected // @TODO: could perhaps be sped up, but there are not that many projects...
        boolean selected = false;

        for (int i = 0; i < projectsTable.getRowCount(); i++) {
            if ((Boolean) projectsTable.getValueAt(i, projectsTable.getColumn(" ").getModelIndex())) {
                selected = true;
                break;
            }
        }

        // enable/disable the reshake button
        reshakeButton.setEnabled(selected);

        // update the information about the selected project
        if (selectedRow != -1) {

            // remove the html for the title
            String title = (String) projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Title").getModelIndex());
            title = title.substring(title.lastIndexOf("\">") + 2, title.lastIndexOf("</font"));

            ((TitledBorder) selectedProjectPanel.getBorder()).setTitle("Selected Project ("
                    + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()) + " - "
                    + title + ")");
            selectedProjectPanel.repaint();

            String spectraCount = "-";
            String peptideCount = "-";
            String proteinCount = "-";

            if ((Integer) projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Spectra").getModelIndex()) != null) {
                spectraCount = "" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Spectra").getModelIndex());
            }
            if ((Integer) projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Peptides").getModelIndex()) != null) {
                peptideCount = "" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Peptides").getModelIndex());
            }
            if ((Integer) projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Proteins").getModelIndex()) != null) {
                proteinCount = "" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Proteins").getModelIndex());
            }

            // display the info about the selected project
            numbersLabel.setText(spectraCount + " / " + peptideCount + " / " + proteinCount);
            accessionTextField.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()));
            titleTextArea.setText(title);
            titleTextArea.setCaretPosition(0);
            projectTextArea.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Project").getModelIndex()));
            projectTextArea.setCaretPosition(0);
            speciesTextField.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Species").getModelIndex()));
            tissueTextField.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Tissue").getModelIndex()));
            ptmsTextArea.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("PTMs").getModelIndex()));
            ptmsTextArea.setCaretPosition(0);
            if (projectsTable.getValueAt(selectedRow, projectsTable.getColumn("References").getModelIndex()) != null) {
                referencesTextArea.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("References").getModelIndex()));
                referencesTextArea.setCaretPosition(0);
            }
            if (pumMedIdsForProject.get((Integer) projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex())) != null) {
                String pubMedIds = pumMedIdsForProject.get((Integer) projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()));
                String[] values = pubMedIds.split(",");

                String pumMedIdsWithLinks = "<html>";
                for (String value : values) {
                    pumMedIdsWithLinks += "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/" + value + "\">" + value + "</a><br> ";
                }

                // remove the last ", "
                //pumMedIdsWithLinks = pumMedIdsWithLinks.substring(0, pumMedIdsWithLinks.length() - 2);
                pumMedIdsWithLinks += "</html>";

                pubMedEditorPane.setText(pumMedIdsWithLinks);
                pubMedEditorPane.setCaretPosition(0);
            }
        }
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
