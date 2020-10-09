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
 *   June 08, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.tempdir;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.util.ThreadUtils.ThreadWithContext;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * The NodeModel for the "Create Temp Dir" Node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CreateTempDir2NodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CreateTempDir2NodeModel.class);

    private static final int MISSING_FS_PORT_IDX = -1;

    private final CreateTempDir2NodeConfig m_config;

    private final int m_fsConnectionPortIdx;

    private final NodeModelStatusConsumer m_statusConumser;

    private final Map<FSLocationSpec, List<FSLocation>> m_onResetTempDirs;

    private final Map<FSLocationSpec, List<FSLocation>> m_onDisposeTempDirs;

    private FSConnection m_fsConnection;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    CreateTempDir2NodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = new CreateTempDir2NodeConfig(portsConfig);
        m_fsConnectionPortIdx = Optional
            .ofNullable(
                portsConfig.getInputPortLocation().get(CreateTempDir2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME))
            .map(idx -> idx[0])//
            .orElse(MISSING_FS_PORT_IDX);
        m_statusConumser = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.INFO));
        m_onResetTempDirs = new HashMap<>();
        m_onDisposeTempDirs = new HashMap<>();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getParentDirChooserModel().configureInModel(inSpecs, m_statusConumser);
        m_statusConumser.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        try (final WritePathAccessor writePathAccessor =
            m_config.getParentDirChooserModel().createWritePathAccessor()) {
            final FSPath parentPath = writePathAccessor.getOutputPath(m_statusConumser);
            m_statusConumser.setWarningsIfRequired(this::setWarningMessage);
            if (!Files.exists(parentPath)) {
                if (m_config.getParentDirChooserModel().isCreateMissingFolders()) {
                    FSFiles.createDirectories(parentPath);
                } else {
                    throw new IOException(String.format(
                        "The directory '%s' does not exist and must not be created due to user settings.", parentPath));
                }
            }
            final FSPath tempDirFSPath;
            if (m_fsConnectionPortIdx != MISSING_FS_PORT_IDX) {
                tempDirFSPath = FSFiles.createTempDirectory(parentPath, m_config.getTempDirPrefix(), "");
                if (m_config.deleteDirOnReset()) {
                    m_fsConnection = ((FileSystemPortObject)inObjects[m_fsConnectionPortIdx]).getFileSystemConnection()
                        .orElseThrow(IllegalStateException::new);
                    markForDeletion(tempDirFSPath, m_onResetTempDirs);
                }
            } else {
                tempDirFSPath = FSFiles.createRandomizedDirectory(parentPath, m_config.getTempDirPrefix(), "");
                markForDeletion(tempDirFSPath);
            }
            createFlowVariables(tempDirFSPath);
            return new PortObject[]{FlowVariablePortObject.INSTANCE};
        }
    }

    private void markForDeletion(final FSPath tempDirFSPath) {
        if (m_config.deleteDirOnReset()) {
            markForDeletion(tempDirFSPath, m_onResetTempDirs);
        }
        markForDeletion(tempDirFSPath, m_onDisposeTempDirs);
    }

    private static void markForDeletion(final FSPath tempDirFSPath,
        final Map<FSLocationSpec, List<FSLocation>> tempDirs) {
        FSLocation fsLocation = tempDirFSPath.toFSLocation();
        tempDirs
            .computeIfAbsent(new DefaultFSLocationSpec(fsLocation.getFileSystemCategory(),
                fsLocation.getFileSystemSpecifier().orElse(null)), k -> new ArrayList<>())//
            .add(fsLocation);
    }

    private void createFlowVariables(final FSPath tempDirFSPath) {
        pushFSLocationVariable(m_config.getTempDirVariableName(), tempDirFSPath.toFSLocation());

        for (int i = 0; i < m_config.getAdditionalVarNames().length; i++) {
            final FSPath additionalPath = (FSPath)tempDirFSPath.resolve(m_config.getAdditionalVarValues()[i]);
            pushFSLocationVariable(m_config.getAdditionalVarNames()[i], additionalPath.toFSLocation());
        }
    }

    private void pushFSLocationVariable(final String name, final FSLocation var) {
        pushFlowVariable(name, FSLocationVariableType.INSTANCE, var);
    }

    @Override
    protected void onDispose() {
        deleteTempDirs(m_onDisposeTempDirs);
        super.onDispose();
    }

    @Override
    protected void reset() {
        if (!m_onResetTempDirs.isEmpty()) {
            for (final Entry<FSLocationSpec, List<FSLocation>> entry : m_onResetTempDirs.entrySet()) {
                if (m_onDisposeTempDirs.containsKey(entry.getKey())) {
                    final List<FSLocation> onDisposePaths = m_onDisposeTempDirs.get(entry.getKey());
                    onDisposePaths.removeAll(entry.getValue());
                    if (onDisposePaths.isEmpty()) {
                        m_onDisposeTempDirs.remove(entry.getKey());
                    }
                }
            }
            deleteTempDirs(m_onResetTempDirs);
            m_onResetTempDirs.clear();
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        setWarningMessage("Temporary directory has been deleted. Please re-execute the node.");
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    private void deleteTempDirs(final Map<FSLocationSpec, List<FSLocation>> tempDirs) {
        if (!tempDirs.isEmpty()) {
            new DeleteTempDirs(m_fsConnection, tempDirs).start();
            m_fsConnection = null;
        }
    }

    private static final class DeleteTempDirs extends ThreadWithContext {

        private final Map<FSLocationSpec, List<FSLocation>> m_dirsToDelete;

        private Optional<FSConnection> m_connection;

        DeleteTempDirs(final FSConnection connection, final Map<FSLocationSpec, List<FSLocation>> dirsToDelete) {
            m_connection = Optional.ofNullable(connection);
            m_dirsToDelete = new HashMap<>(dirsToDelete);
        }

        @Override
        protected void runWithContext() {
            for (final Entry<FSLocationSpec, List<FSLocation>> entry : m_dirsToDelete.entrySet()) {
                try (FSPathProviderFactory factory = FSPathProviderFactory.newFactory(m_connection, entry.getKey())) {
                    for (FSLocation loc : entry.getValue()) {
                        deleteTempDir(factory, loc);
                    }
                } catch (ClosedFileSystemException e) { // NOSONAR can be ignored
                    // do nothing, can be safely ignored because temp dir has already been deleted when closing the file system
                } catch (final IOException facE) {
                    LOGGER.debug("Unable to close path provider factory for " + entry.getKey().toString() + " "
                        + facE.getMessage(), facE);
                }
            }
        }

        private static void deleteTempDir(final FSPathProviderFactory factory, final FSLocation loc) {
            try (FSPathProvider pathProvider = factory.create(loc)) {
                FSFiles.deleteRecursively(pathProvider.getPath());
            } catch (final IOException provE) {
                LOGGER.debug("Unable to delete temp directory: " + provE.getMessage(), provE);
            }
        }
    }

}
