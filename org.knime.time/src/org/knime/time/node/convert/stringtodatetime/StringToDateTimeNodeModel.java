/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 19, 2016 (simon): created
 */
package org.knime.time.node.convert.stringtodatetime;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.node.convert.DateTimeTypes;

/**
 * The node model of the node which converts strings to the new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class StringToDateTimeNodeModel extends NodeModel {

    private final SettingsModelColumnFilter2 m_colSelect = StringToDateTimeNodeDialog.createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = StringToDateTimeNodeDialog.createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = StringToDateTimeNodeDialog.createSuffixModel();

    private final SettingsModelString m_format = StringToDateTimeNodeDialog.createFormatModel();

    private final SettingsModelString m_locale = StringToDateTimeNodeDialog.createLocaleModel();

    private final SettingsModelBoolean m_cancelOnFail = StringToDateTimeNodeDialog.createCancelOnFailModel();

    private String m_selectedType;

    private int m_failCounter;

    /**
     * one in, one out
     */
    protected StringToDateTimeNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_selectedType == null) {
            m_selectedType = DateTimeTypes.LOCAL_DATE_TIME.name();
        }
        final ColumnRearranger columnRearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{columnRearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final ColumnRearranger columnRearranger = createColumnRearranger(inData[0].getDataTableSpec());
        final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], columnRearranger, exec);
        if (m_failCounter > 0) {
            setWarningMessage(
                m_failCounter + " rows could not be converted. Check the message in the missing cells for details.");
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * @param inSpec table input spec
     * @return the CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        final int[] includeIndeces =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
        int i = 0;
        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(StringToDateTimeNodeDialog.OPTION_REPLACE)) {
                final DataColumnSpecCreator dataColumnSpecCreator =
                    new DataColumnSpecCreator(includedCol, DateTimeTypes.valueOf(m_selectedType).getDataType());
                final StringToTimeCellFactory cellFac =
                    new StringToTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec).newColumn(
                    includedCol + m_suffix.getStringValue(), DateTimeTypes.valueOf(m_selectedType).getDataType());
                final StringToTimeCellFactory cellFac = new StringToTimeCellFactory(dataColSpec, includeIndeces[i++]);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];
                final DataTableSpec inSpec = in.getDataTableSpec();
                final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
                final int[] includeIndeces = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                    .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
                final boolean isReplace =
                    m_isReplaceOrAppend.getStringValue().equals(StringToDateTimeNodeDialog.OPTION_REPLACE);

                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    DataCell[] datacells = new DataCell[includeIndeces.length];
                    for (int i = 0; i < includeIndeces.length; i++) {
                        if (isReplace) {
                            final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(
                                includeList[i], DateTimeTypes.valueOf(m_selectedType).getDataType());
                            final StringToTimeCellFactory cellFac =
                                new StringToTimeCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i]);
                            datacells[i] = cellFac.getCell(row);
                        } else {
                            final DataColumnSpec dataColSpec =
                                new UniqueNameGenerator(inSpec).newColumn(includeList[i] + m_suffix.getStringValue(),
                                    DateTimeTypes.valueOf(m_selectedType).getDataType());
                            final StringToTimeCellFactory cellFac =
                                new StringToTimeCellFactory(dataColSpec, includeIndeces[i]);
                            datacells[i] = cellFac.getCell(row);
                        }
                    }
                    if (isReplace) {
                        out.push(new ReplacedColumnsDataRow(row, datacells, includeIndeces));
                    } else {
                        out.push(new AppendedColumnRow(row, datacells));
                    }
                }
                in.close();
                out.close();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_format.saveSettingsTo(settings);
        m_locale.saveSettingsTo(settings);
        m_cancelOnFail.saveSettingsTo(settings);
        settings.addString("typeEnum", m_selectedType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_format.validateSettings(settings);
        m_locale.validateSettings(settings);
        m_cancelOnFail.validateSettings(settings);
        final SettingsModelString formatClone = m_format.createCloneWithValidatedValue(settings);
        final String format = formatClone.getStringValue();
        if (StringUtils.isEmpty(format)) {
            throw new InvalidSettingsException("Format must not be empty!");
        }
        try {
            DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            String msg = "Invalid date format: \"" + format + "\".";
            final String errMsg = e.getMessage();
            if (!StringUtils.isEmpty(errMsg)) {
                msg += " Reason: " + errMsg;
            }
            throw new InvalidSettingsException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_format.loadSettingsFrom(settings);
        m_locale.loadSettingsFrom(settings);
        try {
            LocaleUtils.toLocale(m_locale.getStringValue());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(
                "Unsupported locale in setting (" + m_locale.getStringValue() + "): " + ex.getMessage(), ex);
        }

        m_cancelOnFail.loadSettingsFrom(settings);
        m_selectedType = settings.getString("typeEnum");
        final String dateformat = m_format.getStringValue();
        // if it is not a predefined one -> store it
        if (!StringToDateTimeNodeDialog.PREDEFINED_FORMATS.contains(dateformat)) {
            StringHistory.getInstance(StringToDateTimeNodeDialog.FORMAT_HISTORY_KEY).add(dateformat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_failCounter = 0;
    }

    /**
     * This cell factory converts a single Date&Time cell to a String cell.
     */
    final class StringToTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        /**
         * @param inSpec table input spec
         * @param colIndex index of the column to work on
         */
        public StringToTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
            super(inSpec);
            m_colIndex = colIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            try {
                final String input = ((StringValue)cell).getStringValue();
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_format.getStringValue(),
                    LocaleUtils.toLocale(m_locale.getStringValue()));

                switch (DateTimeTypes.valueOf(m_selectedType)) {
                    case LOCAL_DATE: {
                        final LocalDate ld = LocalDate.parse(input, formatter);
                        return LocalDateCellFactory.create(ld);
                    }
                    case LOCAL_TIME: {
                        final LocalTime lt = LocalTime.parse(input, formatter);
                        return LocalTimeCellFactory.create(lt);
                    }
                    case LOCAL_DATE_TIME: {
                        final LocalDateTime ldt = LocalDateTime.parse(input, formatter);
                        return LocalDateTimeCellFactory.create(ldt);
                    }
                    case ZONED_DATE_TIME: {
                        final ZonedDateTime zdt = ZonedDateTime.parse(input, formatter);
                        return ZonedDateTimeCellFactory.create(zdt);
                    }
                    default:
                        throw new IllegalStateException("Unhandled date&time type: " + m_selectedType);
                }
            } catch (DateTimeParseException e) {
                m_failCounter++;
                if (m_cancelOnFail.getBooleanValue()) {
                    throw new IllegalArgumentException(
                        "Failed to parse date in row '" + row.getKey() + ": " + e.getMessage());
                }
                return new MissingCell(e.getMessage());
            }
        }
    }
}
