/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.time.node.convert.oldtonew;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.node.convert.DateTimeTypes;

/**
 * The node dialog of the node which converts old to new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class OldToNewTimeNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentBoolean m_dialogCompZoneBool;

    private final JComboBox<DateTimeTypes> m_typeCombobox;

    private final DialogComponentBoolean m_dialogCompTypeBool;

    private final DialogComponentStringSelection m_dialogCompTimeZoneSelec;

    /** Setting up all DialogComponents. */
    OldToNewTimeNodeDialog() {

        /*
         * DialogComponents
         */
        m_dialogCompColFilter = new DialogComponentColumnFilter2(OldToNewTimeNodeModel.createColSelectModel(), 0);

        final SettingsModelBoolean typeModelBool = OldToNewTimeNodeModel.createTypeModelBool();
        m_dialogCompTypeBool =
            new DialogComponentBoolean(typeModelBool, "Automatic type detection (based on the first row)");

        final SettingsModelBoolean zoneModelBool = OldToNewTimeNodeModel.createZoneModelBool();
        m_dialogCompZoneBool = new DialogComponentBoolean(zoneModelBool, "Add time zone");

        final SettingsModelString zoneSelectModel = OldToNewTimeNodeModel.createTimeZoneSelectModel();
        final Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();
        final String[] availableZoneIdsArray = availableZoneIds.toArray(new String[availableZoneIds.size()]);
        Arrays.sort(availableZoneIdsArray);
        zoneSelectModel.setEnabled(zoneModelBool.getBooleanValue());
        m_dialogCompTimeZoneSelec =
            new DialogComponentStringSelection(zoneSelectModel, "Time zone: ", availableZoneIdsArray);

        /*
         * create panel with gbc
         */
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;

        /*
         * add column filter
         */
        final JPanel panelColFilter = new JPanel(new BorderLayout());
        panelColFilter.setBorder(BorderFactory.createTitledBorder("Column Selection"));
        panelColFilter.add(m_dialogCompColFilter.getComponentPanel(), BorderLayout.CENTER);
        panel.add(m_dialogCompColFilter.getComponentPanel(), gbc);

        /*
         * add type selection
         */
        gbc.gridy++;
        gbc.weighty = 0;
        final JPanel panelTypeSelec = new JPanel(new GridBagLayout());
        panelTypeSelec.setBorder(BorderFactory.createTitledBorder("New Type Selection"));
        final GridBagConstraints gbcTS = new GridBagConstraints();
        // add check box
        gbcTS.fill = GridBagConstraints.BOTH;
        gbcTS.gridx = 0;
        gbcTS.gridy = 0;
        gbcTS.weightx = 1;
        gbcTS.weighty = 1;
        panelTypeSelec.add(m_dialogCompTypeBool.getComponentPanel(), gbcTS);

        // add label and combo box for type selection
        gbcTS.gridx++;
        final JPanel panelTypeList = new JPanel(new FlowLayout());
        final JLabel label = new JLabel("New type: ");
        panelTypeList.add(label);
        m_typeCombobox = new JComboBox<DateTimeTypes>(DateTimeTypes.values());
        m_typeCombobox.setEnabled(false);
        panelTypeList.add(m_typeCombobox);
        panelTypeSelec.add(panelTypeList, gbcTS);

        gbc.gridy++;
        panel.add(panelTypeSelec, gbc);

        /*
         * add time zone selection
         */
        final JPanel panelZoneSelec = new JPanel(new GridBagLayout());
        panelZoneSelec.setBorder(BorderFactory.createTitledBorder("Time Zone Selection"));
        final GridBagConstraints gbcZS = new GridBagConstraints();
        gbcZS.fill = GridBagConstraints.BOTH;
        gbcZS.gridx = 0;
        gbcZS.gridy = 0;
        gbcZS.weightx = 0.5;
        panelZoneSelec.add(m_dialogCompZoneBool.getComponentPanel(), gbcZS);
        gbcZS.gridx++;
        panelZoneSelec.add(m_dialogCompTimeZoneSelec.getComponentPanel(), gbcZS);

        gbc.gridy++;
        panel.add(panelZoneSelec, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change and action listeners
         */
        typeModelBool.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_typeCombobox.setEnabled(!typeModelBool.getBooleanValue());
                zoneModelBool.setEnabled(typeModelBool.getBooleanValue());
                zoneModelBool.setEnabled(typeModelBool.getBooleanValue());
            }
        });

        m_typeCombobox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_typeCombobox.getModel().getSelectedItem().equals(DateTimeTypes.ZONED_DATE_TIME)) {
                    zoneSelectModel.setEnabled(true);
                } else {
                    zoneSelectModel.setEnabled(false);
                }
            }
        });

        zoneModelBool.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                zoneSelectModel.setEnabled(zoneModelBool.getBooleanValue());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColFilter.saveSettingsTo(settings);
        m_dialogCompTypeBool.saveSettingsTo(settings);
        m_dialogCompZoneBool.saveSettingsTo(settings);
        m_dialogCompTimeZoneSelec.saveSettingsTo(settings);
        settings.addBoolean("typeBool", m_typeCombobox.isEnabled());
        settings.addString("newTypeEnum", ((DateTimeTypes)m_typeCombobox.getModel().getSelectedItem()).name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompTypeBool.loadSettingsFrom(settings, specs);
        m_typeCombobox.setEnabled(!m_dialogCompTypeBool.isSelected());
        m_typeCombobox
            .setSelectedItem(DateTimeTypes.valueOf(settings.getString("newTypeEnum", DateTimeTypes.LOCAL_DATE.name())));
        m_dialogCompZoneBool.loadSettingsFrom(settings, specs);
        m_dialogCompTimeZoneSelec.loadSettingsFrom(settings, specs);
    }

}
