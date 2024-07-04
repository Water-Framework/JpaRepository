/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.repository.jpa.osgi.hibernate;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.osgi.framework.Bundle;

import java.net.URL;

/**
 * @Author Aristide Cittadino
 * Take from the old hibernate-osgi project but simplfying it.
 */
public class OsgiArchiveDescriptionFactory implements ArchiveDescriptorFactory {
    private Bundle persistenceBundle;

    /**
     * Creates a OsgiArchiveDescriptorFactory
     *
     * @param persistenceBundle The OSGi bundle being scanned
     */
    public OsgiArchiveDescriptionFactory(Bundle persistenceBundle) {
        this.persistenceBundle = persistenceBundle;
    }

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url) {
        return buildArchiveDescriptor(url, "");
    }

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url, String entry) {
        return new OsgiArchiveDescriptor(persistenceBundle);
    }

    @Override
    public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
        // not used
        return null;
    }

}
