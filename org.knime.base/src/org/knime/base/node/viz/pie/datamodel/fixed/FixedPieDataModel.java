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
 *    13.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel.fixed;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.util.PieColumnFilter;
import org.knime.base.node.viz.pie.util.TooManySectionsException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;



/**
 * This class represents a pie chart which consists of several
 * {@link PieSectionDataModel}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieDataModel extends PieDataModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(FixedPieDataModel.class);
    /**The name of the data file which contains all data in serialized form.*/
    private static final String CFG_DATA_FILE = "dataFile.xml.gz";
    private static final String CFG_DATA = "fixedPieDataModel";
    private static final String CFG_PIE_COL = "pieColumn";
    private static final String CFG_NUMERIC_PIE_COL = "isNumericPieCol";
    private static final String CFG_AGGR_COL = "aggrColumn";
    private static final String CFG_SECTIONS = "sections";
    private static final String CFG_SECTION_COUNT = "sectionCount";
    private static final String CFG_SECTION = "section_";
    private static final String CFG_MISSING_SECTION = "missingSection";
    private static final String CFG_HILITING = "hiliting";
    private static final String CFG_DETAILS = "details";


    private final String m_pieCol;

    private final boolean m_numericPieCol;

    private final String m_aggrCol;

    private final List<PieSectionDataModel> m_sections;

    private final PieSectionDataModel m_missingSection;

    private boolean m_sectionsInitialized = false;


    /**Constructor for class PieDataModel.
     * @param pieColSpec the {@link DataColumnSpec} of the pie column
     * @param aggrColSpec the optional {@link DataColumnSpec} of the
     * aggregation column
     * @param containsColorHandler <code>true</code> if a color handler is set
     */
    public FixedPieDataModel(final DataColumnSpec pieColSpec,
            final DataColumnSpec aggrColSpec,
            final boolean containsColorHandler) {
        super(false, containsColorHandler);
        if (pieColSpec == null) {
            throw new NullPointerException("pieCol, aggrCol must not be null");
        }
        if (aggrColSpec == null) {
            m_aggrCol = null;
        } else {
            m_aggrCol = aggrColSpec.getName();
        }
        m_pieCol = pieColSpec.getName();
        m_numericPieCol = pieColSpec.getType().isCompatible(DoubleValue.class);
        m_missingSection = createDefaultMissingSection(false);
        m_sections = new ArrayList<PieSectionDataModel>();
    }

    /**Constructor for class PieDataModel.
     * @param pieCol the name of the pie column
     * @param numericPieCol <code>true</code> if the pie column is numerical
     * @param aggrCol the name of the aggregation column
     * @param sections the sections
     * @param missingSection the missing section
     * @param supportHiliting if hiliting is supported
     * @param containsColorHandler <code>true</code> if a color handler is set
     */
    protected FixedPieDataModel(final String pieCol,
            final boolean numericPieCol, final String aggrCol,
            final List<PieSectionDataModel> sections,
            final PieSectionDataModel missingSection,
            final boolean supportHiliting, final boolean containsColorHandler) {
        super(supportHiliting, containsColorHandler);
        if (pieCol == null) {
            throw new NullPointerException("pieCol must not be null");
        }
        m_pieCol = pieCol;
        m_numericPieCol = numericPieCol;
        m_aggrCol = aggrCol;
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
        m_sections = sections;
        if (missingSection == null) {
            m_missingSection = createDefaultMissingSection(supportHiliting);
        } else {
            m_missingSection = missingSection;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDataRow(final DataRow row, final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell)
    throws TooManySectionsException {
        if (pieCell == null) {
            throw new NullPointerException(
                    "Pie section value must not be null.");
        }
        PieSectionDataModel section;
        if (pieCell.isMissing()) {
            section = getMissingSection();
        } else {
            section = getSection(pieCell);
            if (section == null) {
                if (m_sections.size()
                        >= PieColumnFilter.MAX_NO_OF_SECTIONS) {
                    throw new TooManySectionsException(
                            "Selected pie column contains more than "
                            + PieColumnFilter.MAX_NO_OF_SECTIONS
                            + " unique values.");
                }
//                throw new IllegalArgumentException("No section found for: "
//                        + pieCell.toString());
                section = new PieSectionDataModel(pieCell.toString(),
                        Color.BLACK, supportsHiliting());
                m_sections.add(section);
            }
            m_sectionsInitialized = false;
        }
        section.addDataRow(rowColor, row.getKey().getId(), aggrCell);
    }

    /**
     * @return the name of the pie column
     */
    public String getPieColName() {
        return m_pieCol;
    }

    /**
     * @return the name of the pie column
     */
    public String getAggrColName() {
        return m_aggrCol;
    }

    /**
     * @param value the value to get the section for
     * @return the{@link PieSectionDataModel} which represents the given value
     * or <code>null</code> if no section is found for the given value
     */
    private PieSectionDataModel getSection(final DataCell value) {
        for (final PieSectionDataModel section : m_sections) {
            if (section.getName().equals(value.toString())) {
                return section;
            }
        }
        return null;
    }

    /**
     * @return the {@link PieSectionDataModel} list
     */
    private List<PieSectionDataModel> getSections()  {
        if (!m_sectionsInitialized) {
            PieDataModel.sortSections(m_sections, m_numericPieCol, true);
            PieDataModel.setSectionColor(m_sections);
            m_sectionsInitialized = true;
        }
        return m_sections;
    }
    /**
     * @return the missing {@link PieSectionDataModel}
     */
    public PieSectionDataModel getMissingSection()  {
        return m_missingSection;
    }

    /**
     * @return a real copy of all sections
     */
    @SuppressWarnings("unchecked")
    public List<PieSectionDataModel> getClonedSections()  {
        LOGGER.debug("Entering getClonedSections() of class "
                + "FixedPieDataModel.");
        final long startTime = System.currentTimeMillis();
        List<PieSectionDataModel> binClones = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(getSections());
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            binClones = (List<PieSectionDataModel>)
            new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
            final String msg = "Exception while cloning pie sections: "
                + e.getMessage();
              LOGGER.debug(msg);
              throw new IllegalStateException(msg);
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting getClonedSections() of class "
                + "FixedPieDataModel.");
        return binClones;
    }

    /**
     * @return a real copy of the missing section
     */
    public PieSectionDataModel getClonedMissingSection() {
        LOGGER.debug("Entering getClonedMissingSection() of class "
                + "FixedPieDataModel.");
        final long startTime = System.currentTimeMillis();
        PieSectionDataModel missingClone = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(m_missingSection);
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            missingClone =
                (PieSectionDataModel)new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
            final String msg = "Exception while cloning pie sections: "
                + e.getMessage();
              LOGGER.debug(msg);
              throw new IllegalStateException(msg);
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting getClonedMissingSection() of class "
                + "FixedPieDataModel.");
        return missingClone;
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation was canceled
     */
    public void save2File(final File directory, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        if (exec != null) {
            exec.setProgress(0.0, "Start saving histogram data model to file");
        }
        final File dataFile = new File(directory, CFG_DATA_FILE);
        final FileOutputStream os = new FileOutputStream(dataFile);
        final GZIPOutputStream dataOS = new GZIPOutputStream(os);
        final Config config = new NodeSettings(CFG_DATA);
        config.addString(CFG_PIE_COL, m_pieCol);
        config.addBoolean(CFG_NUMERIC_PIE_COL, m_numericPieCol);
        config.addString(CFG_AGGR_COL, m_aggrCol);
        config.addBoolean(CFG_HILITING, supportsHiliting());
        config.addBoolean(CFG_DETAILS, detailsAvailable());
        if (exec != null) {
            exec.setProgress(0.3, "Start saving sections...");
            exec.checkCanceled();
        }
        final Config sectionsConf = config.addConfig(CFG_SECTIONS);
        sectionsConf.addInt(CFG_SECTION_COUNT , m_sections.size());
        int idx = 0;
        for (final PieSectionDataModel section : m_sections) {
            final ConfigWO sectionConf =
                sectionsConf.addConfig(CFG_SECTION + idx++);
            section.save2File(sectionConf, exec);
        }
        if (exec != null) {
            exec.setProgress(0.8, "Start saving missing section...");
            exec.checkCanceled();
        }
        final ConfigWO missingSection =
            sectionsConf.addConfig(CFG_MISSING_SECTION);
        m_missingSection.save2File(missingSection, exec);
        config.saveToXML(dataOS);
        dataOS.flush();
        dataOS.close();
        os.flush();
        os.close();
        if (exec != null) {
            exec.setProgress(1.0, "Pie data model saved");
        }
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the data model
     * wasn't valid
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation was canceled
     * @throws InvalidSettingsException if the file is invalid
     */
    @SuppressWarnings("unchecked")
    public static FixedPieDataModel loadFromFile(final File directory,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException,
            InvalidSettingsException {
        if (exec != null) {
            exec.setProgress(0.0, "Start reading data from file");
        }
        final File settingsFile = new File(directory, CFG_DATA_FILE);
        final FileInputStream is = new FileInputStream(settingsFile);
        final GZIPInputStream inData = new GZIPInputStream(is);
        final ConfigRO config = NodeSettings.loadFromXML(inData);
        final String pieCol = config.getString(CFG_PIE_COL);
        final boolean numericPieCol = config.getBoolean(CFG_NUMERIC_PIE_COL);
        final String aggrCol = config.getString(CFG_AGGR_COL);
        final boolean supportHiliting = config.getBoolean(CFG_HILITING);
        final boolean detailsAvailable = config.getBoolean(CFG_DETAILS);
        if (exec != null) {
            exec.setProgress(0.3, "Loading sections...");
            exec.checkCanceled();
        }
        final Config sectionsConf = config.getConfig(CFG_SECTIONS);
        final int counter = sectionsConf.getInt(CFG_SECTION_COUNT);
        final List<PieSectionDataModel> sections =
            new ArrayList<PieSectionDataModel>(counter);
        for (int i = 0; i < counter; i++) {
            final Config sectionConf = sectionsConf.getConfig(CFG_SECTION + i);
            sections.add(PieSectionDataModel.loadFromFile(sectionConf, exec));
        }

        if (exec != null) {
            exec.setProgress(0.9, "Loading missing section...");
            exec.checkCanceled();
        }
        final Config missingConf = sectionsConf.getConfig(CFG_MISSING_SECTION);
        final PieSectionDataModel missingSection =
            PieSectionDataModel.loadFromFile(missingConf, exec);

        if (exec != null) {
            exec.setProgress(1.0, "Pie data model loaded ");
        }
        //close the stream
        inData.close();
        is.close();
        return new FixedPieDataModel(pieCol, numericPieCol, aggrCol, sections,
                missingSection, supportHiliting, detailsAvailable);
    }
}
