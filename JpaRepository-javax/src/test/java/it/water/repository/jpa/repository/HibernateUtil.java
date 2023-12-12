/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.repository.jpa.repository;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class HibernateUtil {
    private static final String PERSISTENCE_UNIT_NAME = "water-respository";
    private static EntityManagerFactory emFactory;

    public static EntityManagerFactory getEntityManagerFactory() {
        if (emFactory == null) {
            emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }
        return emFactory;
    }

    public static void closeEntityManagerFactory() {
        if (emFactory != null) {
            emFactory.close();
            emFactory = null;
        }
    }
}