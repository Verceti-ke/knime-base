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
 *   Sep 2, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.mockProductionPath;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for DefaultStagedMultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultStagedMultiTableReadTest {

    private static final String TABLE_NAME = "default";

    private static final TypedReaderColumnSpec<String> COL1 = trcs("A", "X");

    private static final TypedReaderColumnSpec<String> COL2 = trcs("B", "Y");

    private static final TypedReaderColumnSpec<String> COL3 = trcs("C", "Z");

    private static final TypedReaderTableSpec<String> SPEC1 = new TypedReaderTableSpec<>(asList(COL1, COL2));

    private static final TypedReaderTableSpec<String> SPEC2 = new TypedReaderTableSpec<>(asList(COL2, COL3));

    private static final String ROOT = "root";

    private static final TypedReaderTableSpec<String> UNION = new TypedReaderTableSpec<>(asList(COL1, COL2, COL3));

    private static final TypedReaderTableSpec<String> INTERSECTION = new TypedReaderTableSpec<>(asList(COL2));

    private static final RawSpec<String> RAW_SPEC = new RawSpec<>(UNION, INTERSECTION);

    @Mock
    private TableReader<DummyReaderSpecificConfig, String, String> m_tableReader;

    @Mock
    private GenericRowKeyGeneratorContextFactory<Path, String> m_rowKeyGenContextFactory;

    @Mock
    private GenericRowKeyGeneratorContext<Path, String> m_rowKeyGenContext;

    @Mock
    private TableReadConfig<DummyReaderSpecificConfig> m_tableReadConfig;

    @Mock
    private TableTransformation<String> m_defaultTransformationModel;

    @Mock
    private Path m_path1;

    @Mock
    private Path m_path2;

    @Mock
    private Supplier<ReadAdapter<String, String>> m_readAdapterSupplier;

    @Mock
    private ColumnTransformation<String> m_transformation1;

    @Mock
    private ColumnTransformation<String> m_transformation2;

    @Mock
    private ColumnTransformation<String> m_transformation3;

    private List<Path> m_paths;

    private ProductionPath m_pp1;

    private ProductionPath m_pp2;

    private ProductionPath m_pp3;

    private DefaultStagedMultiTableRead<DummyReaderSpecificConfig, String, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        Mockito.when(m_defaultTransformationModel.getRawSpec()).thenReturn(RAW_SPEC);
        final Map<Path, TypedReaderTableSpec<String>> individualSpecs = new LinkedHashMap<>();

        m_pp1 = mockProductionPath();
        m_pp2 = mockProductionPath();
        m_pp3 = mockProductionPath();

        m_paths = asList(m_path1, m_path2);
        individualSpecs.put(m_path1, SPEC1);
        individualSpecs.put(m_path2, SPEC2);
        m_testInstance = new DefaultStagedMultiTableRead<>(m_tableReader, ROOT, individualSpecs,
            m_rowKeyGenContextFactory, m_readAdapterSupplier, m_defaultTransformationModel, m_tableReadConfig);
    }

    /**
     * Tests the implementation of {@link StagedMultiTableRead#withoutTransformation()}.
     */
    @Test
    public void testWithoutTransformation() {

        stubTransformationModel(m_defaultTransformationModel);

        final DataTableSpec knimeSpec = new DataTableSpec(TABLE_NAME, new String[]{"A", "B", "C"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE});

        final MultiTableRead mtr = m_testInstance.withoutTransformation(m_paths);
        assertEquals(knimeSpec, mtr.getOutputSpec());

    }

    private static TypedReaderColumnSpec<String> trcs(final String name, final String type) {
        return TypedReaderColumnSpec.createWithName(name, type, true);
    }

    /**
     * Tests the implementation of {@link StagedMultiTableRead#withTransformation(TableTransformation)}.
     */
    @Test
    public void testWithTransformationTypeMapping() {
        TableTransformation<String> lastColumnToString = mockTransformationModel();
        when(m_pp3.getConverterFactory().getDestinationType()).thenReturn(StringCell.TYPE);
        final DataTableSpec knimeSpec = new DataTableSpec(TABLE_NAME, new String[]{"A", "B", "C"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, StringCell.TYPE});
        MultiTableRead mtr = m_testInstance.withTransformation(m_paths, lastColumnToString);
        assertEquals(knimeSpec, mtr.getOutputSpec());
    }

    /**
     * Tests filtering columns.
     */
    @Test
    public void testWithTransformationFiltering() {
        TableTransformation<String> filterSecondColumn = mockTransformationModel();
        when(m_transformation2.keep()).thenReturn(false);
        final DataTableSpec knimeSpec =
            new DataTableSpec(TABLE_NAME, new String[]{"A", "C"}, new DataType[]{StringCell.TYPE, DoubleCell.TYPE});
        MultiTableRead mtr = m_testInstance.withTransformation(m_paths, filterSecondColumn);
        assertEquals(knimeSpec, mtr.getOutputSpec());
    }

    /**
     * Tests renaming columns.
     */
    @Test
    public void testWithTransformationRenaming() {
        TableTransformation<String> renameSecondColumn = mockTransformationModel();
        when(renameSecondColumn.getTransformation(COL2)).thenReturn(m_transformation2);
        when(m_transformation2.getName()).thenReturn("M");
        final DataTableSpec knimeSpec = new DataTableSpec(TABLE_NAME, new String[]{"A", "M", "C"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE});
        MultiTableRead mtr = m_testInstance.withTransformation(m_paths, renameSecondColumn);
        assertEquals(knimeSpec, mtr.getOutputSpec());
    }

    /**
     * Tests reordering columns.
     */
    @Test
    public void testWithTransformationReordering() {
        TableTransformation<String> reorderColumns = mockTransformationModel();
        when(m_transformation2.getPosition()).thenReturn(2);
        when(m_transformation3.getPosition()).thenReturn(1);
        when(m_transformation2.compareTo(ArgumentMatchers.any())).thenReturn(1);
        when(m_transformation3.compareTo(m_transformation1)).thenReturn(1);
        when(m_transformation3.compareTo(m_transformation2)).thenReturn(-1);

        final DataTableSpec knimeSpec = new DataTableSpec(TABLE_NAME, new String[]{"A", "C", "B"},
            new DataType[]{StringCell.TYPE, DoubleCell.TYPE, IntCell.TYPE});
        MultiTableRead mtr = m_testInstance.withTransformation(m_paths, reorderColumns);
        assertEquals(knimeSpec, mtr.getOutputSpec());
    }

    private TableTransformation<String> mockTransformationModel() {
        @SuppressWarnings("unchecked")
        TableTransformation<String> transformationModel = mock(TableTransformation.class);
        stubTransformationModel(transformationModel);
        return transformationModel;
    }

    private static void stubTransformation(final ColumnTransformation<String> mock,
        final TypedReaderColumnSpec<String> spec, final String name, final ProductionPath prodPath, final int position,
        final boolean keep) {
        when(mock.getName()).thenReturn(name);
        when(mock.getProductionPath()).thenReturn(prodPath);
        when(mock.getPosition()).thenReturn(position);
        when(mock.keep()).thenReturn(keep);
    }

    private void stubTransformationModel(final TableTransformation<String> transformationModel) {
        stubTransformation(m_transformation1, COL1, "A", m_pp1, 0, true);
        stubTransformation(m_transformation2, COL2, "B", m_pp2, 1, true);
        stubTransformation(m_transformation3, COL3, "C", m_pp3, 2, true);

        when(transformationModel.getTransformation(COL1)).thenReturn(m_transformation1);
        when(transformationModel.getTransformation(COL2)).thenReturn(m_transformation2);
        when(transformationModel.getTransformation(COL3)).thenReturn(m_transformation3);

        when(m_pp1.getConverterFactory().getDestinationType()).thenReturn(StringCell.TYPE);
        when(m_pp2.getConverterFactory().getDestinationType()).thenReturn(IntCell.TYPE);
        when(m_pp3.getConverterFactory().getDestinationType()).thenReturn(DoubleCell.TYPE);

        when(transformationModel.getRawSpec()).thenReturn(RAW_SPEC);
        when(transformationModel.getColumnFilterMode()).thenReturn(ColumnFilterMode.UNION);
    }

    /**
     * Tests the implementation of {@link StagedMultiTableRead#getRawSpec()}.
     */
    @Test
    public void testGetRawSpec() {
        assertEquals(RAW_SPEC, m_testInstance.getRawSpec());
    }

    /**
     * Tests the implementation of {@link StagedMultiTableRead#isValidFor(java.util.Collection)}.
     */
    @Test
    public void testIsValidFor() {
        assertTrue(m_testInstance.isValidFor(asList(m_path1, m_path2)));
        assertFalse(m_testInstance.isValidFor(asList(m_path1)));
        final Path unknown = mock(Path.class);
        assertFalse(m_testInstance.isValidFor(asList(m_path1, unknown)));
    }

}
