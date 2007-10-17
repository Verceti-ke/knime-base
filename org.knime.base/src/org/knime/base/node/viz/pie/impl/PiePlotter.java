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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.PieHiliteCalculator;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.GeometryUtil;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;


/**
 * The abstract plotter implementation of the pie chart which acts as the
 * controller between the {@link PieVizModel} and the {@link PieDrawingPane}.
 * @author Tobias Koetter, University of Konstanz
 * @param <P> the {@link PieProperties} implementation
 * @param <D> the {@link PieVizModel} implementation
 */
public abstract class PiePlotter
<P extends PieProperties<D>, D extends PieVizModel> extends AbstractPlotter {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PiePlotter.class);

    private D m_vizModel;

    private final P m_props;

    /**Constructor for class PiePlotter.
     * @param properties the properties panel
     * @param handler the optional <code>HiliteHandler</code>
     */
    public PiePlotter(final P properties,
            final HiLiteHandler handler) {
        super(new PieDrawingPane(), properties);
        m_props = properties;
        if (handler != null) {
            super.setHiLiteHandler(handler);
        }
        registerPropertiesChangeListener();
    }


    /**
     * Registers all histogram properties listener to the histogram
     * properties panel.
     */
    private void registerPropertiesChangeListener() {
        m_props.addShowSectionOutlineChangedListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    vizModel.setDrawSectionOutline(
                            e.getStateChange() == ItemEvent.SELECTED);
                    final AbstractDrawingPane drawingPane =
                        getPieDrawingPane();
                    drawingPane.repaint();
                }
            }
        });
        m_props.addLabelDisplayListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    final P props = getPropertiesPanel();
                    if (props != null) {
                        vizModel.setLabelDisplayPolicy(
                                props.getLabelDisplayPolicy());
                        final AbstractDrawingPane drawingPane =
                            getPieDrawingPane();
                        drawingPane.repaint();
                    }
                }
            }
        });
        m_props.addValueScaleListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    final P props = getPropertiesPanel();
                    if (props != null) {
                        vizModel.setValueScale(props.getValueScale());
                        final AbstractDrawingPane drawingPane =
                            getPieDrawingPane();
                        drawingPane.repaint();
                    }
                }
            }
        });
        m_props.addShowDetailsListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    if (vizModel.setShowDetails(
                            e.getStateChange() == ItemEvent.SELECTED)) {
                        final AbstractDrawingPane drawingPane =
                            getPieDrawingPane();
                        drawingPane.repaint();
                    }
                }
            }
        });
        m_props.addPieSizeChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                final int pieSize = source.getValue();
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setPieSize((pieSize / 100.0))) {
                    updatePaintModel();
                }
            }
        });

        m_props.addExplodeSizeChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                final int explodeSize = source.getValue();
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setExplodeSize((explodeSize / 100.0))) {
                    updatePaintModel();
                }
            }
        });
        m_props.addAggrMethodListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                final String methodName = e.getActionCommand();
                if (!AggregationMethod.valid(methodName)) {
                    throw new IllegalArgumentException(
                            "No valid aggregation method");
                }
                final AggregationMethod aggrMethod =
                    AggregationMethod.getMethod4Command(methodName);
                if (vizModel.setAggregationMethod(aggrMethod)) {
                    updatePaintModel();
                }
            }
        });
        m_props.addShowMissingValSectionListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setShowMissingValSection(
                        e.getStateChange() == ItemEvent.SELECTED)) {
                    //reset the details view if the missing section was selected
                    final P properties = getPropertiesPanel();
                    if (properties != null) {
                        properties.updateHTMLDetailsPanel(
                            vizModel.getHTMLDetailData());
                    }
                    updatePaintModel();
                }
            }
        });
        m_props.addExplodeSelectedSectionListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setExplodeSelectedSections(
                        e.getStateChange() == ItemEvent.SELECTED)) {
                    updatePaintModel();
                }
            }
        });
    }

    /**
     * @return the properties panel
     */
    protected P getPropertiesPanel() {
        return m_props;
    }


    /**
     * Convenient method to cast the drawing pane.
     * @return the plotter drawing pane
     */
    protected PieDrawingPane getPieDrawingPane() {
        final PieDrawingPane myPane =
            (PieDrawingPane)getDrawingPane();
        if (myPane == null) {
            throw new IllegalStateException("Drawing pane must not be null");
        }
        return myPane;
    }

    /**
     * @param vizModel the vizModel to display
     */
    @SuppressWarnings("unchecked")
    public void setVizModel(final D vizModel) {
        if (vizModel == null) {
            throw new NullPointerException("vizModel must not be null");
        }
        m_vizModel = vizModel;
        m_vizModel.setDrawingSpace(getDrawingPaneDimension());
        final P properties = getPropertiesPanel();
        if (properties == null) {
            throw new NullPointerException("Properties must not be null");
        }
        properties.updatePanel(vizModel);
        updatePaintModel();
    }


    /**
     * @return the vizModel to display
     */
    protected D getVizModel() {
        return m_vizModel;
    }

    /**
     * Resets the visualization model.
     */
    public void resetVizModel() {
        m_vizModel = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        resetVizModel();
        super.setHiLiteHandler(null);
        getPieDrawingPane().reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final Dimension newDrawingSpace = getDrawingPaneDimension();
        if (vizModel.setDrawingSpace(newDrawingSpace)) {
            updatePaintModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePaintModel() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final PieDrawingPane drawingPane = getPieDrawingPane();
        setPieSections(vizModel);
        drawingPane.setVizModel(vizModel);
    }

    /**
     * Calculates the size of all pie sections.
     * @param vizModel the {@link PieVizModel} that provides visualisation
     * information and the sections
     */
    private void setPieSections(final PieVizModel vizModel) {
        final Rectangle2D pieArea = vizModel.getPieArea();
        final Rectangle2D explodedArea = vizModel.getExplodedArea();
        final boolean explode = vizModel.explodeSelectedSections();
//        final double explodePercentage = vizModel.getExplodeMargin();
        final double totalVal = vizModel.getAbsAggregationValue();
        final double arcPerVal = 360 / totalVal;
        final AggregationMethod method = vizModel.getAggregationMethod();
        final PieHiliteCalculator calculator = vizModel.getCalculator();
        final List<PieSectionDataModel> pieSections =
            vizModel.getSections2Draw();
        final int noOfSections = pieSections.size();
        double startAngle = 0;
        for (int i = 0; i < noOfSections; i++) {
            final PieSectionDataModel section = pieSections.get(i);
            final double value = Math.abs(section.getAggregationValue(method));
            double arcAngle = value * arcPerVal;
            //avoid a rounding gap
            if (i == noOfSections - 1) {
                arcAngle = 360 - startAngle;
            }
            if (arcAngle < PieVizModel.MINIMUM_ARC_ANGLE) {
                LOGGER.warn("Pie section: " + vizModel.createLabel(section)
                        + " angle " + arcAngle + " to small to display."
                        + " Angle updated to set to minimum angle "
                        + PieVizModel.MINIMUM_ARC_ANGLE);
                arcAngle = PieVizModel.MINIMUM_ARC_ANGLE;
                //skip this section
//                section.setPieSection(null, calculator);
//                continue;
            }
            final Rectangle2D bounds;
            //explode selected sections
            if (explode && section.isSelected()) {
                bounds = GeometryUtil.getArcBounds(pieArea, explodedArea,
                        startAngle, arcAngle, 1.0);
            } else {
                bounds = pieArea;
            }
            final Arc2D arc = new Arc2D.Double(bounds, startAngle, arcAngle,
                    Arc2D.PIE);
            section.setPieSection(arc, calculator);
            startAngle += arcAngle;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.clearSelection();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.selectElement(clicked);
        if (vizModel.explodeSelectedSections()) {
            updatePaintModel();
        }
        final P properties = getPropertiesPanel();
        if (properties != null) {
            properties.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.selectElement(selectionRectangle);
        if (vizModel.explodeSelectedSections()) {
            updatePaintModel();
        }
        final P properties = getPropertiesPanel();
        if (properties != null) {
            properties.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell>hilited = event.keys();
        vizModel.updateHiliteInfo(hilited, true);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell>hilited = event.keys();
        vizModel.updateHiliteInfo(hilited, false);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell> selectedKeys =
            vizModel.getSelectedKeys();
        delegateHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell> selectedKeys =
            vizModel.getSelectedKeys();
        delegateUnHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        vizModel.unHiliteAll();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPopupMenu(final JPopupMenu popupMenu) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            //add disable the popup menu since this implementation
            //doesn't supports hiliting
            popupMenu.setEnabled(false);
        } else {
            super.fillPopupMenu(popupMenu);
        }
    }
}
