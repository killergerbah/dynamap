/*
    Copyright 2017 N3TWORK INC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.n3twork.dynamap;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n3twork.dynamap.test.*;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

public class DynamapTest {

    private AmazonDynamoDB ddb;

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeTest
    public void init() {
        System.setProperty("sqlite4java.library.path", "native-libs");
        ddb = DynamoDBEmbedded.create().amazonDynamoDB();
    }

    @Test
    public void testDynaMap() throws Exception {
        SchemaRegistry schemaRegistry = new SchemaRegistry(getClass().getResourceAsStream("/TestSchema.json"));

        // Create tables
        Dynamap dynamap = new Dynamap(ddb, schemaRegistry).withPrefix("test").withObjectMapper(objectMapper);
        dynamap.createTables(true);

        // Save
        String exampleId = UUID.randomUUID().toString();
        String nestedId = UUID.randomUUID().toString();
        NestedTypeBean nestedObject = new NestedTypeBean(nestedId, null, null, null, null, null,
                null, null, null, null);
        ExampleDocumentBean doc = new ExampleDocumentBean(exampleId,
                1, nestedObject, null, null, "alias");
        dynamap.save(doc, null);

        ExampleDocumentBean doc2 = new ExampleDocumentBean(exampleId,
                2, nestedObject, null, null, "alias");

        // overwrite allowed
        dynamap.save(doc2, true, null);

        // overwrite will fail
        try {
            dynamap.save(doc2, false, null);
            Assert.fail();
        }
        catch (RuntimeException ex){
            Assert.assertNotNull(ex);
        }

        // Get Object
        GetObjectRequest<ExampleDocumentBean> getObjectRequest = new GetObjectRequest<>(ExampleDocumentBean.class).withHashKeyValue(exampleId).withRangeKeyValue(1);
        ExampleDocumentBean exampleDocument = dynamap.getObject(getObjectRequest, null);

        Assert.assertEquals(exampleDocument.getExampleId(), exampleId);
        nestedObject = new NestedTypeBean(exampleDocument.getNestedObject());
        Assert.assertEquals(nestedObject.getId(), nestedId);

        // Get Not Exists
        Assert.assertNull(dynamap.getObject(new GetObjectRequest<>(ExampleDocumentBean.class).withHashKeyValue("blah").withRangeKeyValue(1), null));

        // Update root object
        ExampleDocumentUpdates exampleDocumentUpdates = new ExampleDocumentUpdates(exampleDocument, exampleDocument.getHashKeyValue(), exampleDocument.getRangeKeyValue());
        exampleDocumentUpdates.setAlias("new alias");
        dynamap.update(exampleDocumentUpdates);

        exampleDocument = dynamap.getObject(getObjectRequest, null);
        Assert.assertEquals(exampleDocument.getAlias(), "new alias");


        // Update nested object
        NestedTypeUpdates nestedTypeUpdates = new NestedTypeUpdates(nestedObject, exampleId, 1);
        nestedTypeUpdates.setBio("test nested");
        dynamap.update(nestedTypeUpdates);

        exampleDocument = dynamap.getObject(getObjectRequest, null);
        Assert.assertEquals(exampleDocument.getNestedObject().getBio(), "test nested");


        // Update parent and nested object
        exampleDocumentUpdates.setAlias("alias");
        nestedTypeUpdates.setBio("test");
        exampleDocumentUpdates.setNestedObjectUpdates(nestedTypeUpdates);
        dynamap.update(exampleDocumentUpdates);

        exampleDocument = dynamap.getObject(getObjectRequest, null);
        Assert.assertEquals(exampleDocument.getAlias(), "alias");
        Assert.assertEquals(exampleDocument.getNestedObject().getBio(), "test");

        // Query
        QueryRequest<ExampleDocumentBean> queryRequest = new QueryRequest<>(ExampleDocumentBean.class).withHashKeyValue("alias")
                .withRangeKeyCondition(new RangeKeyCondition("seq").eq(1)).withIndex(ExampleDocumentBean.GlobalSecondaryIndex.exampleIndex);
        List<ExampleDocumentBean> exampleDocuments = dynamap.query(queryRequest, null);
        Assert.assertEquals(exampleDocuments.size(), 1);
        Assert.assertEquals(exampleDocuments.get(0).getNestedObject().getBio(), "test");


        // Migration
        String jsonSchema = IOUtils.toString(getClass().getResourceAsStream("/TestSchema.json"));
        jsonSchema = jsonSchema.replace("\"version\": 1,", "\"version\": 2,");
        schemaRegistry = new SchemaRegistry(new ByteArrayInputStream(jsonSchema.getBytes()));
        schemaRegistry.registerMigration("Example", new Migration() {
            @Override
            public int getVersion() {
                return 2;
            }

            @Override
            public int getSequence() {
                return 0;
            }

            @Override
            public void migrate(Item item, int version, Object context) {
                item.withString("alias", "newAlias");
            }

            @Override
            public void postMigration(Item item, int version, Object context) {

            }
        });


        dynamap = new Dynamap(ddb, schemaRegistry).withPrefix("test").withObjectMapper(objectMapper);
        getObjectRequest = new GetObjectRequest(ExampleDocumentBean.class).withHashKeyValue(exampleId).withRangeKeyValue(1);
        exampleDocument = dynamap.getObject(getObjectRequest, null);
        Assert.assertEquals(exampleDocument.getAlias(), "newAlias");


        // Delete
        final int sequence = 3;
        ExampleDocumentBean doc3 = new ExampleDocumentBean(exampleId,
                sequence, nestedObject, null, null, "alias");
        dynamap.save(doc3, false, null);

        GetObjectRequest<ExampleDocumentBean> getObjectRequest3 = new GetObjectRequest<>(ExampleDocumentBean.class).withHashKeyValue(exampleId).withRangeKeyValue(sequence);
        ExampleDocument exampleDocument3 = dynamap.getObject(getObjectRequest3, null);
        Assert.assertNotNull(exampleDocument3);

        DeleteRequest<ExampleDocumentBean> deleteRequest = new DeleteRequest<>(ExampleDocumentBean.class)
                .withHashKeyValue(exampleId)
                .withRangeKeyValue(sequence);

        dynamap.delete(deleteRequest);

        getObjectRequest3 = new GetObjectRequest<>(ExampleDocumentBean.class).withHashKeyValue(exampleId).withRangeKeyValue(sequence);
        exampleDocument3 = dynamap.getObject(getObjectRequest3, null);
        Assert.assertNull(exampleDocument3);


    }


}
