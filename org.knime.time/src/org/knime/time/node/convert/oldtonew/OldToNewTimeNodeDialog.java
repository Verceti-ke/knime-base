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

import java.time.ZoneId;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The node dialog of the node which converts old to new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class OldToNewTimeNodeDialog extends DefaultNodeSettingsPane {

    /** Setting up all DialogComponents. */
    OldToNewTimeNodeDialog() {

        /*
         * DialogComponents
         */
        createNewGroup("Column Selection");
        addDialogComponent(new DialogComponentColumnFilter2(OldToNewTimeNodeModel.createColSelectModel(), 0));

        createNewGroup("New Type Selection");
        final SettingsModelBoolean typeModelBool = OldToNewTimeNodeModel.createTypeModelBool();
        addDialogComponent(
            new DialogComponentBoolean(typeModelBool, "Automatic type detection (based on the first row)"));

        final String[] availableTypes = new String[]{"LocalDateTime", "ZonedDateTime", "LocalDate", "LocalTime"};
        final SettingsModelString typeSelectModel = OldToNewTimeNodeModel.createTypeSelectModel();
        typeSelectModel.setEnabled(!typeModelBool.getBooleanValue());
        addDialogComponent(new DialogComponentStringSelection(typeSelectModel, "New type: ", availableTypes));

        createNewGroup("Time Zone Selection");
        final SettingsModelBoolean zoneModelBool = OldToNewTimeNodeModel.createZoneModelBool();
        addDialogComponent(new DialogComponentBoolean(zoneModelBool, "Add time zone"));

        final SettingsModelString zoneSelectModel = OldToNewTimeNodeModel.createTimeZoneSelectModel();
        zoneSelectModel.setEnabled(false);
        addDialogComponent(
            new DialogComponentStringSelection(zoneSelectModel, "Time zone: ", ZoneId.getAvailableZoneIds()));

        /*
         * ChangeListeners
         */
        typeModelBool.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                typeSelectModel.setEnabled(!typeModelBool.getBooleanValue());
                zoneModelBool.setEnabled(typeModelBool.getBooleanValue());
                zoneModelBool.setEnabled(typeModelBool.getBooleanValue());
            }
        });

        typeSelectModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if (typeSelectModel.getStringValue() == "ZonedDateTime") {
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

}
