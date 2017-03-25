/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.curator.x.discovery.details;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.TestJsonInstanceSerializer;
import org.apache.curator.x.discovery.UriSpec;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;
import java.util.Date;

public class TestJsonInstanceSerializerCompatibility
{
    @Test
    public void testCompatibilityMode() throws Exception
    {
        JsonInstanceSerializer<TestJsonInstanceSerializer.Payload> serializer = new JsonInstanceSerializer<TestJsonInstanceSerializer.Payload>(TestJsonInstanceSerializer.Payload.class, true, true);
        ServiceInstance<TestJsonInstanceSerializer.Payload> instance = new ServiceInstance<TestJsonInstanceSerializer.Payload>("name", "id", "address", 10, 20, new TestJsonInstanceSerializer.Payload("test"), 0, ServiceType.DYNAMIC, new UriSpec("{a}/b/{c}"), true);
        byte[] bytes = serializer.serialize(instance);

        OldServiceInstance<TestJsonInstanceSerializer.Payload> oldInstance = new OldServiceInstance<TestJsonInstanceSerializer.Payload>("name", "id", "address", 10, 20, new TestJsonInstanceSerializer.Payload("test"), 0, ServiceType.DYNAMIC, new UriSpec("{a}/b/{c}"));
        ObjectMapper mapper = new ObjectMapper();
        byte[] oldBytes = mapper.writeValueAsBytes(oldInstance);
        Assert.assertEquals(bytes, oldBytes, String.format("%s vs %s", new String(bytes), new String(oldBytes)));
    }

    @Test
    public void testBackwardCompatibility() throws Exception
    {
        JsonInstanceSerializer<TestJsonInstanceSerializer.Payload> serializer = new JsonInstanceSerializer<TestJsonInstanceSerializer.Payload>(TestJsonInstanceSerializer.Payload.class, true, true);
        ServiceInstance<TestJsonInstanceSerializer.Payload> instance = new ServiceInstance<TestJsonInstanceSerializer.Payload>("name", "id", "address", 10, 20, new TestJsonInstanceSerializer.Payload("test"), 0, ServiceType.DYNAMIC, new UriSpec("{a}/b/{c}"), false);
        byte[] bytes = serializer.serialize(instance);

        instance = serializer.deserialize(bytes);
        Assert.assertTrue(instance.isEnabled());    // passed false for enabled in the ctor but that is lost with compatibleSerializationMode

        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructType(OldServiceInstance.class);
        OldServiceInstance rawServiceInstance = mapper.readValue(bytes, type);
        TestJsonInstanceSerializer.Payload.class.cast(rawServiceInstance.getPayload()); // just to verify that it's the correct type
        //noinspection unchecked
        OldServiceInstance<TestJsonInstanceSerializer.Payload> check = (OldServiceInstance<TestJsonInstanceSerializer.Payload>)rawServiceInstance;
        Assert.assertEquals(check.getName(), "name");
        Assert.assertEquals(check.getPayload(), new TestJsonInstanceSerializer.Payload("test"));
    }

    @Test
    public void testFutureChanges() throws Exception
    {
        NewServiceInstance<String> newInstance = new NewServiceInstance<String>("name", "id", "address", 10, 20, "hey", 0, ServiceType.DYNAMIC, new UriSpec("{a}/b/{c}"), false, "what", 10101L, new Date(), new URI("http://hey"));
        byte[] newInstanceBytes = new ObjectMapper().writeValueAsBytes(newInstance);
        JsonInstanceSerializer<String> serializer = new JsonInstanceSerializer<String>(String.class);
        ServiceInstance<String> instance = serializer.deserialize(newInstanceBytes);
        Assert.assertEquals(instance.getName(), "name");
        Assert.assertEquals(instance.getPayload(), "hey");
        Assert.assertEquals(instance.isEnabled(), false);
    }
}