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

import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.osgi.framework.Bundle;

/**
 * @Author Aristide Cittadino
 * Take from the old hibernate-osgi project but simplfying it.
 * We just change the way Hibernate scans inside a bundle. Since each bundle will create its own persistence context.
 */
public class OsgiScanner extends AbstractScannerImpl implements Scanner {
    public OsgiScanner(Bundle persistenceBundle) {
        super(new OsgiArchiveDescriptionFactory(persistenceBundle));
    }
}
