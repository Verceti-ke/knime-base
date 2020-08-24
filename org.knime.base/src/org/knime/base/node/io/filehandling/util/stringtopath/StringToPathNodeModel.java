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
 *   Jul 28, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */

package org.knime.base.node.io.filehandling.util.stringtopath;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.data.location.cell.FSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The NodeModel for the "String to Path" Node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class StringToPathNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StringToPathNodeModel.class);

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Data Table";

    private static final int DATA_TABLE_OUTPUT_IDX = 0;

    private static final String CFG_FILE_SYSTEM = "file_system";

    private static final String CFG_SELECTED_COLUMN_NAME = "selected_column_name";

    private static final String CFG_GENERATED_COLUMN_MODE = "generated_column_mode";

    private static final String CFG_APPENDED_COLUMN_NAME = "appended_column_name";

    private static final String CFG_ABORT_ON_MISSING_FILE = "fail_on_missing_file_folder";

    private static final String CFG_FAIL_ON_MISSING_VALS = "fail_on_missing_values";

    private final SettingsModelFileSystem m_fileSystemModel;

    private final SettingsModelString m_selectedColumnNameModel = createSettingsModelColumnName();

    private final SettingsModelString m_generatedColumnModeModel = createSettingsModelColumnMode();

    private final SettingsModelString m_appendedColumnNameModel = createSettingsModelAppendedColumnName();

    private final SettingsModelBoolean m_abortOnMissingFileModel = createSettingsModelAbortOnMissingFile();

    private final SettingsModelBoolean m_failOnMissingValues = createSettingsModelFailOnMissingValues();

    private final NodeModelStatusConsumer m_statusConsumer;

    private final int m_dataTablePortIndex;

    static SettingsModelFileSystem createSettingsModelFileSystem(final PortsConfiguration portsConfig) {
        return new SettingsModelFileSystem(CFG_FILE_SYSTEM, portsConfig,
            StringToPathNodeModel.CONNECTION_INPUT_PORT_GRP_NAME, EnumSet.allOf(FSCategory.class));
    }

    static SettingsModelString createSettingsModelColumnName() {
        return new SettingsModelString(CFG_SELECTED_COLUMN_NAME, "");
    }

    static SettingsModelString createSettingsModelColumnMode() {
        return new SettingsModelString(CFG_GENERATED_COLUMN_MODE, GenerateColumnMode.APPEND_NEW.getActionCommand());
    }

    static SettingsModelString createSettingsModelAppendedColumnName() {
        return new SettingsModelString(CFG_APPENDED_COLUMN_NAME, "Path");
    }

    static SettingsModelBoolean createSettingsModelAbortOnMissingFile() {
        return new SettingsModelBoolean(CFG_ABORT_ON_MISSING_FILE, false);
    }

    static SettingsModelBoolean createSettingsModelFailOnMissingValues() {
        return new SettingsModelBoolean(CFG_FAIL_ON_MISSING_VALS, true);
    }

    StringToPathNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_dataTablePortIndex = portsConfig.getInputPortLocation().get(DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.INFO));
        m_fileSystemModel = createSettingsModelFileSystem(portsConfig);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fileSystemModel.configureInModel(inSpecs, m_statusConsumer);

        DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        DataTableSpec outSpec = null;

        try (final StringToPathCellFactory factory = createStringPathCellFactory(inSpec, null)) {
            outSpec = createColumnRearranger(inSpec, factory).createSpec();
            return new PortObjectSpec[]{outSpec};
        }
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        final BufferedDataTable inTable = (BufferedDataTable)data[m_dataTablePortIndex];
        final DataTableSpec inSpec = inTable.getDataTableSpec();
        try (final StringToPathCellFactory factory = createStringPathCellFactory(inSpec, exec)) {
            final BufferedDataTable out =
                exec.createColumnRearrangeTable(inTable, createColumnRearranger(inSpec, factory), exec);
            return new BufferedDataTable[]{out};
        }
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        // assumes that the order is (dynamic) FS connection port before data table input port
        return m_dataTablePortIndex == 0 //
            ? new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE}//
            : new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StringToPathStreamableOperator();

    }

    /**
     * Create a {@link ColumnRearranger} that either replaces the selected column with its Path counterpart, or appends
     * a new column.
     *
     * @param inSpec specification of the input table
     * @param factory the {@link StringToPathCellFactory}
     * @return {@link ColumnRearranger} that will append a new column or replace the selected column
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final StringToPathCellFactory factory) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Either replace or append a column depending on users choice
        if (isReplaceMode(m_generatedColumnModeModel)) {
            rearranger.replace(factory, m_selectedColumnNameModel.getStringValue());
        } else {
            rearranger.append(factory);
        }
        return rearranger;
    }

    private StringToPathCellFactory createStringPathCellFactory(final DataTableSpec inSpec, final ExecutionContext exec)
        throws InvalidSettingsException {
        final String selectedColumn = m_selectedColumnNameModel.getStringValue();
        final int colIdx = inSpec.findColumnIndex(selectedColumn);
        checkSettings(inSpec, colIdx, selectedColumn);
        DataColumnSpec colSpec = getNewColumnSpec(inSpec);
        return new StringToPathCellFactory(colSpec, colIdx, exec);
    }

    private DataColumnSpec getNewColumnSpec(final DataTableSpec inSpec) {
        final String columnName = isReplaceMode(m_generatedColumnModeModel) ? m_selectedColumnNameModel.getStringValue()
            : DataTableSpec.getUniqueColumnName(inSpec, m_appendedColumnNameModel.getStringValue());
        return new DataColumnSpecCreator(columnName, FSLocationCellFactory.TYPE).createSpec();
    }

    /**
     * Check if all the settings are valid.
     *
     * @param inSpec Specification of the input table
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private void checkSettings(final DataTableSpec inSpec, final int colIdx, final String selectedColumn)
        throws InvalidSettingsException {

        CheckUtils.checkSetting(colIdx >= 0, "Please select a column", selectedColumn);
        final DataColumnSpec colSpec = inSpec.getColumnSpec(colIdx);
        CheckUtils.checkSetting(colSpec.getType().isCompatible(StringValue.class),
            "Selected column '%s' is not a string column", selectedColumn);

        if (isAppendMode(m_generatedColumnModeModel)) {
            // Is column name empty?
            if (m_appendedColumnNameModel.getStringValue() == null
                || m_appendedColumnNameModel.getStringValue().trim().isEmpty()) {
                throw new InvalidSettingsException("Column name cannot be empty");
            }
            if (inSpec.findColumnIndex(m_appendedColumnNameModel.getStringValue()) != -1) {
                setWarningMessage("Column name already taken, using unique auto-generated column name instead.");
            }
        }
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileSystemModel.saveSettingsTo(settings);
        m_selectedColumnNameModel.saveSettingsTo(settings);
        m_abortOnMissingFileModel.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_appendedColumnNameModel.saveSettingsTo(settings);
        m_generatedColumnModeModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileSystemModel.loadSettingsFrom(settings);
        m_selectedColumnNameModel.loadSettingsFrom(settings);
        m_abortOnMissingFileModel.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_appendedColumnNameModel.loadSettingsFrom(settings);
        m_generatedColumnModeModel.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileSystemModel.validateSettings(settings);
        m_selectedColumnNameModel.validateSettings(settings);
        m_abortOnMissingFileModel.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
        m_appendedColumnNameModel.validateSettings(settings);
        m_generatedColumnModeModel.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    private class StringToPathCellFactory extends SingleCellFactory implements Closeable {

        private final int m_colIdx;

        private final FSLocationFactory m_fsLocationFactory;

        private final FSLocationCellFactory m_fsLocationCellFactory;

        private final FSPathProviderFactory m_fsPathProviderFactory;

        /**
         * Cell factory to convert cells from String to Path
         *
         * @param newColSpec the spec for the new column
         * @param colIdx the index of the selected column
         * @param exec execution context used to create file store for {@link FSLocationCellFactory}
         */
        StringToPathCellFactory(final DataColumnSpec newColSpec, final int colIdx, final ExecutionContext exec) {
            super(newColSpec);
            m_colIdx = colIdx;
            if (exec != null) {
                m_fsLocationFactory = m_fileSystemModel.createFSLocationFactory();

                final FSLocationSpec locationSpec = m_fileSystemModel.getLocationSpec();

                m_fsLocationCellFactory =
                    new FSLocationCellFactory(FileStoreFactory.createFileStoreFactory(exec), locationSpec);

                if (m_abortOnMissingFileModel.getBooleanValue()) {
                    m_fsPathProviderFactory =
                        FSPathProviderFactory.newFactory(m_fileSystemModel.getConnection(), locationSpec);
                } else {
                    m_fsPathProviderFactory = null;
                }
            } else {
                m_fsLocationFactory = null;
                m_fsLocationCellFactory = null;
                m_fsPathProviderFactory = null;
            }
        }

        @Override
        public DataCell getCell(final DataRow row) {
            return createPathCell(row);
        }

        private DataCell createPathCell(final DataRow row) {
            DataCell pathCell;
            final DataCell tempCell = row.getCell(m_colIdx);

            if (!tempCell.isMissing()) {
                final String cellValue = ((StringValue)tempCell).getStringValue();

                CheckUtils.checkArgument(cellValue != null && !cellValue.trim().isEmpty(),
                    "Selected column contains empty string(s).");

                final FSLocation fsLocation = m_fsLocationFactory.createLocation(cellValue);

                if (m_fsPathProviderFactory != null) {
                    checkIfFileExists(fsLocation);
                }

                pathCell = m_fsLocationCellFactory.createCell(fsLocation);

            } else {
                if (m_failOnMissingValues.getBooleanValue()) {
                    throw new MissingValueException((MissingValue)tempCell,
                        "Selected column contains missing cell(s).");
                }
                pathCell = DataType.getMissingCell();
            }

            return pathCell;
        }

        private void checkIfFileExists(final FSLocation fsLocation) {
            try (final FSPathProvider pathProvider = m_fsPathProviderFactory.create(fsLocation)) {
                final FSPath fsPath = pathProvider.getPath();
                Files.readAttributes(fsPath, BasicFileAttributes.class);
            } catch (final IOException e) {
                throw new IllegalArgumentException(
                    String.format("The file/folder '%s' does not exists or cannot be accessed", fsLocation.getPath()), e);
            }
        }

        @Override
        public void close() {
            // we can catch those exceptions as they cannot occur and even if we don't need to care about them here
            if (m_fsLocationFactory != null) {
                try {
                    m_fsLocationFactory.close();
                } catch (final IOException e) {
                    LOGGER.debug("Unable to close fs location factory", e);
                }
            }
            if (m_fsPathProviderFactory != null) {
                try {
                    m_fsPathProviderFactory.close();
                } catch (final IOException e) {
                    LOGGER.debug("Unable to close fs path factory", e);
                }
            }
        }
    }

    /**
     * Inner class allowing the node to be executed in streaming mode.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private class StringToPathStreamableOperator extends StreamableOperator {

        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
            throws Exception {
            final RowInput in = (RowInput)inputs[m_dataTablePortIndex];
            final RowOutput out = (RowOutput)outputs[DATA_TABLE_OUTPUT_IDX];
            final DataTableSpec inSpec = in.getDataTableSpec();
            try (StringToPathCellFactory factory = createStringPathCellFactory(inSpec, exec)) {
                final StreamableFunction streamableFunction =
                    createColumnRearranger(inSpec, factory).createStreamableFunction();
                DataRow row;
                while ((row = in.poll()) != null) {
                    out.push(streamableFunction.compute(row));
                }
            } finally {
                in.close();
                out.close();
            }
        }
    }

    /**
     * Options used to decide whether the newly generated column should replace selected column or should be appended as
     * new.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    enum GenerateColumnMode implements ButtonGroupEnumInterface {

            APPEND_NEW("Append column:"), //
            REPLACE_SELECTED("Replace selected column");

        private static final String TOOLTIP =
            "<html>The newly generated column should replace selected column or should be appended as new.";

        private final String m_text;

        GenerateColumnMode(final String text) {
            this.m_text = text;
        }

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return TOOLTIP;
        }

        @Override
        public boolean isDefault() {
            return this == APPEND_NEW;
        }
    }

    /**
     * A convenience method to check if a SettingsModelString value is {@code generatedColumnMode.APPEND_NEW}
     *
     * @param settingsModel the settings model to check
     * @return {@code true} if and only if the generatedColumnMode is APPEND_NEW
     */
    static boolean isAppendMode(final SettingsModelString settingsModel) {
        return GenerateColumnMode.valueOf(settingsModel.getStringValue()) == GenerateColumnMode.APPEND_NEW;
    }

    /**
     * A convenience method to check if a SettingsModelString value is {@code generatedColumnMode.REPLACE_SELECTED}
     *
     * @param settingsModel the settings model to check
     * @return {@code true} if and only if the generatedColumnMode is {@code generatedColumnMode.REPLACE_SELECTED}
     */
    static boolean isReplaceMode(final SettingsModelString settingsModel) {
        return GenerateColumnMode.valueOf(settingsModel.getStringValue()) == GenerateColumnMode.REPLACE_SELECTED;
    }
}