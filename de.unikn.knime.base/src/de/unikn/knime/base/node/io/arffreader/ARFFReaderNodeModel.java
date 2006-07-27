/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   11.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffreader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.swing.filechooser.FileFilter;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.util.StringHistory;

/**
 * The model of the ARFF reader node. The interesting work is done in the
 * {@link de.unikn.knime.base.node.io.arffreader.ARFFTable} and
 * {@link de.unikn.knime.base.node.io.arffreader.ARFFRowIterator}.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFReaderNodeModel extends NodeModel {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ARFFReaderNodeModel.class);

    /** Key used to store the ARFF file location in the settings object. */
    static final String CFGKEY_FILEURL = "FileURL";

    /** Key used to store the row prefix in the settings object. */
    static final String CFGKEY_ROWPREFIX = "RowPrefix";

    private static final String ARFF_HISTORY_ID = "ARFFFiles";

    private String m_rowPrefix;

    private URL m_file;

    /**
     * Creates a new ARFF reader model.
     */
    public ARFFReaderNodeModel() {
        super(0, 1);
        m_rowPrefix = null;
        m_file = null;
        reset();
    }

    /**
     * Creates a new ARFF reader with a default file.
     * 
     * @param arffFileLocation URL to the ARFF file to read
     */
    public ARFFReaderNodeModel(final String arffFileLocation) {
        this();
        try {
            m_file = stringToURL(arffFileLocation);
        } catch (MalformedURLException mue) {
            LOGGER.error(mue.getMessage());
        }
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_file == null) {
            throw new InvalidSettingsException("File is not specified.");
        }
        try {
            return new DataTableSpec[]{ARFFTable
                    .createDataTableSpecFromARFFfile(m_file, null)};
        } catch (IOException ioe) {
            throw new InvalidSettingsException("ARFFReader: I/O Error", ioe);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("ARFFReader: ARFF Header Error",
                    ise);
        } catch (CanceledExecutionException cee) {
            // never flies
            throw new InvalidSettingsException(
                    "ARFFReader: User canceled action.");
        }
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        assert m_file != null;
        if (m_file == null) {
            throw new NullPointerException("Configure the ARFF reader before"
                    + " you execute it, please.");
        }
        // now that we actually read it, add it to the history.
        ARFFReaderNodeModel.addToFileHistory(m_file.toString());

        BufferedDataTable out = exec.createBufferedDataTable(new ARFFTable(
                m_file,
                ARFFTable.createDataTableSpecFromARFFfile(m_file, exec),
                m_rowPrefix), exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_file = stringToURL(settings.getString(CFGKEY_FILEURL));
        } catch (MalformedURLException mue) {
            throw new InvalidSettingsException(mue);
        }
        m_rowPrefix = settings.getString(CFGKEY_ROWPREFIX);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // nothing to do.
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #saveInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #loadInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (m_file != null) {
            settings.addString(CFGKEY_FILEURL, m_file.toString());
        } else {
            settings.addString(CFGKEY_FILEURL, null);
        }
        settings.addString(CFGKEY_ROWPREFIX, m_rowPrefix);

    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            stringToURL(settings.getString(CFGKEY_FILEURL));
        } catch (MalformedURLException mue) {
            throw new InvalidSettingsException(mue);
        }
        settings.getString(CFGKEY_ROWPREFIX);
    }

    /**
     * Tries to create an URL from the passed string.
     * 
     * @param url the string to transform into an URL
     * @return URL if entered value could be properly tranformed, or
     * @throws MalformedURLException if the value entered in the text field was
     *             invalid
     */
    public static URL stringToURL(final String url)
            throws MalformedURLException {

        if ((url == null) || (url.equals(""))) {
            throw new MalformedURLException("Specify a not empty valid URL");
        }

        URL newURL;
        try {
            newURL = new URL(url);
        } catch (Exception e) {
            // see if they specified a file without giving the protocol
            File tmp = new File(url);

            // if that blows off we let the exception go up the stack.
            newURL = tmp.getAbsoluteFile().toURL();
        }
        return newURL;
    }

    /**
     * @param removeNotExistingFiles if <code>true</code> the returned list
     *            will not contain files that doesn't exist (they will not be
     *            removed from the global history though
     * @return the current file history associated with the ARFF reader/writer
     */
    public static String[] getFileHistory(final boolean removeNotExistingFiles) {

        StringHistory h = StringHistory.getInstance(ARFF_HISTORY_ID);
        Vector<String> allLocs = new Vector<String>();

        for (int l = 0; l < h.getHistory().length; l++) {
            String loc = h.getHistory()[l];

            if (removeNotExistingFiles) {
                URL url;
                try {
                    url = new URL(loc);
                    if (url.getProtocol().equalsIgnoreCase("FILE")) {
                        // if we have a file location check its existence
                        File f = new File(url.getPath());
                        if ((f != null) && (f.exists())) {
                            allLocs.add(loc);
                        } // else ignore old, not existing entries
                    } else {
                        // non-file URL we just take over
                        allLocs.add(loc);
                    }
                } catch (MalformedURLException mue) {
                    // ignore this (invalid) entry in the history
                }

            } else {
                allLocs.add(loc);
            }

        }
        return allLocs.toArray(new String[0]);

    }

    /**
     * Adds the specified string to the ARFF reader/writer history.
     * 
     * @param filename the filename to add
     */
    public static void addToFileHistory(final String filename) {
        StringHistory h = StringHistory.getInstance(ARFF_HISTORY_ID);
        h.add(filename);
    }

    /**
     * FileFilter for the ARFFReader/writer file chooser dialog.
     * 
     * @author Peter Ohl, University of Konstanz
     */
    public static class ARFFFileFilter extends FileFilter {

        /**
         * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
         */
        @Override
        public boolean accept(final File f) {
            if (f != null) {
                if (f.isDirectory()) {
                    return true;
                }
                String lastFive = f.getName().substring(
                        f.getName().length() - 5, f.getName().length());
                return lastFive.equalsIgnoreCase(".arff");
            }
            return true;

        }

        /**
         * @see javax.swing.filechooser.FileFilter#getDescription()
         */
        @Override
        public String getDescription() {
            return "ARFF data files (*.arff)";
        }
    }
}
