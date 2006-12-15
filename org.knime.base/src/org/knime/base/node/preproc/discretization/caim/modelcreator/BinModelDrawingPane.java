/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   13.11.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim.modelcreator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;

/**
 * The actual drawing pain, rendering the binned column model. The binned
 * columns are rendered as horizontal lines with ticks at the bin boarders.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class BinModelDrawingPane extends ScatterPlotterDrawingPane {

    private static final int TICK_SIZE = 4;

    private int m_hMargin = 50;

    private BinRuler[] m_binningSchemes;

    /**
     * Sets the background color to white.
     */
    public BinModelDrawingPane() {
        super.setBackground(Color.white);
    }

    /**
     * Sets the horizontal margin.
     * 
     * @param hMargin sets the horizontal margin
     */
    public void setHorizontalMargin(final int hMargin) {
        m_hMargin = hMargin;
        assert m_hMargin == hMargin;
    }

    /**
     * Sets the binning schemes for all bins that schould be painted.
     * 
     * @param schemes sets the binning schemes for all bins that schould be
     *            painted.
     */
    public void setBinningSchemes(final BinRuler[] schemes) {

        m_binningSchemes = schemes;
    }

    /**
     * @see org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane#
     *      paintContent(java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {

        // set the preferred size to the needed height
        int width = getPreferredSize().width;
        int height =
                m_binningSchemes[m_binningSchemes.length - 1]
                        .getLeftStartPoint().y + 40;
        setPreferredSize(new Dimension(width, height));

        super.paintContent(g);
        if (m_binningSchemes == null) {
            return;
        }

        for (BinRuler scheme : m_binningSchemes) {

            // first draw the line of the ruler
            int x = scheme.getLeftStartPoint().x;
            int y = scheme.getLeftStartPoint().y;
            int xR = x + scheme.getWidth();
            g.drawLine(x, y, xR, y);

            // then draw the ticks...
            int[] tickXPositions = scheme.getBinPositions();
            for (int tickXPos : tickXPositions) {
                g.drawLine(tickXPos + m_hMargin, y, tickXPos + m_hMargin, y
                        + TICK_SIZE);
            }

            // ...and the corresponding labels
            String[] labels = scheme.getBinPosValue();
            int yPos = y + TICK_SIZE + 12;
            g.drawString(labels[0], x, yPos);
            for (int i = 1; i < labels.length; i++) {
                int fontHeight = g.getFontMetrics().getHeight();
                int yPosAlternate = yPos + (i % 2 * fontHeight);

                int tickX;
                // if this is the last tick
                if (i == labels.length - 1) {
                    tickX =
                            tickXPositions[i]
                                    - g.getFontMetrics().stringWidth(labels[i]);
                } else {
                    int fontWidthHalf =
                            g.getFontMetrics().stringWidth(labels[i]) / 2;
                    tickX = tickXPositions[i] - fontWidthHalf;
                }
                g.drawString(labels[i], tickX + m_hMargin, yPosAlternate);
            }
            // draw the column name above the ruler
            Font tempFont = g.getFont();
            g.setFont(new Font("arial", Font.BOLD, 12));
            g.drawString(scheme.getName(), x, y
                    - g.getFontMetrics().getHeight());
            g.setFont(tempFont);
        }

        super.paintContent(g);

    }

    /**
     * Not supported yet.
     * 
     * @param x1 left corner x
     * @param y1 left corner y
     * @param x2 right corner x
     * @param y2 right corner y
     */
    @Override
    public void selectElementsIn(final int x1, final int y1, final int x2,
            final int y2) {

        // no selection supported yet
    }

    /**
     * No tool tips yet.
     * 
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(final MouseEvent e) {

        // no tool tips yet
        return null;
    }

}
