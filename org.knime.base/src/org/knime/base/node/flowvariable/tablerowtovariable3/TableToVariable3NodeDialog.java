/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tablerowtovariable3;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * <code>NodeDialog</code> for the "TableRowToVariable" node. Exports the first row of a table into variables.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TableToVariable3NodeDialog extends DefaultNodeSettingsPane {

    private final SettingsModelString m_onMissing;

    private final SettingsModelDouble m_replaceDouble;

    private final SettingsModelInteger m_replaceInteger;

    private final SettingsModelLong m_replaceLong;

    private final SettingsModelString m_replaceString;

    private final SettingsModelString m_replaceBoolean;

    private final SettingsModelColumnFilter2 m_columnFilter;

    /**
     * New pane for configuring the TableToVariable2 node.
     */
    TableToVariable3NodeDialog() {
        // Missing Values
        m_onMissing = getOnMissing();
        final DialogComponentButtonGroup missingGroup =
            new DialogComponentButtonGroup(m_onMissing, false, " Missing Values ", MissingValuePolicy.getAllSettings());
        missingGroup.setToolTipText("Applies to missing values and if the input table is empty");
        addDialogComponent(missingGroup);
        // Default Values
        createNewGroup(" Default Values ");
        m_replaceDouble = getReplaceDouble(m_onMissing);
        addDialogComponent(new DialogComponentNumber(m_replaceDouble, "Double: ", 0.1, 10));
        m_replaceInteger = getReplaceInteger(m_onMissing);
        addDialogComponent(new DialogComponentNumber(m_replaceInteger, "Integer: ", 1, 10));
        m_replaceLong = getReplaceLong(m_onMissing);
        addDialogComponent(new DialogComponentNumber(m_replaceLong, "Long: ", 1L, 10));
        m_replaceString = getReplaceString(m_onMissing);
        addDialogComponent(new DialogComponentString(m_replaceString, "String: ", true, 13));
        m_replaceBoolean = getReplaceBoolean(m_onMissing);
        addDialogComponent(new DialogComponentStringSelection(m_replaceBoolean, "Boolean: ", "false", "true"));
        m_columnFilter = getColumnFilter();
        addDialogComponent(new DialogComponentColumnFilter2(m_columnFilter, 0));
    }

    @Override
    public final void createNewGroup(final String title) {
        super.createNewGroup(title);
    }

    @Override
    public final void addDialogComponent(final DialogComponent diaC) {
        super.addDialogComponent(diaC);
    }

    static final SettingsModelString getOnMissing() {
        return new SettingsModelString("CFG_FAILONMISS", MissingValuePolicy.DEFAULT.getName());
    }

    static final SettingsModelDouble getReplaceDouble(final SettingsModelString policyModel) {
        final SettingsModelDouble model = new SettingsModelDouble("CFG_Double", 0);
        ChangeListener listener = new PolicyChangeListener(policyModel, model);
        policyModel.addChangeListener(listener);
        listener.stateChanged(null);
        return model;
    }

    static final SettingsModelString getReplaceString(final SettingsModelString policyModel) {
        SettingsModelString model = new SettingsModelString("CFG_String", "missing");
        ChangeListener listener = new PolicyChangeListener(policyModel, model);
        policyModel.addChangeListener(listener);
        listener.stateChanged(null);
        return model;
    }

    /**
     * This method returns a SettingsModelString, since a SettingsModelBoolean / DialogComponentBoolean would be
     * represented as a checkbox with the label placed behind. Not only does that look weird, it is also less intuitive
     * to use and is not in line with the other options in the dialog, which have their label in the front.
     */
    static final SettingsModelString getReplaceBoolean(final SettingsModelString policyModel) {
        final SettingsModelString model = new SettingsModelString("CFG_Boolean", "false");
        final ChangeListener listener = new PolicyChangeListener(policyModel, model);
        policyModel.addChangeListener(listener);
        listener.stateChanged(null);
        return model;
    }

    static final SettingsModelInteger getReplaceInteger(final SettingsModelString policyModel) {
        SettingsModelInteger model = new SettingsModelInteger("CFG_Integer", 0);
        ChangeListener listener = new PolicyChangeListener(policyModel, model);
        policyModel.addChangeListener(listener);
        listener.stateChanged(null);
        return model;
    }

    static final SettingsModelLong getReplaceLong(final SettingsModelString policyModel) {
        final SettingsModelLong model = new SettingsModelLong("CFG_Long", 0L);
        final ChangeListener listener = new PolicyChangeListener(policyModel, model);
        policyModel.addChangeListener(listener);
        listener.stateChanged(null);
        return model;
    }

    static final SettingsModelColumnFilter2 getColumnFilter() {
        return new SettingsModelColumnFilter2("column_selection", null,
            NameFilterConfiguration.FILTER_BY_NAMEPATTERN | DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE);
    }

    private static class PolicyChangeListener implements ChangeListener {

        private SettingsModelString m_policyModel;

        private SettingsModel m_model;

        PolicyChangeListener(final SettingsModelString policyModel, final SettingsModel defaultValueModel) {
            m_policyModel = policyModel;
            m_model = defaultValueModel;
        }

        @Override
        public void stateChanged(final ChangeEvent arg0) {
            boolean isDefaultMissValue = MissingValuePolicy.DEFAULT.getName().equals(m_policyModel.getStringValue());
            m_model.setEnabled(isDefaultMissValue);
        }

    }
}
