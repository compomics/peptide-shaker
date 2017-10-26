package eu.isas.peptideshaker.preferences;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * In this class are stored the user preferences for PeptideShaker.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class UserPreferences implements Serializable {

    /**
     * The serial number for serialization compatibility.
     */
    static final long serialVersionUID = 3298905131097982664L;
    /**
     * The recent projects.
     */
    private final ArrayList<String> recentProjects = new ArrayList<>();
    /**
     * Show/hide sliders.
     */
    private boolean showSliders = false;

    /**
     * Constructor.
     */
    public UserPreferences() {
    }

    /**
     * Returns whether sliders should be displayed.
     *
     * @return whether sliders should be displayed
     */
    public boolean showSliders() {
        return showSliders;
    }

    /**
     * Sets whether sliders should be displayed.
     *
     * @param showSliders whether sliders should be displayed
     */
    public void setShowSliders(boolean showSliders) {
        this.showSliders = showSliders;
    }

    /**
     * Returns the paths of the recent projects.
     *
     * @return the paths of the recent projects
     */
    public ArrayList<String> getRecentProjects() {
        return recentProjects;
    }

    /**
     * Removes a recent project from the list.
     *
     * @param recentProject the recent project to remove
     */
    public void removeRecentProject(String recentProject) {
        
        recentProjects.remove(recentProject);
    }

    /**
     * Adds a recent project to the list and limits the list of recent projects
     * to a size of 20.
     *
     * @param recentProject the path of the recent project to add
     */
    public void addRecentProject(String recentProject) {
        if (recentProjects.contains(recentProject)) {
            recentProjects.remove(recentProject);
        }
        recentProjects.add(0, recentProject);
        while (recentProjects.size() > 20) {
            recentProjects.remove(recentProjects.size() - 1);
        }
    }

    /**
     * Adds a recent project to the list and limits the list of recent projects
     * to a size of 20.
     *
     * @param recentProject the recent project to add
     */
    public void addRecentProject(File recentProject) {
        addRecentProject(recentProject.getAbsolutePath());
    }
}
