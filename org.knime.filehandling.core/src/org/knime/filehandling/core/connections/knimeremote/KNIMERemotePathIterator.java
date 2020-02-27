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
 *   Nov 11, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knimeremote;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import org.knime.filehandling.core.util.MountPointIDProviderService;

/**
 * Iterates over all the files and folders of the path on a remote KNIME mount point.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMERemotePathIterator implements Iterator<Path> {

    private final KNIMERemoteFileSystem m_fileSystem;

    private Iterator<KNIMERemotePath> m_iterator;

    /**
     * Creates an iterator over all the files and folder in the given paths location.
     *
     * @param path destination to iterate over
     * @param filter
     * @throws IOException
     * @throws UncheckedIOException on I/O errors
     */
    public KNIMERemotePathIterator(final Path path, final Filter<? super Path> filter) throws IOException {
        final KNIMERemotePath knimePath = (KNIMERemotePath)path;
        m_fileSystem = (KNIMERemoteFileSystem)knimePath.getFileSystem();

        final List<URI> uriList;
        uriList = MountPointIDProviderService.instance().listFiles(path.toUri());
        m_iterator = uriList.stream()
            .map(p -> new KNIMERemotePath(m_fileSystem, p))
            .filter(p -> {
                try {
                    return filter.accept(p);
                } catch (final IOException ex) {
                    throw new UncheckedIOException(ex);
                }})
            .iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path next() {
        return m_iterator.next();
    }

}
