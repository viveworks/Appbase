/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.core.service.exception.store.inmemory;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class InMemoryExceptionStoreTest {
    @Test
    void testExceptionStore() throws Exception {
        final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration =
                InMemoryExceptionStoreConfiguration.builder().build();
        final InMemoryExceptionStore inMemoryExceptionStore =
                new InMemoryExceptionStore(inMemoryExceptionStoreConfiguration);
        runTests(inMemoryExceptionStore);

        inMemoryExceptionStore.reload(inMemoryExceptionStoreConfiguration);
        runTests(inMemoryExceptionStore);

        Assertions.assertNotNull(inMemoryExceptionStore.getInMemoryExceptionStoreConfiguration());
        Assertions.assertDoesNotThrow(inMemoryExceptionStore::synchronize);
        Assertions.assertDoesNotThrow(inMemoryExceptionStore::shutdown);
    }

    private void runTests(final InMemoryExceptionStore inMemoryExceptionStore) throws Exception {
        final long timestamp1 = System.currentTimeMillis();
        final ExceptionMessage exceptionMessage1 = CoreTestFixtures.getExceptionMessage(timestamp1);
        inMemoryExceptionStore.writeExceptionMessage(exceptionMessage1);


        final Collection<ExceptionMessageDetails> allExceptionMessageDetails = inMemoryExceptionStore
                .asyncReadAllExceptionMessageDetails()
                .get();
        Assertions.assertNotNull(allExceptionMessageDetails);
        Assertions.assertEquals(1, allExceptionMessageDetails.size());
        final ExceptionMessageDetails exceptionMessageDetails1 = allExceptionMessageDetails.iterator().next();
        check(exceptionMessage1, exceptionMessageDetails1);

        final Collection<ExceptionMessageDetails> allExceptionMessageDetails1 = inMemoryExceptionStore
                .readAllExceptionMessageDetails();
        Assertions.assertNotNull(allExceptionMessageDetails1);
        Assertions.assertEquals(1, allExceptionMessageDetails1.size());
        final ExceptionMessageDetails exceptionMessageDetails11 = allExceptionMessageDetails1.iterator().next();
        check(exceptionMessage1, exceptionMessageDetails11);

        final ExceptionMessageDetails exceptionMessageDetails0 = inMemoryExceptionStore
                .readExceptionMessageDetailsById(exceptionMessageDetails1.getId());
        Assertions.assertNotNull(exceptionMessageDetails0);
        Assertions.assertEquals(exceptionMessageDetails1, exceptionMessageDetails0);

        final long timestamp2 = timestamp1 + 1000;
        final ExceptionMessage exceptionMessage2 = CoreTestFixtures.getExceptionMessage(timestamp2);
        exceptionMessage2.setThrowable(new NullPointerException());
        inMemoryExceptionStore.asyncWriteExceptionMessage(exceptionMessage2);
        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTimestamps1 = inMemoryExceptionStore
                .asyncReadAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2)
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTimestamps1);
        Assertions.assertEquals(1, exceptionMessageDetailsByTimestamps1.size());
        final ExceptionMessageDetails exceptionMessageDetails2 = exceptionMessageDetailsByTimestamps1.iterator().next();
        check(exceptionMessage1, exceptionMessageDetails2);

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTimestamps11 = inMemoryExceptionStore
                .readAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp2);
        Assertions.assertNotNull(exceptionMessageDetailsByTimestamps11);
        Assertions.assertEquals(1, exceptionMessageDetailsByTimestamps11.size());
        final ExceptionMessageDetails exceptionMessageDetails12 = exceptionMessageDetailsByTimestamps11.iterator().next();
        check(exceptionMessage1, exceptionMessageDetails12);

        final long timestamp3 = timestamp2 + 100000;
        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTimestamps2 = inMemoryExceptionStore
                .asyncReadAllExceptionMessageDetailsByTimestamps(timestamp1, timestamp3)
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTimestamps2);
        Assertions.assertEquals(2, exceptionMessageDetailsByTimestamps2.size());
        final Iterator<ExceptionMessageDetails> iterator = exceptionMessageDetailsByTimestamps2.iterator();
        final ExceptionMessageDetails exceptionMessageDetails3 = iterator.next();
        check(exceptionMessage1, exceptionMessageDetails3);
        final ExceptionMessageDetails exceptionMessageDetails4 = iterator.next();
        check(exceptionMessage2, exceptionMessageDetails4);


        final Collection<String> typeNames = inMemoryExceptionStore
                .asyncReadUniqueThrowableTypeNames()
                .get();
        Assertions.assertNotNull(typeNames);
        Assertions.assertEquals(2, typeNames.size());
        Assertions.assertTrue(typeNames.contains(AppbaseException.class.getName()));
        Assertions.assertTrue(typeNames.contains(NullPointerException.class.getName()));


        final Collection<String> typeNames1 = inMemoryExceptionStore
                .readUniqueThrowableTypeNames();
        Assertions.assertNotNull(typeNames1);
        Assertions.assertEquals(2, typeNames1.size());
        Assertions.assertTrue(typeNames1.contains(AppbaseException.class.getName()));
        Assertions.assertTrue(typeNames1.contains(NullPointerException.class.getName()));

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTypeName1 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsByThrowableTypeName(AppbaseException.class.getName())
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTypeName1);
        Assertions.assertEquals(1, exceptionMessageDetailsByTypeName1.size());
        check(exceptionMessage1, exceptionMessageDetailsByTypeName1.iterator().next());
        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTypeName11 = inMemoryExceptionStore
                .readExceptionMessageDetailsByThrowableTypeName(AppbaseException.class.getName());
        Assertions.assertNotNull(exceptionMessageDetailsByTypeName11);
        Assertions.assertEquals(1, exceptionMessageDetailsByTypeName11.size());
        check(exceptionMessage1, exceptionMessageDetailsByTypeName11.iterator().next());

        final Collection<ExceptionMessageDetails> exceptionMessageDetailsByTypeName2 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsByThrowableTypeName(NullPointerException.class.getName())
                .get();
        Assertions.assertNotNull(exceptionMessageDetailsByTypeName2);
        Assertions.assertEquals(1, exceptionMessageDetailsByTypeName2.size());
        check(exceptionMessage2, exceptionMessageDetailsByTypeName2.iterator().next());

        final int removeId = exceptionMessageDetails1.getId();
        final ExceptionMessageDetails exceptionMessageDetails6 = inMemoryExceptionStore
                .readExceptionMessageDetailsById(removeId);
        Assertions.assertNotNull(exceptionMessageDetails6);
        final Void result = inMemoryExceptionStore
                .asyncRemoveExceptionMessageDetailsById(removeId)
                .get();
        Assertions.assertNull(result);
        inMemoryExceptionStore.removeExceptionMessageDetailsById(removeId);

        final ExceptionMessageDetails exceptionMessageDetails7 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsById(removeId)
                .get();
        Assertions.assertNull(exceptionMessageDetails7);


        final ExceptionMessageDetails exceptionMessageDetails8 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsById(exceptionMessageDetails4.getId())
                .get();
        Assertions.assertNotNull(exceptionMessageDetails8);
        final long timestamp4 = timestamp3 + 10000;
        final Void removeExceptionMessageDetailsBefore = inMemoryExceptionStore
                .asyncRemoveExceptionMessageDetailsExclusiveBefore(timestamp4)
                .get();
        Assertions.assertNull(removeExceptionMessageDetailsBefore);
        inMemoryExceptionStore.removeExceptionMessageDetailsExclusiveBefore(timestamp4);

        final ExceptionMessageDetails exceptionMessageDetails9 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsById(exceptionMessageDetails4.getId())
                .get();
        Assertions.assertNull(exceptionMessageDetails9);


        final ExceptionMessageDetails noExceptionMessageDetails1 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsById(1111)
                .get();
        Assertions.assertNull(noExceptionMessageDetails1);

        final Collection<ExceptionMessageDetails> noExceptionMessageDetails2 = inMemoryExceptionStore
                .asyncReadExceptionMessageDetailsByThrowableTypeName(ClassNotFoundException.class.getName())
                .get();
        Assertions.assertNotNull(noExceptionMessageDetails2);
        Assertions.assertTrue(noExceptionMessageDetails2.isEmpty());

        final long current = timestamp4 + 1000;
        final long after = current + 1000;
        final Collection<ExceptionMessageDetails> noExceptionMessageDetails3 = inMemoryExceptionStore
                .asyncReadAllExceptionMessageDetailsByTimestamps(current, after)
                .get();
        Assertions.assertNotNull(noExceptionMessageDetails3);
        Assertions.assertTrue(noExceptionMessageDetails3.isEmpty());
    }

    public static void check(
            final ExceptionMessage exceptionMessage,
            final ExceptionMessageDetails exceptionMessageDetails)
    {
        Assertions.assertEquals(
                exceptionMessage.getSourceType().getName(),
                exceptionMessageDetails.getSourceTypeName());
        Assertions.assertEquals(
                exceptionMessage.getMessage(),
                exceptionMessageDetails.getMessage());
        Assertions.assertEquals(
                exceptionMessage.getThreadName(),
                exceptionMessageDetails.getThreadName());
        Assertions.assertEquals(
                exceptionMessage.getTimestamp(),
                exceptionMessageDetails.getTimestamp());
    }
}
