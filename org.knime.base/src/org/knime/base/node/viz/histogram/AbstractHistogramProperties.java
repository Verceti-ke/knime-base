/*
 * ------------------------------------------------------------------
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
 *   18.08.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;

/**
 * Abstract class which handles the default properties like x column selection.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramProperties extends
        AbstractPlotterProperties {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractHistogramProperties.class);

    private static final String BIN_TAB_LABEL = "Bin settings";

    private static final String AGGREGATION_TAB_LABEL = 
        "Aggregation settings";

    private static final String VIZ_SETTINGS_TAB_LABEL = 
        "Visualization settings";

    private static final String DETAILS_TAB_LABEL = 
        "Details";

    private static final String AGGREGATION_METHOD_LABEL = 
        "Aggregation method:";

    private static final String BIN_SIZE_LABEL = "Bin size:";

    private static final String BIN_WIDTH_TOOLTIP = "Width of the bins";

    private static final String NUMBER_OF_BINS_LABEL = "Number of bins:";

    private static final String NO_OF_BINS_TOOLTIP = 
        "Number of bins (incl. empty bins, excl. missing value bin)";

    private static final String SHOW_MISSING_VALUE_BIN_LABEL = 
        "Show missing value bin";

    private static final String SHOW_MISSING_VAL_BIN_TOOLTIP = "Shows a bin "
            + "with rows which have a missing value for the selected x column.";

    private static final String SHOW_EMPTY_BINS_LABEL = "Show empty bins";

    private static final String SHOW_GRID_LABEL = "Show grid lines";

    private static final String SHOW_BIN_OUTLINE_LABEL = "Show bin outline";
    
    private static final String SHOW_BAR_OUTLINE_LABEL = "Show bar outline";
    
    private static final String SHOW_ELEMENT_OUTLINE_LABEL = 
        "Show element outline";
    
    private static final String AGGR_METHOD_DISABLED_TOOLTIP = 
        "Only available with aggregation column";

//    private static final String APPLY_BUTTON_LABEL = "Apply";

    private static final Dimension HORIZONTAL_SPACER_DIM = new Dimension(10, 1);

    private final JSlider m_binWidth;

    private final JSlider m_noOfBins;

    private final JLabel m_noOfBinsLabel;

    private final ButtonGroup m_aggrMethButtonGrp;

    private final JCheckBox m_showEmptyBins;

    private final JCheckBox m_showMissingValBin;
    
    private final JPanel m_detailsPane;
    
    private final JScrollPane m_detailsScrollPane;
    
    private final JEditorPane m_detailsHtmlPane;

//    private final JButton m_applyAggrSettingsButton;

//    private final JButton m_applyBarSettingsButton;

    private final JCheckBox m_showGrid;

    private final JCheckBox m_showBinOutline;
    
    private final JCheckBox m_showBarOutline;
    
    private final JCheckBox m_showElementOutline;

    private final ButtonGroup m_labelDisplayPolicy;

    private final ButtonGroup m_labelOrientation;

    private final ButtonGroup m_layoutDisplayPolicy;

    private static final String LABEL_ORIENTATION_LABEL = "Orientation:";

    private static final String LABEL_ORIENTATION_VERTICAL = "Vertical";

    private static final String LABEL_ORIENTATION_HORIZONTAL = "Horizontal";

    /**
     * Constructor for class AbstractHistogramProperties.
     * 
     * @param tableSpec the {@link DataTableSpec} to initialize the column
     * @param vizModel the aggregation method to set
     * selection boxes
     */
    public AbstractHistogramProperties(final DataTableSpec tableSpec, 
            final AbstractHistogramVizModel vizModel) {
        if (vizModel == null) {
            throw new IllegalArgumentException("VizModel shouldn't be null");
        }
        if (tableSpec == null) {
            throw new IllegalArgumentException("TableSpec shouldn't be null");
        }
     
        // create the additional settings components which get added to the
        // histogram settings panel
        m_binWidth = new JSlider(0, vizModel.getBinWidth(), 
                vizModel.getBinWidth());
        m_binWidth.setEnabled(false);
        m_noOfBins = new JSlider(1, 1, 1);
        m_noOfBinsLabel = new JLabel();
        m_noOfBins.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                updateNoOfBinsText(source.getValue());
            }
        });
        m_noOfBins.setEnabled(false);

        // set the aggregation method radio buttons
        final JRadioButton countMethod = new JRadioButton(
                AggregationMethod.COUNT.name());
        countMethod.setActionCommand(AggregationMethod.COUNT.name());
        countMethod.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSelectAggrMethod(e.getActionCommand());
            }
        });
        final JRadioButton sumMethod = new JRadioButton(AggregationMethod.SUM
                .name());
        sumMethod.setActionCommand(AggregationMethod.SUM.name());
        sumMethod.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSelectAggrMethod(e.getActionCommand());
            }
        });
        final JRadioButton avgMethod = new JRadioButton(
                AggregationMethod.AVERAGE.name());
        avgMethod.setActionCommand(AggregationMethod.AVERAGE.name());
        avgMethod.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSelectAggrMethod(e.getActionCommand());
            }
        });
        // Group the radio buttons.
        m_aggrMethButtonGrp = new ButtonGroup();
        m_aggrMethButtonGrp.add(countMethod);
        m_aggrMethButtonGrp.add(sumMethod);
        m_aggrMethButtonGrp.add(avgMethod);

        // select the right radio button
        for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(
                    vizModel.getAggregationMethod().name())) {
                button.setSelected(true);
            }
        }
        m_showEmptyBins = new JCheckBox(SHOW_EMPTY_BINS_LABEL, 
                vizModel.isShowEmptyBins());
        m_showMissingValBin = new JCheckBox(SHOW_MISSING_VALUE_BIN_LABEL, 
                vizModel.isShowEmptyBins());
        m_showMissingValBin.setToolTipText(SHOW_MISSING_VAL_BIN_TOOLTIP);
//        m_applyAggrSettingsButton = new JButton(
//                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
//        m_applyAggrSettingsButton.setHorizontalAlignment(
//        SwingConstants.RIGHT);
//
//        m_applyBarSettingsButton = new JButton(
//                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
//        m_applyBarSettingsButton.setHorizontalAlignment(SwingConstants.RIGHT);
        // create the visualization option elements
        m_showGrid = new JCheckBox(SHOW_GRID_LABEL, vizModel.isShowGridLines());
        m_showBinOutline = new JCheckBox(SHOW_BIN_OUTLINE_LABEL, true);
        m_showBarOutline = new JCheckBox(SHOW_BAR_OUTLINE_LABEL, true);
        m_showElementOutline = new JCheckBox(SHOW_ELEMENT_OUTLINE_LABEL, true);

        m_labelDisplayPolicy = AbstractHistogramProperties
                .createEnumButtonGroup(LabelDisplayPolicy.values());

        m_labelOrientation = new ButtonGroup();
        final JRadioButton labelVertical = new JRadioButton(
                LABEL_ORIENTATION_VERTICAL);
        labelVertical.setActionCommand(LABEL_ORIENTATION_VERTICAL);
        labelVertical.setSelected(true);
        m_labelOrientation.add(labelVertical);
        final JRadioButton labelHorizontal = new JRadioButton(
                LABEL_ORIENTATION_HORIZONTAL);
        labelHorizontal.setActionCommand(LABEL_ORIENTATION_HORIZONTAL);
        m_labelOrientation.add(labelHorizontal);

        m_layoutDisplayPolicy = AbstractHistogramProperties
                .createEnumButtonGroup(HistogramLayout.values());
        // The bin settings tab
        final JPanel binPanel = createBinSettingsPanel();
        addTab(BIN_TAB_LABEL, binPanel);
        // the aggregation settings tab
        final JPanel aggrPanel = createAggregationSettingsPanel();
        addTab(AGGREGATION_TAB_LABEL, aggrPanel);

        final JPanel visOptionPanel = createVizSettingsPanel();
        addTab(VIZ_SETTINGS_TAB_LABEL, visOptionPanel);
        
        //create the details panel
        m_detailsHtmlPane = new JEditorPane("text/html", "");
        //I have to subtract the tab height from the preferred size
        Dimension tabSize = getTabSize();
        if (tabSize == null) {
            tabSize = new Dimension(1, 1);
        }
        m_detailsHtmlPane.setText("");
        m_detailsHtmlPane.setEditable(false);
        m_detailsHtmlPane.setBackground(getBackground());
        m_detailsScrollPane = new JScrollPane(m_detailsHtmlPane);
        m_detailsScrollPane.setPreferredSize(tabSize);
        m_detailsPane = new JPanel();
        m_detailsPane.add(m_detailsScrollPane);
        addTab(DETAILS_TAB_LABEL, m_detailsPane);
    }

    private Dimension getTabSize() {
        try {
            final Dimension totalSize = getPreferredSize();
            return new Dimension((int)totalSize.getWidth(), 
                    (int) totalSize.getHeight() - 10);
        } catch (Exception e) {
            LOGGER.debug("Exception in getTabSize: " + e.getMessage());
        }
        return null;
    }
//
//    /**
//     * This method is called to resize the tabs. 
//     */
//    public void resize() {
//        final Dimension tabSize = getTabSize();
//        if (tabSize != null) {
//            m_detailsScrollPane.setPreferredSize(tabSize);
//        }
//    }
    
    /**
     * @param html the new details view
     */
    protected void updateHTMLDetailsPanel(final String html) {
        m_detailsHtmlPane.setText(html);
    }

    /**
     * @param props the properties to generate the buttons for
     * @return the {@link ButtonGroup} with all properties as button
     */
    private static ButtonGroup createEnumButtonGroup(
            final HistogramProperty[] props) {
        final ButtonGroup group = new ButtonGroup();
        for (final HistogramProperty property : props) {
            final JRadioButton button = new JRadioButton(property.getLabel());
            button.setActionCommand(property.getID());
            button.setSelected(property.isDefault());
            group.add(button);
        }
        return group;
    }

    /**
     * The visualization panel with the following options:
     * <ol>
     * <li>Show grid line</li>
     * <li>Show bar outline</li>
     * </ol>.
     * 
     * @return the visualization settings panel
     */
    private JPanel createVizSettingsPanel() {
        final JPanel vizPanel = new JPanel();
//visualisation box
        final Box vizBoxLeft = Box.createVerticalBox();
        vizBoxLeft.add(Box.createVerticalGlue());
        vizBoxLeft.add(m_showGrid);
        vizBoxLeft.add(Box.createVerticalGlue());
        final Box vizBoxRight = Box.createVerticalBox();
        vizBoxRight.add(Box.createVerticalGlue());
        vizBoxRight.add(m_showBinOutline);
        vizBoxRight.add(Box.createVerticalGlue());
        vizBoxRight.add(m_showBarOutline);
        vizBoxRight.add(Box.createVerticalGlue());
        vizBoxRight.add(Box.createVerticalGlue());
        vizBoxRight.add(m_showElementOutline);
        final Box vizBox = Box.createHorizontalBox();
        vizBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Display option"));
        vizBox.add(Box.createHorizontalGlue());
        vizBox.add(vizBoxLeft);
        vizBox.add(Box.createHorizontalGlue());
        vizBox.add(vizBoxRight);
        vizBox.add(Box.createHorizontalGlue());
//        vizBox.add(Box.createVerticalGlue());
//        vizBox.add(m_showGrid);
//        vizBox.add(Box.createVerticalGlue());
//        vizBox.add(m_showBinOutline);
//        vizBox.add(Box.createVerticalGlue());
//        vizBox.add(m_showBarOutline);
//        vizBox.add(Box.createVerticalGlue());
//        vizBox.add(Box.createVerticalGlue());
//        vizBox.add(m_showElementOutline);
        
        
//label layout box  
        final Box labelBox = Box.createHorizontalBox();
        labelBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Labels"));
        labelBox.add(Box.createHorizontalGlue());
        final Box labelDisplayBox = AbstractHistogramProperties
                .createButtonGroupBox(m_labelDisplayPolicy, true, null);
        labelBox.add(labelDisplayBox);
        labelBox.add(Box.createHorizontalGlue());
        final Box labelOrientationBox = AbstractHistogramProperties
                .createButtonGroupBox(m_labelOrientation, true,
                        LABEL_ORIENTATION_LABEL);
        labelBox.add(labelOrientationBox);
        labelBox.add(Box.createHorizontalGlue());
        
//bar layout box                
        final Box layoutDisplayBox = AbstractHistogramProperties
        .createButtonGroupBox(m_layoutDisplayPolicy, false, null);
        final Box binWidthBox = Box.createVerticalBox();
        // barWidthBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        final Box binWidthLabelBox = Box.createHorizontalBox();
        final JLabel binWidthLabel = new JLabel(
                AbstractHistogramProperties.BIN_SIZE_LABEL);
        binWidthLabelBox.add(Box.createHorizontalGlue());
        binWidthLabelBox.add(binWidthLabel);
        binWidthLabelBox.add(Box.createHorizontalGlue());
        binWidthBox.add(binWidthLabelBox);
        // the bin width slider box
        final Box binWidthSliderBox = Box.createHorizontalBox();
        binWidthSliderBox.add(Box.createHorizontalGlue());
        binWidthSliderBox.add(m_binWidth);
        binWidthSliderBox.add(Box.createHorizontalGlue());
        binWidthBox.add(binWidthSliderBox);
        final Box barLayoutBox = Box.createVerticalBox();
        barLayoutBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Bar Layout"));
        barLayoutBox.add(Box.createVerticalGlue());
        barLayoutBox.add(binWidthBox);
        barLayoutBox.add(Box.createVerticalGlue());
        barLayoutBox.add(layoutDisplayBox);
        barLayoutBox.add(Box.createVerticalGlue());

        final Box rootBox = Box.createHorizontalBox();
        rootBox.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        rootBox.add(vizBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(labelBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(barLayoutBox);
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        vizPanel.add(rootBox);
        return vizPanel;
    }


    /**
     * The bar aggregation settings:
     * <ol>
     * <li>aggregation method</li>
     * <li>aggregation column</li>
     * </ol>.
     * 
     * @return the aggregation settings panel
     */
    private JPanel createAggregationSettingsPanel() {
        final JPanel aggrPanel = new JPanel();
        // the aggregation method label box
        final Box aggrLabelButtonBox = Box.createVerticalBox();
        final Box aggrLabelBox = Box.createHorizontalBox();
        final JLabel aggrMethLabel = new JLabel(
                AbstractHistogramProperties.AGGREGATION_METHOD_LABEL);
        aggrMethLabel.setVerticalAlignment(SwingConstants.CENTER);
        aggrLabelBox.add(Box.createHorizontalGlue());
        aggrLabelBox.add(aggrMethLabel);
        aggrLabelBox.add(Box.createHorizontalGlue());
        aggrLabelButtonBox.add(aggrLabelBox);
        // the aggregation method radio button box
        final Box aggrButtonBox = AbstractHistogramProperties
                .createButtonGroupBox(m_aggrMethButtonGrp, false, null);
        aggrLabelButtonBox.add(aggrButtonBox);
        final Box aggrBox = Box.createHorizontalBox();
        aggrBox
                .setBorder(BorderFactory
                        .createEtchedBorder(EtchedBorder.RAISED));
        aggrBox.add(aggrLabelButtonBox);
        aggrBox.add(Box.createHorizontalGlue());
        aggrBox.add(Box.createHorizontalGlue());
        aggrPanel.add(aggrBox);
        return aggrPanel;
    }

    /**
     * The bin related settings:
     * <ol>
     * <li>size</li>
     * <li>number of bins</li>
     * <li>show empty bins</li>
     * <li>show missing value bin</li>
     * </ol>.
     * 
     * @return the panel with all bar related settings
     */
    private JPanel createBinSettingsPanel() {
        final JPanel binPanel = new JPanel();
        final Box binNoBox = Box.createVerticalBox();
        // barNoBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        // the number of bars label box
        final Box noOfBinsLabelBox = Box.createHorizontalBox();
        final JLabel noOfBinsLabel = new JLabel(
                AbstractHistogramProperties.NUMBER_OF_BINS_LABEL);
        noOfBinsLabelBox.add(Box.createHorizontalGlue());
        noOfBinsLabelBox.add(noOfBinsLabel);
        noOfBinsLabelBox.add(Box.createHorizontalStrut(10));
        noOfBinsLabelBox.add(m_noOfBinsLabel);
        noOfBinsLabelBox.add(Box.createHorizontalGlue());
        binNoBox.add(noOfBinsLabelBox);
        // the number of bins slider box
        final Box noOfBinsSliderBox = Box.createHorizontalBox();
        noOfBinsSliderBox.add(Box.createHorizontalGlue());
        noOfBinsSliderBox.add(m_noOfBins);
        noOfBinsSliderBox.add(Box.createHorizontalGlue());
        binNoBox.add(noOfBinsSliderBox);
        // the box with the select boxes and apply button
        final Box binSelButtonBox = Box.createVerticalBox();
        // barSelButtonBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        final Box binSelectBox = Box.createVerticalBox();
        // barSelectBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        binSelectBox.add(m_showEmptyBins);
        binSelectBox.add(Box.createVerticalGlue());
        binSelectBox.add(m_showMissingValBin);
        binSelectBox.add(Box.createVerticalGlue());
        binSelButtonBox.add(binSelectBox);
        binSelButtonBox.add(Box.createVerticalGlue());
//        final Box buttonBox = Box.createHorizontalBox();
//        // buttonBox.setBorder(BorderFactory
//        // .createEtchedBorder(EtchedBorder.RAISED));
//        final Dimension d = new Dimension(75, 1);
//        buttonBox.add(Box.createRigidArea(d));
//        buttonBox.add(m_applyBarSettingsButton);
//        barSelButtonBox.add(buttonBox);
        final Box binBox = Box.createHorizontalBox();
        binBox.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        binBox.add(Box.createHorizontalGlue());
        binBox.add(binNoBox);
        binBox.add(Box.createHorizontalGlue());
        binBox.add(binSelButtonBox);
        binPanel.add(binBox);
        return binPanel;
    }

  private static Box createButtonGroupBox(final ButtonGroup group,
            final boolean vertical, final String label) {
        Box buttonBox = null;
        if (vertical) {
            buttonBox = Box.createVerticalBox();
            buttonBox.add(Box.createVerticalGlue());
            if (label != null) {
                buttonBox.add(new JLabel(label));
                buttonBox.add(Box.createVerticalGlue());
            }
        } else {
            buttonBox = Box.createHorizontalBox();
            buttonBox.add(Box.createHorizontalGlue());
            if (label != null) {
                buttonBox.add(new JLabel(label));
                buttonBox.add(Box.createHorizontalGlue());
            }
        }

        for (final Enumeration<AbstractButton> buttons = group.getElements(); 
            buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            buttonBox.add(button);
            if (vertical) {
                buttonBox.add(Box.createVerticalGlue());
            } else {
                buttonBox.add(Box.createHorizontalGlue());
            }
        }
        return buttonBox;
    }

    /**
     * Sets the label of the given slider.
     * 
     * @param slider the slider to label
     * @param divisor the steps are calculated
     *            <code>maxVal - minVal / divisor</code>
     */
    private static void setSliderLabels(final JSlider slider,
            final int divisor, final boolean showDigitsAndTicks) {
        // show at least the min, middle and max value on the slider.
        final int minimum = slider.getMinimum();
        final int maximum = slider.getMaximum();
        final int increment = (maximum - minimum) / divisor;
        if (increment < 1) {
            // if their is no increment we don't need to enable this slider
            // Hashtable labels = m_barWidth.createStandardLabels(1);
            final Hashtable<Integer, JLabel> labels = 
                new Hashtable<Integer, JLabel>(1);
            labels.put(minimum, new JLabel("Min"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(false);
        } else if (showDigitsAndTicks) {
            // slider.setLabelTable(slider.createStandardLabels(increment));
            final Hashtable<Integer, JLabel> labels = 
                new Hashtable<Integer, JLabel>();
            // labels.put(minimum, new JLabel("Min"));
            labels.put(minimum, new JLabel(Integer.toString(minimum)));
            for (int i = 1; i < divisor; i++) {
                final int value = minimum + i * increment;
                labels.put(value, new JLabel(Integer.toString(value)));
            }
            // labels.put(maximum, new JLabel("Max"));
            labels.put(maximum, new JLabel(Integer.toString(maximum)));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setMajorTickSpacing(divisor);
            slider.setPaintTicks(true);
            // slider.setSnapToTicks(true);
            slider.setEnabled(true);
        } else {
            final Hashtable<Integer, JLabel> labels = 
                new Hashtable<Integer, JLabel>();
            labels.put(minimum, new JLabel("Min"));
            labels.put(maximum, new JLabel("Max"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(true);
        }
    }

    /**
     * 
     * @param spec current data table specification
     * @param xColName preselected x column name
     * @param yColumns preselected y column names
     * @param aggrMethod the current {@link AggregationMethod}
     */
    public abstract void updateColumnSelection(final DataTableSpec spec,
            final String xColName, final Collection<ColorColumn> yColumns,
            final AggregationMethod aggrMethod);

    /**
     * Updates the available slider with the current values of the Histogram
     * plotter.
     * 
     * @param vizModel the {@link AbstractHistogramVizModel} object which
     *            contains the data
     */
    public void updateHistogramSettings(
            final AbstractHistogramVizModel vizModel) {
        LOGGER.debug("Entering updateHistogramSettings(vizModel) "
                + "of class AbstractHistogramProperties.");
        final long startTime = System.currentTimeMillis();
        if (vizModel == null) {
            return;
        }
//update the bin settings tab components
        //update the number of bin values
        int maxNoOfBins = vizModel.getMaxNoOfBins();
        final int currentNoOfBins = vizModel.getNoOfBins();
        final ChangeListener[] noOfListeners = m_noOfBins.getChangeListeners();
        LOGGER.debug("No of noOfBins listener: " + noOfListeners.length);
        for (ChangeListener listener : noOfListeners) {
            m_noOfBins.removeChangeListener(listener);
        }
        m_noOfBins.setMaximum(maxNoOfBins);
        m_noOfBins.setValue(currentNoOfBins);
        m_noOfBinsLabel.setText(Integer.toString(currentNoOfBins));
        AbstractHistogramProperties.setSliderLabels(m_noOfBins, 2, true);
        // disable this noOfbins slider for nominal values
        if (vizModel.isBinNominal() || vizModel.isFixed()) {
            m_noOfBins.setEnabled(false);
            if (vizModel.isFixed()) {
                m_noOfBins.setToolTipText("Not available for "
                        + "this histogram implementation");
            } else {
                m_noOfBins
                    .setToolTipText("Only available for numerical properties");
            }
        } else {
            m_noOfBins.setEnabled(true);
            m_noOfBins.setToolTipText(
                    AbstractHistogramProperties.NO_OF_BINS_TOOLTIP);
        }
        for (ChangeListener listener : noOfListeners) {
            m_noOfBins.addChangeListener(listener);
        }
        LOGGER.debug("No of bins updated");
        //show empty bins box
        updateCheckBox(m_showEmptyBins, vizModel.isShowEmptyBins(),
                vizModel.containsEmptyBins());
        LOGGER.debug("Show empty bins updated");
        //show missing value bin box
        updateCheckBox(m_showMissingValBin, vizModel.isShowMissingValBin(), 
                vizModel.containsMissingValueBin());
        LOGGER.debug("Show missing value bin updated");
        
//update the aggregation settings tab        
//      set the right aggregation method settings
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        final Collection<? extends ColorColumn> aggrColumns = 
            vizModel.getAggrColumns();
        if ((aggrColumns == null || aggrColumns.size() < 1)
                && vizModel.isFixed()) {
            //if we have no aggregation columns selected disable all
            //aggregation methods but not count
            for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                final AbstractButton button = buttons.nextElement();
                if (!button.getActionCommand()
                        .equals(AggregationMethod.COUNT.name())) {
                    button.setEnabled(false);
                    button.setToolTipText(AGGR_METHOD_DISABLED_TOOLTIP);
                }
//              select the current aggregation method
                if (button.getActionCommand()
                        .equals(vizModel.getAggregationMethod().name())) {
                    button.setSelected(true);
                }
            }
        } else {
            //enable all buttons
            for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                final AbstractButton button = buttons.nextElement();
                button.setEnabled(true);
                //remove the tool tip
                button.setToolTipText(null);
                //select the current aggregation method
                if (button.getActionCommand()
                        .equals(vizModel.getAggregationMethod().name())) {
                    button.setSelected(true);
                }
            }
        }
        LOGGER.debug("Aggregation method updated");
        
//update the visualization settings tab
        //show grid lines
        updateCheckBox(m_showGrid, vizModel.isShowGridLines(), true);
        LOGGER.debug("Show grid line updated");
        //Labels group
        //select the current display policy
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        for (final Enumeration<AbstractButton> buttons = m_labelDisplayPolicy
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand()
                    .equals(vizModel.getLabelDisplayPolicy().getID())) {
                button.setSelected(true);
            }
        }
        LOGGER.debug("Label display policy updated");
        //select the current label orientation
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        for (final Enumeration<AbstractButton> buttons = m_labelOrientation
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand()
                    .equals(LABEL_ORIENTATION_VERTICAL) 
                    && vizModel.isShowLabelVertical()) {
                button.setSelected(true);
            } else if (button.getActionCommand()
                    .equals(LABEL_ORIENTATION_HORIZONTAL) 
                    && !vizModel.isShowLabelVertical()) {
                button.setSelected(true);
            }
        }
        LOGGER.debug("Label orientation updated");
        //Bar layout group
        //select the current layout
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        for (final Enumeration<AbstractButton> buttons = m_layoutDisplayPolicy
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand()
                    .equals(vizModel.getHistogramLayout().getID())) {
                button.setSelected(true);
            }
        }
        LOGGER.debug("Layout updated");
        final int currentBinWidth = vizModel.getBinWidth();
        final int maxBinWidth = vizModel.getMaxBinWidth();
        int minBinWidth = AbstractHistogramVizModel.MIN_BIN_WIDTH;
        if (minBinWidth > maxBinWidth) {
            minBinWidth = maxBinWidth;
        }
        // update the bin width values        
        final ChangeListener[] widthListeners = m_binWidth.getChangeListeners();
        LOGGER.debug("No of bin width listener: " + widthListeners.length);
        for (ChangeListener listener : widthListeners) {
            m_binWidth.removeChangeListener(listener);
        }
        m_binWidth.setMaximum(maxBinWidth);
        m_binWidth.setMinimum(minBinWidth);
        m_binWidth.setValue(currentBinWidth);
        m_binWidth.setEnabled(true);
        m_binWidth.setToolTipText(
                AbstractHistogramProperties.BIN_WIDTH_TOOLTIP);
        AbstractHistogramProperties.setSliderLabels(m_binWidth, 2, false);
        for (ChangeListener listener : widthListeners) {
            m_binWidth.addChangeListener(listener);
        }
        LOGGER.debug("Bin width updated");
        //show bin outline
        updateCheckBox(m_showBinOutline, vizModel.isShowBinOutline(), 
                true);
        LOGGER.debug("Show bin outline updated");
        //show bar outline
        updateCheckBox(m_showBarOutline, vizModel.isShowBarOutline(), 
                true);
        LOGGER.debug("Show bar outline updated");
        //show element outline
        updateCheckBox(m_showElementOutline, vizModel.isShowElementOutline(), 
                true);
        LOGGER.debug("Show element outline updated");
        
        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for updateHistogramSettings. " 
                + durationTime + " ms");
        LOGGER.debug("Exiting updateHistogramSettings(vizModel) "
                + "of class AbstractHistogramProperties.");
    }

    /**
     * Removes all listener from the given box updates the values and
     * adds all previous removed listener.
     * @param box the select box to update
     * @param selected the selected value
     * @param enabled the enable value
     */
    private static void updateCheckBox(final JCheckBox box, 
            final boolean selected, 
            final boolean enabled) {
        LOGGER.debug("Entering updateSelectBox(box, selected, enabled) "
                + "of class AbstractHistogramProperties.");
        final ItemListener[] listeners = 
            box.getItemListeners();
        LOGGER.debug("No of select box listener: " + listeners.length);
        for (ItemListener listener : listeners) {
            box.removeItemListener(listener);
        }
        box.setSelected(selected);
        box.setEnabled(enabled);
        for (ItemListener listener : listeners) {
            box.addItemListener(listener);
        }
        LOGGER.debug("Exiting updateSelectBox(box, selected, enabled) "
                + "of class AbstractHistogramProperties.");
    }

    /**
     * @return the currently set bin width
     */
    public int getBinWidth() {
        return m_binWidth.getValue();
    }

    /**
     * @return the current no of bins
     */
    public int getNoOfBins() {
        return m_noOfBins.getValue();
    }

    /**
     * @return the current selected aggregation method
     */
    public AggregationMethod getSelectedAggrMethod() {
        if (m_aggrMethButtonGrp == null) {
            return AggregationMethod.getDefaultMethod();
        }
        final String methodName = m_aggrMethButtonGrp.getSelection()
                .getActionCommand();
        if (!AggregationMethod.valid(methodName)) {
            throw new IllegalArgumentException("No valid aggregation method");
        }
        return AggregationMethod.getMethod4String(methodName);
    }

    /**
     * @param actionCommand the action command of the radio button which should
     *            be the name of the <code>AggregationMethod</code>
     */
    protected abstract void onSelectAggrMethod(final String actionCommand);

    /**
     * Helper method to update the number of bins text field.
     * 
     * @param noOfbins the number of bins
     */
    protected void updateNoOfBinsText(final int noOfbins) {
        m_noOfBinsLabel.setText(Integer.toString(noOfbins));
    }

    /**
     * @return the current value of the show grid line select box
     */
    public boolean isShowGrid() {
        return m_showGrid.isSelected();
    }


    /**
     * @return the current value of the show bin outline select box
     */
    public boolean isShowBinOutline() {
        return m_showBinOutline.isSelected();
    }
    
    /**
     * @return the current value of the show bar outline select box
     */
    public boolean isShowBarOutline() {
        return m_showBarOutline.isSelected();
    }
    
    /**
     * @return the current value of the show element outline select box
     */
    public boolean isShowElementOutline() {
        return m_showElementOutline.isSelected();
    }

    /**
     * @return if the empty bins should be shown
     */
    public boolean isShowEmptyBins() {
        if (m_showEmptyBins == null) {
            return false;
        }
        return m_showEmptyBins.isSelected();
    }

    /**
     * @return if the missing value bin should be shown
     */
    public boolean isShowMissingValBin() {
        if (m_showMissingValBin == null) {
            return false;
        }
        return m_showMissingValBin.isSelected();
    }

    /**
     * @return <code>true</code> if the bar labels should be displayed
     *         vertical or <code>false</code> if the labels should be
     *         displayed horizontal
     */
    public boolean isShowLabelVertical() {
        final String actionCommand = m_labelOrientation.getSelection()
                .getActionCommand();
        return (LABEL_ORIENTATION_VERTICAL.equals(actionCommand));
    }

    /**
     * @return the label display policy
     */
    public LabelDisplayPolicy getLabelDisplayPolicy() {
        final String actionCommand = m_labelDisplayPolicy.getSelection()
                .getActionCommand();
        return LabelDisplayPolicy.getOption4ID(actionCommand);
    }

    /**
     * @return the histogram layout
     */
    public HistogramLayout getHistogramLayout() {
        final String actionCommand = m_layoutDisplayPolicy.getSelection()
                .getActionCommand();
        return HistogramLayout.getLayout4ID(actionCommand);
    }

    /**
     * @param listener the listener to listen if the label orientation has
     *            changed
     */
    protected void addLabelOrientationListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_labelOrientation
                .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener the listener to listen if the label display policy has
     *            changed
     */
    protected void addLabelDisplayListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_labelDisplayPolicy
                .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener the listener to listen if the layout has changed
     */
    protected void addLayoutListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_layoutDisplayPolicy
                .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener adds the listener to the show bin outline check box
     */
    public void addShowBinOutlineChangedListener(final ItemListener listener) {
        m_showBinOutline.addItemListener(listener);
    }

    /**
     * @param listener adds the listener to the show bar outline check box
     */
    public void addShowBarOutlineChangedListener(final ItemListener listener) {
        m_showBarOutline.addItemListener(listener);
    }
    
    /**
     * @param listener adds a listener to the show element outline check box.
     */
    protected void addShowElementOutlineChangedListener(
            final ItemListener listener) {
        m_showElementOutline.addItemListener(listener);
    }
    
    /**
     * @param listener adds the listener to the bin width slider
     */
    protected void addBinWidthChangeListener(final ChangeListener listener) {
        m_binWidth.addChangeListener(listener);
    }

    /**
     * @param listener adds a listener to the show grid lines check box.
     */
    protected void addShowGridChangedListener(final ItemListener listener) {
        m_showGrid.addItemListener(listener);
    }
    
    /**
     * @param listener adds the listener to the number of bars slider
     */
    protected void addNoOfBinsChangeListener(final ChangeListener listener) {
        m_noOfBins.addChangeListener(listener);
    }

    /**
     * @param listener adds the listener to the aggregation method button
     * group
     */
    protected void addAggrMethodListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
        .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }
    
    /**
     * @param listener adds the listener to the show empty bins select box
     */
    protected void addShowEmptyBinListener(final ItemListener listener) {
        m_showEmptyBins.addItemListener(listener);
    }
    
    /**
     * @param listener adds the listener to the show missing value bin 
     * select box
     */
    protected void addShowMissingValBinListener(final ItemListener listener) {
        m_showMissingValBin.addItemListener(listener);
    }
}
