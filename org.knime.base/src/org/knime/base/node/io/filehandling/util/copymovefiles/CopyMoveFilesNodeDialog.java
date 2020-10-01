/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 5, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.copymovefiles;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.util.IncludeParentFolderAvailableSwingWorker;
import org.knime.base.node.io.filehandling.util.SwingWorkerManager;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Node dialog of the Copy/Move Files node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class CopyMoveFilesNodeDialog extends NodeDialogPane {

    private final DialogComponentReaderFileChooser m_sourceFilePanel;

    //    private final DialogComponentColumnNameSelection m_selectedDestinationColumn;

    private final DialogComponentColumnNameSelection m_selectedSourceColumn;

    private final DialogComponentWriterFileChooser m_destinationFilePanel;

    private final DialogComponentBoolean m_deleteSourceFilesCheckbox;

    private final DialogComponentBoolean m_includeParentFolderCheckbox;

    private final SwingWorkerManager m_swingWorkerManager;

    /**
     * Constructor.
     *
     * @param config the CopyMoveFilesNodeConfig
     */
    CopyMoveFilesNodeDialog(final CopyMoveFilesNodeConfig config, final PortsConfiguration portsConfiguration) {
        final SettingsModelReaderFileChooser sourceFileChooserConfig = config.getSourceFileChooserModel();
        final SettingsModelWriterFileChooser destinationFileChooserConfig = config.getDestinationFileChooserModel();

        final FlowVariableModel sourceFvm =
            createFlowVariableModel(sourceFileChooserConfig.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserConfig.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);

        m_sourceFilePanel = new DialogComponentReaderFileChooser(sourceFileChooserConfig, "source_chooser", sourceFvm,
            FilterMode.FILE, FilterMode.FILES_IN_FOLDERS, FilterMode.FOLDER);

        m_destinationFilePanel = new DialogComponentWriterFileChooser(destinationFileChooserConfig,
            "destination_chooser", writeFvm, s -> new CopyMoveFilesStatusMessageReporter(s,
                sourceFileChooserConfig.createClone(), config.getSettingsModelIncludeParentFolder().getBooleanValue()),
            FilterMode.FOLDER);

        m_deleteSourceFilesCheckbox =
            new DialogComponentBoolean(config.getDeleteSourceFilesModel(), "Delete source files (move)");

        m_includeParentFolderCheckbox = new DialogComponentBoolean(config.getSettingsModelIncludeParentFolder(),
            "Include parent folder from source path");

        //Update the component in case something changes so that the status message will be updated accordingly
        sourceFileChooserConfig.addChangeListener(l -> m_destinationFilePanel.updateComponent());
        config.getSettingsModelIncludeParentFolder().addChangeListener(l -> m_destinationFilePanel.updateComponent());

        m_swingWorkerManager = new SwingWorkerManager(
            () -> new IncludeParentFolderAvailableSwingWorker(sourceFileChooserConfig::createReadPathAccessor,
                sourceFileChooserConfig.getFilterModeModel().getFilterMode(),
                m_includeParentFolderCheckbox.getModel()::setEnabled));

        sourceFileChooserConfig.addChangeListener(l -> m_swingWorkerManager.startSwingWorker());

        final ColumnFilter filter = createColumnFilter();

        m_selectedSourceColumn =
            new DialogComponentColumnNameSelection(config.getSelectedSourceColumnModel(), "Source Column",
                portsConfiguration.getInputPortLocation().get(CopyMoveFilesNodeModel.TABLE_PORT_GRP_NAME)[0], false,
                filter);

        createPanel();
    }

    //TODO desc and so on
    private static ColumnFilter createColumnFilter() {
        return new ColumnFilter() {
            @Override
            public final boolean includeColumn(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(FSLocationValue.class);
            }

            @Override
            public final String allFilteredMsg() {
                return "No applicable column available";
            }
        };
    }

    /**
     * Method to create the panel.
     */
    private void createPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.add(createSourcePanel(), gbc);
        gbc.gridy++;
        panel.add(createDestinationPanel(), gbc);
        gbc.gridy++;
        panel.add(createOptionPanel(), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        addTab("Settings", panel);
    }

    /**
     * Creates the source file chooser panel.
     */
    private JPanel createSourcePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Source"));
        panel.add(m_sourceFilePanel.getComponentPanel(), gbc);

        return panel;
    }

    /**
     * Creates the option file chooser panel.
     */
    private JPanel createOptionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        gbc.weightx = 0;
        panel.add(m_includeParentFolderCheckbox.getComponentPanel(), gbc);
        gbc.gridx++;
        panel.add(m_deleteSourceFilesCheckbox.getComponentPanel(), gbc);
        gbc.gridx++;
        panel.add(m_selectedSourceColumn.getComponentPanel(), gbc);
        gbc.weightx = 1;
        gbc.gridx++;
        panel.add(Box.createHorizontalBox(), gbc);
        return panel;
    }

    /**
     * Creates the destination file chooser panel.
     */
    private JPanel createDestinationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));
        panel.add(m_destinationFilePanel.getComponentPanel(), gbc);

        return panel;
    }

    /**
     * Creates the initial {@link GridBagConstraints}.
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1;
        return gbc;
    }

    /**
     * {@inheritDoc}
     *
     * Cancels the {@link IncludeParentFolderAvailableSwingWorker} when the dialog will be closed.
     */
    @Override
    public void onClose() {
        m_swingWorkerManager.cancelSwingWorker();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_destinationFilePanel.saveSettingsTo(settings);
        m_sourceFilePanel.saveSettingsTo(settings);
        m_deleteSourceFilesCheckbox.saveSettingsTo(settings);
        m_includeParentFolderCheckbox.saveSettingsTo(settings);
        m_selectedSourceColumn.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sourceFilePanel.loadSettingsFrom(settings, specs);
        m_destinationFilePanel.loadSettingsFrom(settings, specs);
        m_deleteSourceFilesCheckbox.loadSettingsFrom(settings, specs);
        m_includeParentFolderCheckbox.loadSettingsFrom(settings, specs);
        m_selectedSourceColumn.loadSettingsFrom(settings, specs);
    }
}
