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
 *   Nov 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ReorderRenderer extends JPanel implements TableCellRenderer, TableCellEditor {

    private final JButton m_up = new JButton(SharedIcons.SMALL_ARROW_UP_DARK.get());

    private final JButton m_down = new JButton(SharedIcons.SMALL_ARROW_DOWN_DARK.get());

    private final Set<CellEditorListener> m_listeners = new CopyOnWriteArraySet<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    private Direction m_direction;

    enum Direction {
        UP, DOWN, NONE;
    }

    ReorderRenderer() {
        super(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder().resetPos().fillHorizontal().setWeightX(1.0);
        m_up.setMaximumSize(new Dimension(25, 50));
        m_down.setMaximumSize(new Dimension(25, 50));
        add(m_up, gbc.build());
        add(m_down, gbc.incX().build());
        m_up.addActionListener(e -> click(Direction.UP));
        m_down.addActionListener(e -> click(Direction.DOWN));
    }

    private void click(final Direction direction) {
        m_direction = direction;
        for (CellEditorListener listener : m_listeners) {
            listener.editingStopped(m_changeEvent);
        }
    }


    @Override
    public Object getCellEditorValue() {
        return m_direction;
    }

    @Override
    public boolean isCellEditable(final EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(final EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public void cancelCellEditing() {
        // editing is just a click so nothing needs to be cancelled
    }

    @Override
    public void addCellEditorListener(final CellEditorListener l) {
        m_listeners.add(l);
    }

    @Override
    public void removeCellEditorListener(final CellEditorListener l) {
        m_listeners.remove(l);
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        m_direction = Direction.NONE;
        m_up.setEnabled(row > 0);
        m_down.setEnabled(row < table.getModel().getRowCount() - 1);
        return this;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus,
        final int row, final int column) {
        m_up.setEnabled(row > 0);
        m_down.setEnabled(row < table.getModel().getRowCount() - 1);
        return this;
    }

}
