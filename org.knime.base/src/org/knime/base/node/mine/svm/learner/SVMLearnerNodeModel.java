/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   27.09.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Future;

import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.util.BinarySvmRunnable;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.ThreadPool;


/**
 * 
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeModel extends NodeModel {

    /**
     * Key to store the parameter c in the NodeSettings.
     */
    public static final String CFG_PARAMC = "c_parameter";

    /**
     * Key to store the class column in the NodeSettings.
     */
    public static final String CFG_CLASSCOL = "classcol";
    
    /**
     * Key to store kernel parameters in the NodeSettings ATTENTION: this key
     * name is used together with an index. So the i'th parameter will be in
     * KEY_KERNELPARAM + i.toString()
     */
    public static final String CFG_KERNELPARAM = "kernel_param";

    /**
     * Key to store the kernel type in the NodeSettings.
     */
    public static final String CFG_KERNELTYPE = "kernel_type";
    
    /** Keys under which to save the parameters. */
    public static final String KEY_CATEG_COUNT = "Category count"; 
    
    /** key to save the DataTableSpec .*/
    public static final String KEY_SPEC = "DataTableSpec"; 
    
    /** Key to save the DataTableSpec .*/
    public static final String KEY_CLASSCOL = "classcol"; 
   

    /** Default c parameter. */
    public static final double DEFAULT_PARAMC = 1.0;

    /*
     * The c parameter value.
     */
    private SettingsModelDouble m_paramC =
            new SettingsModelDouble(CFG_PARAMC, DEFAULT_PARAMC);
    
    /*
     * Class column
     */
    private SettingsModelString m_classcol =
            new SettingsModelString(CFG_CLASSCOL, "");
    
    /*
     * Position of class column
     */
    private int m_classpos;
    
    /*
     * The chosen kernel
     */
    private String m_kernelType = KernelFactory.getDefaultKernelType();

    private HashMap<String, Vector<SettingsModelDouble>> m_kernelParameters;

    /*
     * For each category, a BinarySvm that splits the category from the others.
     */
    private Svm[] m_svms;
    
    /*
     * The DataTableSpec we have learned with
     */
    private DataTableSpec m_spec;
    
    /*
     * String containing info about the trained SVM's
     */
    private String m_svmInfo = "";
    
   /**
    * creates the kernel parameter SettingsModels.
    * @return HashMap containing the kernel and its assigned SettingsModels.
    */
    static HashMap<String, Vector<SettingsModelDouble>>
                    createKernelParams() {
        HashMap<String, Vector<SettingsModelDouble>> kernelParameters =
                new HashMap<String, Vector<SettingsModelDouble>>();
        for (String kernelname : KernelFactory.getKernelNames()) {
            Kernel kernel = KernelFactory.getKernel(kernelname);
            Vector<SettingsModelDouble> settings =
                    new Vector<SettingsModelDouble>();
            for (int i = 0; i < kernel.getNumberParameters(); i++) {
                settings.add(new SettingsModelDouble(CFG_KERNELPARAM + "_"
                        + kernel.getParameterName(i), kernel
                        .getDefaultParameter(i)));
            }
            kernelParameters.put(kernelname, settings);
        }
        return kernelParameters;
    }

    /**
     * 
     */
    public SVMLearnerNodeModel() {
        super(1, 0, 0, 1);
        m_kernelParameters = createKernelParams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec myspec = inSpecs[0];
        if (myspec.getNumColumns() > 0) {
            if (m_classcol.getStringValue().equals("")) {
                throw new InvalidSettingsException("Class column not set");
            } else {
                boolean found = false;
                for (DataColumnSpec colspec : myspec) {
                    if (colspec.getName().equals(m_classcol.getStringValue())) {
                        found = true;
                        if (!colspec.getType().isCompatible(
                                NominalValue.class)) {
                            throw new InvalidSettingsException("Target column "
                                    + colspec.getName() + " must be nominal.");
                        }
                        m_classpos =
                                myspec.findColumnIndex(m_classcol
                                        .getStringValue());
                    } else {
                        if (colspec.getType().isCompatible(StringValue.class)) {
                            throw new InvalidSettingsException(
                                    "Unknown String column "
                                            + colspec.getName()
                                            + " (is not class column)");
                        }
                    }
                }
                if (!found) {
                    throw new InvalidSettingsException("Class column "
                            + m_classcol.getStringValue() + " not found"
                            + " in DataTableSpec.");
                }
            }
        }
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_spec = inData[0].getDataTableSpec();
        m_classpos = m_spec.findColumnIndex(m_classcol
                                    .getStringValue());
        // convert input data
        ArrayList<DoubleVector> inputData = new ArrayList<DoubleVector>();
        ArrayList<String> categories = new ArrayList<String>();
        StringCell stringcell = null;
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            ArrayList<Double> values = new ArrayList<Double>();
            boolean add = true;
            for (int i = 0; i < row.getNumCells(); i++) {
                if (row.getCell(i).isMissing()) {
                    add = false;
                    break;
                }
                if (i != m_classpos) {
                    DoubleValue cell = (DoubleValue)row.getCell(i);
                    values.add(cell.getDoubleValue());
                } else {
                    stringcell = (StringCell)row.getCell(m_classpos);
                    if (!categories.contains(stringcell.getStringValue())) {
                        categories.add(stringcell.getStringValue());
                    }
                }
            }
            if (add) {
                inputData.add(new DoubleVector(values, stringcell
                        .getStringValue()));
            }
        }
        DoubleVector[] inputDataArr = new DoubleVector[inputData.size()];
        inputDataArr = inputData.toArray(inputDataArr);
        Kernel kernel = KernelFactory.getKernel(m_kernelType);
        Vector<SettingsModelDouble> kernelparams =
                m_kernelParameters.get(m_kernelType);
        for (int i = 0; i < kernel.getNumberParameters(); ++i) {
            kernel.setParameter(i, kernelparams.get(i).getDoubleValue());
        }

        m_svms = new Svm[categories.size()];
        exec.setMessage("Training SVM");
        final BinarySvmRunnable[] bst =
                new BinarySvmRunnable[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            bst[i] =
                    new BinarySvmRunnable(inputDataArr, categories.get(i),
                            kernel, m_paramC.getDoubleValue(),
                            exec.createSubProgress((1.0 / categories.size())));

        }
        ThreadPool pool = KNIMEConstants.GLOBAL_THREAD_POOL;
        final Future<?>[] fut = new Future<?>[bst.length];
        KNIMETimer timer = KNIMETimer.getInstance();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    exec.checkCanceled();
                } catch (final CanceledExecutionException ce) {
                    for (int i = 0; i < fut.length; i++) {
                        if (fut[i] != null) {
                            fut[i].cancel(true);
                        }
                    }
                    super.cancel();
                }

            }
        }, 0, 3000);
        for (int i = 0; i < bst.length; i++) {
            fut[i] = pool.enqueue(bst[i]);
        }

        
        boolean alldone = false;
        while (!alldone) {
            alldone = true;
            for (int i = 0; i < fut.length; ++i) {
                if (!fut[i].isDone()) {
                    alldone = false;
                } else {
                    bst[i].ok();
                    m_svms[i] = bst[i].getSvm();
                }
            }
        }
        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "SVM_Internals");
        FileReader in = new FileReader(f);
        StringBuffer sb = new StringBuffer();
        char c;
        while (in.ready()) {
            c = (char)in.read(); 
            sb.append(c);
        }
        m_svmInfo = sb.toString();
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_paramC.loadSettingsFrom(settings);
        m_classcol.loadSettingsFrom(settings);
        if (settings.containsKey(CFG_KERNELTYPE)) {
            m_kernelType = settings.getString(CFG_KERNELTYPE);
        }
        for (Map.Entry<String, Vector<SettingsModelDouble>> entry 
                            : m_kernelParameters.entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                smd.loadSettingsFrom(settings);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_svms = null;
        m_svmInfo = "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "SVM_Internals");
        FileWriter out = new FileWriter(f);
        out.write(m_svmInfo);
        out.close();
    }
    
    /**
     * @return a string containing all SVM infos in HTML for the view.
     */
    String getSVMInfos() {
        if (m_svmInfo.length() > 0) {
            return m_svmInfo;
        }
        StringBuffer sb = new StringBuffer();
        if (m_svms != null) {
            sb.append("<html>\n");
            sb.append("<body>\n");
            for (int i = 0; i < m_svms.length; i++) {
                if (m_svms[i] != null) {
                    sb.append("<h1> SVM " + i + " Class: "
                            + m_svms[i].getPositive() + "</h1>");
                    sb.append("<b> Support Vectors: </b><br>");
                    DoubleVector[] supvecs = m_svms[i].getSupportVectors();
                    for (DoubleVector vec : supvecs) {
                        for (int s = 0; s < vec.getNumberValues(); s++) {
                            sb.append(vec.getValue(s) + ", ");
                        }
                        sb.append(vec.getClassValue() + "<br>");
                    }
                }
            }
            sb.append("</body>\n");
            sb.append("</html>\n");
        }
        m_svmInfo = sb.toString();
        return m_svmInfo;
    }
    
     /**
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert index == 0;
        predParams.addInt(KEY_CATEG_COUNT, m_svms.length);
        for (int i = 0; i < m_svms.length; ++i) {
            m_svms[i].saveToPredictorParams(predParams, 
                                        new Integer(i).toString() + "SVM");
        }
        Config specconf = predParams.addConfig(KEY_SPEC);
        m_spec.save(specconf);
        predParams.addString(KEY_CLASSCOL, m_classcol.getStringValue());
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KERNELTYPE, m_kernelType);
        m_paramC.saveSettingsTo(settings);
        m_classcol.saveSettingsTo(settings);
        for (Map.Entry<String, Vector<SettingsModelDouble>> entry 
                : m_kernelParameters
                .entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                smd.saveSettingsTo(settings);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey(CFG_KERNELTYPE)) {
            String tmpKernel = settings.getString(CFG_KERNELTYPE);
            boolean found = false;
            for (String kernel : KernelFactory.getKernelNames()) {
                if (tmpKernel.equals(kernel)) {
                    found = true;
                }
            }
            if (!found) {
                throw new InvalidSettingsException("Unknown kernel type: "
                        + tmpKernel);
            }

        }
        for (Map.Entry<String, Vector<SettingsModelDouble>> entry 
                : m_kernelParameters.entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                smd.validateSettings(settings);
            }
        }
        m_paramC.validateSettings(settings);
        m_classcol.validateSettings(settings);
    }
}
