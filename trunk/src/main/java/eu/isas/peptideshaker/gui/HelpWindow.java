package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.SwingUtilities;

/**
 * A window used to display help text in HTML format.
 * 
 * @author Harald Barsnes
 */
public class HelpWindow extends javax.swing.JFrame {

   /**
     * The default width of the frame.
     */
    private int windowWidth = 500;
    /**
     * The default height reduction relative to the parent frame.
     */
    private int windowHeightReduction = 300;
    
    /**
     * Creates a new HelpWindow with a Frame as a parent.
     *
     * @param parent
     * @param fileName the name of the help file
     */
    public HelpWindow(java.awt.Frame parent, URL fileName) {
        this(parent, fileName, null);
    }

    /**
     * Creates a new HelpWindow with a Frame as a parent.
     *
     * @param parent
     * @param fileName the name of the help file
     * @param reference a reference in the html file to scroll to, can be null
     */
    public HelpWindow(java.awt.Frame parent, URL fileName, String reference) {

        this.setAlwaysOnTop(true);
        this.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        initComponents();

        try {
            InputStream stream = fileName.openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            String s = b.readLine();
            String helpText = "";

            while (s != null) {
                helpText += s;
                s = b.readLine();
            }
            
            String imageTag = "<img src=\"";
            int index = 0;
            
            // replace any relative image tags by their static alternatives
            while (helpText.indexOf(imageTag, index) != -1) {     
                int startIndex = helpText.indexOf(imageTag, index) + imageTag.length();
                String figureName = helpText.substring(startIndex, helpText.indexOf(".", startIndex));
                String figureType = helpText.substring(helpText.indexOf(".", startIndex) + 1, helpText.indexOf("\"", startIndex));   
                helpText = helpText.replaceAll(imageTag + figureName + "." + figureType, imageTag + getClass().getResource("/helpFiles/" + figureName + "." + figureType));
                index = helpText.indexOf(imageTag, index) + 1;
            }
            
            textJEditorPane.setText(helpText);

            if (fileName.getPath().endsWith("AboutPeptideShaker.html")) {
                setTitle("About");
                setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            } else {
                setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")));
            }
        } catch (Exception e) {

            e.printStackTrace();
            
            try {
                textJEditorPane.setPage(getClass().getResource("/helpfiles/DefaultHelpFile.html"));
            } catch (Exception ex) {
                textJEditorPane.setText("The selected help file is not yet available.");
            }
        }

        textJEditorPane.setCaretPosition(0);

        if (reference != null) {
            final String marker = reference;

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    textJEditorPane.scrollToReference(marker);
                }
            });
        }
        
        setSize(windowWidth, parent.getHeight() - windowHeightReduction);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Creates a new HelpWindow object with a Frame as a parent.
     *
     * @param parent
     * @param fileName the name of the help file
     */
    public HelpWindow(javax.swing.JDialog parent, URL fileName) {
        this(parent, fileName, null);
    }

    /**
     * Creates a new HelpWindow with a Frame as a parent.
     *
     * @param parent
     * @param fileName the name of the help file
     * @param reference a reference in the html file to scroll to, can be null
     */
    public HelpWindow(javax.swing.JDialog parent, URL fileName, String reference) {

        this.setAlwaysOnTop(true);
        this.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        initComponents();

        try {
            InputStream stream = fileName.openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            String s = b.readLine();
            String helpText = "";

            while (s != null) {
                helpText += s;
                s = b.readLine();
            }
            
            String imageTag = "<img src=\"";
            int index = 0;
            
            // replace any relative image tags by their static alternatives
            while (helpText.indexOf(imageTag, index) != -1) {     
                int startIndex = helpText.indexOf(imageTag, index) + imageTag.length();
                String figureName = helpText.substring(startIndex, helpText.indexOf(".", startIndex));
                String figureType = helpText.substring(helpText.indexOf(".", startIndex) + 1, helpText.indexOf("\"", startIndex));   
                helpText = helpText.replaceAll(imageTag + figureName + "." + figureType, imageTag + getClass().getResource("/helpFiles/" + figureName + "." + figureType));
                index = helpText.indexOf(imageTag, index) + 1;
            }

            textJEditorPane.setText(helpText);

            if (fileName.getPath().endsWith("AboutPeptideShaker.html")) {
                setTitle("About");
                setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            } else {
                setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")));
            }
        } catch (Exception e) {

            try {
                textJEditorPane.setPage(getClass().getResource("/helpfiles/DefaultHelpFile.html"));
            } catch (Exception ex) {
                textJEditorPane.setText("The selected help file is not yet available.");
            }
        }

        textJEditorPane.setCaretPosition(0);

        if (reference != null) {
            final String marker = reference;

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    textJEditorPane.scrollToReference(marker);
                }
            });
        }

        setSize(windowWidth, parent.getParent().getHeight() - windowHeightReduction);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        closeJButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        textJEditorPane = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PeptideShaker - Help");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        closeJButton.setText("Close");
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeJButtonActionPerformed(evt);
            }
        });

        textJEditorPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        textJEditorPane.setContentType("text/html");
        textJEditorPane.setEditable(false);
        textJEditorPane.setMinimumSize(new java.awt.Dimension(10, 10));
        textJEditorPane.setPreferredSize(new java.awt.Dimension(10, 10));
        textJEditorPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                textJEditorPaneHyperlinkUpdate(evt);
            }
        });
        jScrollPane1.setViewportView(textJEditorPane);

        org.jdesktop.layout.GroupLayout backgroundPanelLayout = new org.jdesktop.layout.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, closeJButton)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(closeJButton)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Closes the dialog 
     *
     * @param evt
     */
    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeJButtonActionPerformed

    /**
     * Makes the links active.
     *
     * @param evt
     */
    private void textJEditorPaneHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_textJEditorPaneHyperlinkUpdate
        if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ENTERED.toString())) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.EXITED.toString())) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ACTIVATED.toString())) {
            if (evt.getDescription().startsWith("#")) {
                textJEditorPane.scrollToReference(evt.getDescription());
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(evt.getDescription());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_textJEditorPaneHyperlinkUpdate
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton closeJButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JEditorPane textJEditorPane;
    // End of variables declaration//GEN-END:variables
}
