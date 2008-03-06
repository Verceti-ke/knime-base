/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   27.02.2008 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DBConnectionDialogPanel extends JPanel {
    
    private final DialogComponentButtonGroup m_group;
    
    private final DialogComponentNumber m_cacheRows;
    
    /**
     * Options the handle incoming SQL statement.
     */
    enum DBTableOptions implements ButtonGroupEnumInterface {
        /** Wraps the SQL into a new SELECT statement. */
        WRAP_SQL("keep_sql", "Keep SQL query", true),
        /** Creates a new TABLE in the database. */
        CREATE_TABLE("create_table", "Create TABLE", false);
        private final boolean m_isDefault;
        private final String m_label;
        private final String m_id;
        /**
         * Creates a new table option object.
         * @param id the identifier
         * @param label the label of this component
         * @param isDefault if default
         */
        DBTableOptions(final String id, final String label, 
                final boolean isDefault) {
            m_isDefault = isDefault;
            m_label = label;
            m_id = id;
        }
        /**
         * {@inheritDoc}
         */
        public String getActionCommand() {
            return m_id;
        }
        /**
         * {@inheritDoc}
         */
        public String getText() {
            return m_label;
        }
        /**
         * {@inheritDoc}
         */
        public String getToolTip() {
            return null;
        }
        /**
         * {@inheritDoc}
         */
        public boolean isDefault() {
            return m_isDefault;
        }
    }
    
    /**
     * Creates a new panel used to select number of rows to cache and the 
     * method to handle the SQL statement.
     */
    public DBConnectionDialogPanel() {
        super(new BorderLayout());
        m_group = new DialogComponentButtonGroup(createTableModel(), null, true,
                DBTableOptions.values());
        super.add(m_group.getComponentPanel(), BorderLayout.NORTH);
        m_cacheRows = new DialogComponentNumber(createCachedRowsModel(), 
                "No. of row to cache: ", 100);
        super.add(m_cacheRows.getComponentPanel(), BorderLayout.CENTER);
    }
    
    /**
     * @return new settings model to create wrapped SQL statement
     */
    static final SettingsModelString createTableModel() {
        return new SettingsModelString("table_option", 
                DBTableOptions.WRAP_SQL.m_label);
    }
    
    /**
     * @return new settings model to resrict number of cached rows
     */
    static final SettingsModelIntegerBounded createCachedRowsModel() {
        return new SettingsModelIntegerBounded(
                "no_cached_rows", 100, 0, Integer.MAX_VALUE);
    }

    /**
     * Loads dialog settings. 
     * @param settings to load
     * @param specs input spec 
     * @throws NotConfigurableException if the settings are not applicable
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_group.loadSettingsFrom(settings, specs);
        m_cacheRows.loadSettingsFrom(settings, specs);
    }
    
    /**
     * Saves the dialog settings.
     * @param settings to save into
     * @throws InvalidSettingsException if the settings can't be saved
     */
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_group.saveSettingsTo(settings);
        m_cacheRows.saveSettingsTo(settings);
    }
    
}
