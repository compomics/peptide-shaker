package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import java.awt.Toolkit;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import org.jfree.chart.plot.PlotOrientation;
import no.uib.jsparklines.renderers.*;
import org.apache.commons.io.FileUtils;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;
import uk.ac.ebi.pride.tools.jmzreader.model.Spectrum;
import uk.ac.ebi.pride.tools.mzdata_parser.MzDataFile;

/**
 * A simple GUI for downloading the mgf and search params for a PRIDE dataset.
 *
 * @author Harald Barsnes
 */
public class PrideReshakeGui extends javax.swing.JDialog {

    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of PubMed IDs for a give PRIDE project accession number.
     */
    private HashMap<Integer, String> pumMedIdsForProject;
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
     * The current zipped mzData file.
     */
    private File currentZippedMzDataFile;
    /**
     * The current mzData file.
     */
    private File currentMzDataFile;
    /**
     * The current mgf file.
     */
    private File currentMgfFile;
    /**
     * The current URL contect length.
     */
    private int currentUrlContentLength;
    /**
     * True of a file is currently being downloaded.
     */
    private boolean isFileBeingDownloaded = false;

    /**
     * Creates a new PrideReshakeGui dialog.
     *
     * @param peptideShakerGUI
     * @param modal
     */
    public PrideReshakeGui(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        setUpGui();
        insertData();

        this.setSize(peptideShakerGUI.getWidth() - 200, peptideShakerGUI.getHeight() - 200);

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        projectsTable.getColumn("Accession").setMaxWidth(80);
        projectsTable.getColumn("Accession").setMinWidth(80);
        projectsTable.getColumn("Reshake").setMaxWidth(70);
        projectsTable.getColumn("Reshake").setMinWidth(70);
        searchTable.getColumn("Accession").setMaxWidth(80);
        searchTable.getColumn("Accession").setMinWidth(80);
        searchTable.getColumn("Reshake").setMaxWidth(70);
        searchTable.getColumn("Reshake").setMinWidth(70);

        // make sure that the scroll panes are see-through
        projectsScrollPane.getViewport().setOpaque(false);
        searchScrollPane.getViewport().setOpaque(false);

        projectsTable.setAutoCreateRowSorter(true);
        searchTable.setAutoCreateRowSorter(true);

        projectsTable.getTableHeader().setReorderingAllowed(false);
        searchTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel projectsCorner = new JPanel();
        projectsCorner.setBackground(projectsTable.getTableHeader().getBackground());
        projectsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, projectsCorner);

        JPanel searchCorner = new JPanel();
        searchCorner.setBackground(searchTable.getTableHeader().getBackground());
        searchScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, searchCorner);

        projectsTable.getColumn("Reshake").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));

        projectsTableToolTips = new ArrayList<String>();
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
        projectsTableToolTips.add("Reanalyze PRIDE Project");
    }

    /**
     * Insert the PRIDE project data.
     */
    private void insertData() {

        pumMedIdsForProject = new HashMap<Integer, String>();

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
                String pumMedId = null;
                if (values.length > columnCounter) {
                    pumMedId = values[columnCounter++];
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

                ((DefaultTableModel) projectsTable.getModel()).addRow(new Object[]{
                            accession,
                            title,
                            project,
                            species,
                            tissue,
                            ptms,
                            numSpectra,
                            numPeptides, // note that the order of peptides and proteins is different in the tsv file!
                            numProteins,
                            references,
                            "<html><a href=\"dummy\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">Reshake</font></a>"
                        });

                line = br.readLine();
            }

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

        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrideReshakeGui.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrideReshakeGui.class.getName()).log(Level.SEVERE, null, ex);
        }
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PRIDE - Public Projects");

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        allProjectsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PRIDE Projects"));
        allProjectsPanel.setOpaque(false);

        projectsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Accession", "Title", "Project", "Species", "Tissue", "PTMs", "#Spectra", "#Peptides", "#Proteins", "References", "Reshake"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class
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
                projectsTableKeyReleased(evt);
            }
        });
        projectsScrollPane.setViewportView(projectsTable);

        javax.swing.GroupLayout allProjectsPanelLayout = new javax.swing.GroupLayout(allProjectsPanel);
        allProjectsPanel.setLayout(allProjectsPanelLayout);
        allProjectsPanelLayout.setHorizontalGroup(
            allProjectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, allProjectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane)
                .addContainerGap())
        );
        allProjectsPanelLayout.setVerticalGroup(
            allProjectsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(allProjectsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                .addContainerGap())
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

        titleTextArea.setColumns(20);
        titleTextArea.setEditable(false);
        titleTextArea.setLineWrap(true);
        titleTextArea.setRows(2);
        titleTextArea.setWrapStyleWord(true);
        titleScrollPane.setViewportView(titleTextArea);

        projectTextArea.setColumns(20);
        projectTextArea.setEditable(false);
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
                            .addComponent(projectScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
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
                        .addComponent(projectScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)))
                .addContainerGap(0, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        ptmsTextArea.setColumns(20);
        ptmsTextArea.setEditable(false);
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
                    .addComponent(ptmsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
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
        pumMedLabel.setText("PumMed");

        referencesTextArea.setColumns(20);
        referencesTextArea.setEditable(false);
        referencesTextArea.setLineWrap(true);
        referencesTextArea.setRows(2);
        referencesTextArea.setWrapStyleWord(true);
        referencesScrollPane.setViewportView(referencesTextArea);

        pubMedEditorPane.setContentType("text/html");
        pubMedEditorPane.setEditable(false);
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
                    .addComponent(referencesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
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

        searchPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search PRIDE"));
        searchPanel.setOpaque(false);

        searchTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, ""}
            },
            new String [] {
                "Accession", "Title", "Project", "Species", "Tissue", "PTMs", "#Spectra", "#Peptides", "#Proteins", "References", "Reshake"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
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

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectedProjectPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(allProjectsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        }

        final int selectedRow = projectsTable.getSelectedRow();
        int column = projectsTable.getSelectedColumn();

        if (evt == null || (evt.getButton() == MouseEvent.BUTTON1)) {


            // download mzData file
            if (column == projectsTable.getColumn("Reshake").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) projectsTable.getValueAt(selectedRow, column)).lastIndexOf("<html>") != -1) {

                try {
                    currentPrideProjectUrl = new URL("ftp://ftp.ebi.ac.uk/pub/databases/pride/PRIDE_Exp_mzData_Ac_"
                            + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()) + ".xml.gz");
                    currentZippedMzDataFile = new File(peptideShakerGUI.getJarFilePath(), "resources/conf/pride/temp/PRIDE_Exp_mzData_Ac_"
                            + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()) + ".xml.gz");
                    currentMzDataFile = new File(peptideShakerGUI.getJarFilePath(), "resources/conf/pride/temp/PRIDE_Exp_mzData_Ac_"
                            + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()) + ".xml");
                    currentMgfFile = new File(peptideShakerGUI.getJarFilePath(), "resources/conf/pride/temp/PRIDE_Exp_mzData_Ac_"
                            + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()) + ".mgf");
                    URLConnection conn = currentPrideProjectUrl.openConnection();
                    currentUrlContentLength = conn.getContentLength();
                    // currentUrlContentLength = conn.getContentLengthLong(): // @TODO: requires Java 7...

                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                    currentPrideProjectUrl = null;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    currentPrideProjectUrl = null;
                }

                if (currentPrideProjectUrl != null) {

                    progressDialog = new ProgressDialogX(peptideShakerGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);
                    
                    if (currentUrlContentLength != -1) {
                        progressDialog.setIndeterminate(false);
                        progressDialog.setValue(0);
                        progressDialog.setMaxProgressValue(currentUrlContentLength);
                    } else {
                        progressDialog.setIndeterminate(true);
                    }

                    progressDialog.setTitle("Downloading PRIDE Project. Please Wait...");
                    progressDialog.setUnstoppable(true); // @TODO: not sure if this process can be stopped at all...
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

                            // download the mzData file
                            try {
                                FileUtils.copyURLToFile(currentPrideProjectUrl, currentZippedMzDataFile);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }

                            isFileBeingDownloaded = false;

                            // file downloaded, unzip file
                            progressDialog.setTitle("Unzipping Project. Please Wait...");
                            progressDialog.setIndeterminate(true);
                            unzipProject();

                            // file unzipped, time to start the conversion to mgf
                            convertMzDataToMgf();

                            progressDialog.setRunFinished();
                        }
                    }.start();

                    new Thread("DownloadMonitorThread") {

                        @Override
                        public void run() {

                            long start = System.currentTimeMillis();

                            while (isFileBeingDownloaded) {
                                long now = System.currentTimeMillis();

                                // update the progress dialog every 100 millisecond or so
                                if ((now - start) > 100 && progressDialog != null) {
                                    long length = currentZippedMzDataFile.length();

                                    if (currentUrlContentLength != -1) {
                                        progressDialog.setValue(new Long(length).intValue());
                                    }

                                    progressDialog.setTitle("Downloading PRIDE Project. Please Wait... (" + (length / (1024L * 1024L)) + " MB)");

                                    start = System.currentTimeMillis();
                                }
                            }
                        }
                    }.start();
                }
            }
        }
    }//GEN-LAST:event_projectsTableMouseReleased

    /**
     * Update the info about the selected project.
     *
     * @param evt
     */
    private void projectsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectsTableKeyReleased
        updateSelectedProjectInfo();
    }//GEN-LAST:event_projectsTableKeyReleased

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

        if (row != -1 && column != -1 && column == projectsTable.getColumn("Reshake").getModelIndex() && projectsTable.getValueAt(row, column) != null) {

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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel accessionLabel;
    private javax.swing.JTextField accessionTextField;
    private javax.swing.JPanel allProjectsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel numbersLabel;
    private javax.swing.JLabel projectLabel;
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

        // clear the old datra
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

        if (selectedRow != -1) {

            ((TitledBorder) selectedProjectPanel.getBorder()).setTitle("Selected Project ("
                    + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()) + " - "
                    + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Title").getModelIndex()) + ")");
            selectedProjectPanel.repaint();

            // display the info about the selected project
            numbersLabel.setText(projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Spectra").getModelIndex())
                    + " / " + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Peptides").getModelIndex())
                    + " / " + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("#Proteins").getModelIndex()));
            accessionTextField.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Accession").getModelIndex()));
            titleTextArea.setText("" + projectsTable.getValueAt(selectedRow, projectsTable.getColumn("Title").getModelIndex()));
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

                for (int i = 0; i < values.length; i++) {
                    pumMedIdsWithLinks += "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/" + values[i] + "\">" + values[i] + "</a><br> ";
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
     * Convert the mzData file to mgf.
     */
    private void convertMzDataToMgf() {
        try {
            MzDataFile inputParser = new MzDataFile(currentMzDataFile);
            FileWriter w = new FileWriter(currentMgfFile);
            BufferedWriter bw = new BufferedWriter(w);
            int spectraCount = inputParser.getSpectraCount();

            inputParser.getDescription();

            progressDialog.setIndeterminate(false);
            progressDialog.setTitle("Converting Spectra. Please Wait...");
            progressDialog.setMaxProgressValue(spectraCount);
            progressDialog.setValue(0);

            for (int i = 1; i <= spectraCount; i++) {
                Spectrum spectrum = inputParser.getSpectrumByIndex(i);
                String spectrumAsMgf = asMgf(spectrum);
                bw.write(spectrumAsMgf);
                progressDialog.increaseProgressValue();
            }

            bw.close();
            w.close();

        } catch (JMzReaderException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the mzData values as an mgf bloc.
     *
     * @param spectrum
     * @return the mzData spectrum as an mgf bloc
     */
    public String asMgf(Spectrum spectrum) {

        String result = "BEGIN IONS" + System.getProperty("line.separator");
        result += "TITLE=" + spectrum.getId() + System.getProperty("line.separator");

        // @TODO: what to do if precursor details are missing???
        
        // add precursor details
        result += "PEPMASS=" + spectrum.getPrecursorMZ(); // @TODO: verify that this is the correct value!!!

        // get the spectrum's precursor intensity
        Double precursorIntensity = spectrum.getPrecursorIntensity();
        if (precursorIntensity != null) {
            result += "\t" + precursorIntensity;
        }

        result += System.getProperty("line.separator");

        //result += "RTINSECONDS="; // @TODO: get from spectrum.getAdditional();
//        if (precursor.hasRTWindow()) {
//            result += "RTINSECONDS=" + precursor.getRtWindow()[0] + "-" + precursor.getRtWindow()[1] + System.getProperty("line.separator");
//        } else if (precursor.getRt() != -1) {
//            result += "RTINSECONDS=" + precursor.getRt() + System.getProperty("line.separator");
//        }

        result += "CHARGE=" + spectrum.getPrecursorCharge() + System.getProperty("line.separator");

        // retrieve the spectrum's peaklist
        Map<Double, Double> peakList = spectrum.getPeakList();

        // process all peaks by iterating over the m/z values
        for (Double mz : peakList.keySet()) {
            Double intensity = peakList.get(mz);
            result += mz + " " + intensity + System.getProperty("line.separator");
        }

        result += "END IONS" + System.getProperty("line.separator") + System.getProperty("line.separator");

        return result;
    }

    /**
     * Unzip the mzData file.
     */
    private void unzipProject() {

        try {
            FileInputStream instream = new FileInputStream(currentZippedMzDataFile);
            GZIPInputStream ginstream = new GZIPInputStream(instream);
            FileOutputStream outstream = new FileOutputStream(currentMzDataFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = ginstream.read(buf)) > 0) {
                outstream.write(buf, 0, len);
            }
            ginstream.close();
            outstream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
