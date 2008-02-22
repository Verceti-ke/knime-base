/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.03.2006 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerTable.MissingValueReplacementFunction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * Abstract basisfunction model holding the trained rule table.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionLearnerNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BasisFunctionLearnerNodeModel.class);

    /** The choice of distance function. */
    private int m_distance = 0;

    /** Key for choice of distance measure. */
    public static final String DISTANCE = "distance_function";

    /** An array of possible distance measures. */
    public static final Distance[] DISTANCES = {Distance.getInstance()};

    /** NodeSettings key for <i>shrink_after_commit</i>. */
    public static final String SHRINK_AFTER_COMMIT = "shrink_after_commit";

    /** NodeSettings key for <i>max_class_coverage</i>. */
    public static final String MAX_CLASS_COVERAGE = "max_class_coverage";
    
    /** Key of the target column. */
    public static final String TARGET_COLUMNS = "target_column";
    
    /** Key of the target column. */
    public static final String DATA_COLUMNS = "data_columns";

    /**
     * Keeps names of all columns used to make classification during training.
     */
    private String[] m_targetColumns = null;
    
    /** Keeps names of all numeric columns used for training. */
    private String[] m_dataColumns = null;
    
    /** Keeps a value for missing replacement function index. */
    private int m_missing = -1;

    /** The <i>shrink_after_commit</i> flag. */
    private boolean m_shrinkAfterCommit = true;
    
    /** The <i>max_class_coverage</i> flag. */
    private boolean m_maxCoverage = true;
    
    /** Config key for maximum number of epochs. */
    public static final String MAX_EPOCHS = "max_epochs";
    
    /** Maximum number of epochs to train. */
    private int m_maxEpochs = -1;

    /** Contains model info after training. */
    private ModelContent m_modelInfo;

    private DataColumnSpec[] m_modelSpec;

    private final Map<DataCell, List<BasisFunctionLearnerRow>> m_bfs;

    /** Translates hilite events between model and training data. */
    private final HiLiteTranslator m_translator;
    
    /** Number of data inports. */
    public static final int NR_DATA_INS   = 1;
    /** Number of data outports. */
    public static final int NR_DATA_OUTS  = 1;
    /** Number of model inports. */
    public static final int NR_MODEL_INS  = 0;
    /** Number of model outports. */
    public static final int NR_MODEL_OUTS = 1;

    /**
     * Creates a new model with one data in and out port, and model outport.
     */
    protected BasisFunctionLearnerNodeModel() {
        super(1, 1, 0, 1);
        m_bfs = new LinkedHashMap<DataCell, List<BasisFunctionLearnerRow>>();
        m_translator = new HiLiteTranslator(new DefaultHiLiteHandler());
    }

    /**
     * Reset the trained model.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_modelInfo = null;
        m_modelSpec = null;
        m_bfs.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int id, final HiLiteHandler hdl) {
        assert (id == 0);
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hdl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        assert (outPortID == 0);
        return m_translator.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec[] configure(final DataTableSpec[] ins)
            throws InvalidSettingsException {
        // check if target column available
        if (m_targetColumns == null) {
            throw new InvalidSettingsException("Target columns not available.");
        }
        for (String target : m_targetColumns) {
            if (!ins[0].containsName(target)) {
                throw new InvalidSettingsException(
                    "Target \"" + target + "\" column not available.");
            }
            if (m_targetColumns.length > 1) {
                if (!ins[0].getColumnSpec(target).getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException(
                            "Target \"" + target 
                            + "\" column not of type DoubleValue.");
                }
            }
        }
        // check if double type column available
        if (!ins[0].containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "No data column of type DoubleValue found.");
        }
        
        List<String> targetHash = Arrays.asList(m_targetColumns);
        // if only one double type column, check if not the target column
        for (int i = 0; i < ins[0].getNumColumns(); i++) {
            DataColumnSpec cspec = ins[0].getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                if (!targetHash.contains(cspec.getName())) {
                    break;
                }
            }
            // if last column was tested
            if (i + 1 == ins[0].getNumColumns()) {
                throw new InvalidSettingsException("Found only one column of"
                        + " type DoubleValue: " 
                        + Arrays.toString(m_targetColumns));
            }
        }
        
        // if no data columns are found, use all numeric columns
        List<String> dataCols = new ArrayList<String>();
        if (m_dataColumns == null || m_dataColumns.length == 0) {
            for (DataColumnSpec cspec : ins[0]) {
                if (!targetHash.contains(cspec.getName())
                        && cspec.getType().isCompatible(DoubleValue.class)) {
                    dataCols.add(cspec.getName());
                }
            }
            m_dataColumns = dataCols.toArray(new String[dataCols.size()]);
        }
        
        // check data columns, only numeric
        for (String dataColumn : m_dataColumns) {
            if (!ins[0].containsName(dataColumn)) {
                throw new InvalidSettingsException(
                    "Data \"" + dataColumn + "\" column not available.");
            }
            if (!ins[0].getColumnSpec(dataColumn).getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException(
                            "Data \"" + dataColumn 
                            + "\" column not of type DoubleValue.");
            }
        }
        
        return new DataTableSpec[]{BasisFunctionFactory.createModelSpec(ins[0],
                m_dataColumns, m_targetColumns, getModelType())};
    }

    /**
     * @return the type of the learned model cells
     */
    public abstract DataType getModelType();

    /**
     * Starts the learning algorithm in the learner.
     * 
     * @param data the input training data at index 0
     * @param exec the execution monitor
     * @return the output fuzzy rule model
     * @throws CanceledExecutionException if the training was canceled
     */
    @Override
    public BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        // check input data
        assert (data != null && data.length == 1 && data[0] != null);

        // find all double cell columns in the data
        DataTableSpec tSpec = data[0].getDataTableSpec();
        LinkedHashSet<String> columns = new LinkedHashSet<String>(tSpec
                .getNumColumns());
        List<String> targetHash = Arrays.asList(m_targetColumns);
        for (int c = 0; c < tSpec.getNumColumns(); c++) {
            DataColumnSpec cSpec = tSpec.getColumnSpec(c);
            String name = cSpec.getName();
            if (!targetHash.contains(name)) {
                // TODO only numeric columns allowed
                if (cSpec.getType().isCompatible(DoubleValue.class)) {
                    columns.add(cSpec.getName());
                }
            }
        }
        // add target columns at the end
        columns.addAll(Arrays.asList(m_targetColumns));
        
        // if no data columns are found, use all numeric columns
        List<String> dataCols = new ArrayList<String>();
        if (m_dataColumns == null || m_dataColumns.length == 0) {
            for (DataColumnSpec cspec : tSpec) {
                if (!targetHash.contains(cspec.getName())
                        && cspec.getType().isCompatible(DoubleValue.class)) {
                    dataCols.add(cspec.getName());
                }
            }
            m_dataColumns = dataCols.toArray(new String[dataCols.size()]);
        }

        // filter selected columns from input data
        String[] cols = columns.toArray(new String[]{});
        ColumnRearranger colRe = new ColumnRearranger(tSpec);
        colRe.keepOnly(cols);
        BufferedDataTable trainData = exec.createColumnRearrangeTable(
                data[0], colRe, exec);

        // print settings info
        LOGGER.debug("distance     : " + getDistance());
        LOGGER.debug("missing      : " + getMissingFct());
        LOGGER.debug("targets      : " + m_targetColumns);
        LOGGER.debug("shrink_commit: " + isShrinkAfterCommit());
        LOGGER.debug("max_coverage : " + isMaxClassCoverage());
        LOGGER.debug("max #epochs  : " + m_maxEpochs);

        // create factory
        BasisFunctionFactory factory = getFactory(
                trainData.getDataTableSpec());
        // start training
        BasisFunctionLearnerTable table = new BasisFunctionLearnerTable(
                trainData, m_dataColumns, m_targetColumns, factory,
                BasisFunctionLearnerTable.MISSINGS[m_missing],
                m_shrinkAfterCommit, m_maxCoverage, m_maxEpochs, exec);
        DataTableSpec modelSpec = table.getDataTableSpec();
        m_modelSpec = new DataColumnSpec[modelSpec.getNumColumns()];
        for (int i = 0; i < m_modelSpec.length; i++) {
            DataColumnSpecCreator creator = 
                new DataColumnSpecCreator(modelSpec.getColumnSpec(i));
            creator.removeAllHandlers();
            m_modelSpec[i] = creator.createSpec();
        }

        // set translator mapping
        m_translator.setMapper(table.getHiLiteMapper());

        m_bfs.putAll(table.getBasisFunctions());

        ModelContent modelInfo = new ModelContent(MODEL_INFO);
        table.saveInfos(modelInfo);
        m_modelInfo = modelInfo;

        // set out data table
        return new BufferedDataTable[]{exec
                .createBufferedDataTable(table, exec)};
    } // execute(DataTable[], ExecutionMonitor)

    /**
     * Create factory to generate BasisFunctions.
     * 
     * @param spec the cleaned data for training
     * @return factory to create special basis function rules
     */
    public abstract BasisFunctionFactory getFactory(DataTableSpec spec);

    /**
     * @return get model info after training or <code>null</code>
     */
    public ModelContentRO getModelInfo() {
        return m_modelInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringBuffer msg = new StringBuffer();
        // get target columns
        String[] targetColumns = null;
        try {
            targetColumns = settings.getStringArray(TARGET_COLUMNS);
            if (targetColumns == null || targetColumns.length == 0) {
                msg.append("No target column specified.\n");
            }
        } catch (InvalidSettingsException ise) {
            // try to read only one target ref. to KNIME 1.2.0 and before
            targetColumns = 
                new String[]{settings.getString(TARGET_COLUMNS, null)};
        }
        if (targetColumns == null || targetColumns.length == 0) {
            msg.append("Target columns not found in settings.\n");
        }
        // get data columns
        String[] dataColumns = null;
        try {
            dataColumns = settings.getStringArray(DATA_COLUMNS);
        } catch (InvalidSettingsException ise) {
            // suppress since before 1.2.0 all numeric data columns were used
            // msg.append("Data columns not found in settings.\n");
        }
//        if (dataColumns == null || dataColumns.length == 0) {
//            msg.append("No data column specified.\n");
//        }
        if (dataColumns != null && targetColumns != null) {
            Set<String> hash = new HashSet<String>(Arrays.asList(dataColumns));
            for (String target : targetColumns) {
                if (hash.contains(target)) {
                    msg.append("Target and data columns overlap in: " 
                            + Arrays.toString(targetColumns) + "\n");
                }
            }
        }
        // distance function
        int distance = settings.getInt(DISTANCE, -1);
        if (distance < 0 || distance > DISTANCES.length) {
            msg.append("Distance function index out of range: " + distance);
        }
        // missing replacement method
        int missing = settings.getInt(BasisFunctionLearnerTable.MISSING, -1);
        if (missing < 0 
                || missing > BasisFunctionLearnerTable.MISSINGS.length) {
            msg.append("Missing replacement function index out of range: "
                    + missing);
        }
        // if message length contains chars
        if (msg.length() > 0) {
            throw new InvalidSettingsException(msg.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // target columns for classification
        m_targetColumns = settings.getStringArray(
                TARGET_COLUMNS, (String[]) null);
        if (m_targetColumns == null) {
            // try to find single target column from version 1.2.0 and before
            m_targetColumns = new String[]{settings.getString(
                    TARGET_COLUMNS, null)};
        }
        // data columns for training
        m_dataColumns = settings.getStringArray(
                DATA_COLUMNS, (String[]) null);
        // missing value replacement
        m_missing = settings.getInt(BasisFunctionLearnerTable.MISSING);
        // distance function
        m_distance = settings.getInt(DISTANCE);
        // shrink after commit
        m_shrinkAfterCommit = settings.getBoolean(SHRINK_AFTER_COMMIT);
        // max class coverage
        m_maxCoverage = settings.getBoolean(MAX_CLASS_COVERAGE, true);
        // maximum epochs
        m_maxEpochs = settings.getInt(MAX_EPOCHS, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        // selected target columns
        settings.addStringArray(TARGET_COLUMNS, m_targetColumns);
        // selected target columns
        settings.addStringArray(DATA_COLUMNS, m_dataColumns);
        // missing value replacement function
        settings.addInt(BasisFunctionLearnerTable.MISSING, m_missing);
        // distance function
        settings.addInt(DISTANCE, m_distance);
        // shrink after commit
        settings.addBoolean(SHRINK_AFTER_COMMIT, m_shrinkAfterCommit);
        // max class coverage
        settings.addBoolean(MAX_CLASS_COVERAGE, m_maxCoverage);
        // maximum number of epochs
        settings.addInt(MAX_EPOCHS, m_maxEpochs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index, final ModelContentWO pp)
            throws InvalidSettingsException {
        assert index == 0 : index;
        // add used columns
        assert m_modelSpec != null;
        ModelContentWO modelSpec = pp.addModelContent("model_spec");
        for (int i = 0; i < m_modelSpec.length; i++) {
            DataColumnSpec cspec = m_modelSpec[i];
            cspec.save(modelSpec.addConfig(cspec.getName()));
        }
        // save basisfunctions
        ModelContentWO ruleSpec = pp.addModelContent("rules");
        for (DataCell key : m_bfs.keySet()) {
            List<BasisFunctionLearnerRow> list = m_bfs.get(key);
            for (BasisFunctionLearnerRow bf : list) {
                BasisFunctionPredictorRow predBf = bf.getPredictorRow();
                ModelContentWO bfParam = ruleSpec.addModelContent(bf.getKey()
                        .getId().toString());
                predBf.save(bfParam);
            }
        }
    }

    /** Model info identifier. */
    public static final String MODEL_INFO = "model_info";

    /** Model info file extension. */
    public static final String MODEL_INFO_FILE_NAME = MODEL_INFO + ".pmml.gz";

    /** File name for hilite mapping. */
    public static final String HILITE_MAPPING_FILE_NAME = 
        "hilite_mapping.pmml.gz";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        // load model info
        exec.checkCanceled();
        exec.setProgress(0.1, "Loading model information");
        File file = new File(internDir, MODEL_INFO_FILE_NAME);
        m_modelInfo = (ModelContent)ModelContent
                .loadFromXML(new GZIPInputStream(new BufferedInputStream(
                        new FileInputStream(file))));
        // load hilite mapping
        exec.checkCanceled();
        exec.setProgress(0.5, "Loading hilite mapping");
        File mappingFile = new File(internDir, HILITE_MAPPING_FILE_NAME);
        NodeSettingsRO mapSettings = NodeSettings
                .loadFromXML(new GZIPInputStream(new BufferedInputStream(
                        new FileInputStream(mappingFile))));
        try {
            m_translator.setMapper(DefaultHiLiteMapper.load(mapSettings));
        } catch (InvalidSettingsException ise) {
            m_translator.setMapper(null);
            throw new IOException(ise.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        assert (m_modelInfo != null);
        // save model info
        exec.checkCanceled();
        exec.setProgress(0.1, "Saving model information");
        File file = new File(internDir, MODEL_INFO_FILE_NAME);
        m_modelInfo.saveToXML(new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(file))));
        // save hilite mapping
        exec.checkCanceled();
        exec.setProgress(0.5, "Saving hilite mapping");
        NodeSettings mapSettings = new NodeSettings(HILITE_MAPPING_FILE_NAME);
        DefaultHiLiteMapper mapper = (DefaultHiLiteMapper)m_translator
                .getMapper();
        mapper.save(mapSettings);
        File mappingFile = new File(internDir, HILITE_MAPPING_FILE_NAME);
        mapSettings.saveToXML(new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(mappingFile))));
    }

    /**
     * @return missing replacement function
     */
    public final MissingValueReplacementFunction getMissingFct() {
        return BasisFunctionLearnerTable.MISSINGS[m_missing];
    }

    /**
     * @return the target columns with class info
     */
    public final String[] getTargetColumns() {
        return m_targetColumns;
    }
    
    /**
     * @return the data columns used for training
     */
    public final String[] getDataColumns() {
        return m_dataColumns;
    }

    /**
     * @return <code>true</code> if shrink after commit
     */
    public final boolean isShrinkAfterCommit() {
        return m_shrinkAfterCommit;
    }

    /**
     * @return <code>true</code> if max class coverage
     */
    public final boolean isMaxClassCoverage() {
        return m_maxCoverage;
    }
    
    /**
     * @return the choice of distance function
     */
    public final int getDistance() {
        return m_distance;
    }
    
    /**
     * @return maximum number of epochs to train
     */
    public final int getMaxNrEpochs() {
        return m_maxEpochs;
    }
    
}
