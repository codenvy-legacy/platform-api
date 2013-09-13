/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.core.rest;

import com.codenvy.api.core.rest.dto.DtoException;
import com.codenvy.api.core.rest.dto.DtoType;
import com.codenvy.api.core.rest.dto.DtoTypesRegistry;
import com.codenvy.api.core.rest.dto.JsonDto;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class DtoTest {
    @DtoType(1001)
    public static class TestDto {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "TestDto{" +
                   "message='" + message + '\'' +
                   '}';
        }
    }

    // There is no DtoType annotation.
    public static class InvalidTestDto {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "InvalidTestDto{" +
                   "message='" + message + '\'' +
                   '}';
        }
    }

    @BeforeTest
    public void before() {
        DtoTypesRegistry.registerDto(TestDto.class);
    }

    @AfterTest
    public void after() {
        Assert.assertTrue(DtoTypesRegistry.unregisterDto(TestDto.class));
    }

    @Test
    public void testJsonToDto() throws Exception {
        final String content = "{\"_type\":1001, \"message\":\"DTO test\"}";
        final JsonDto dto = JsonDto.fromJson(content);
        Assert.assertEquals(dto.getType(), 1001);
        TestDto object = dto.cast();
        Assert.assertEquals(object.getMessage(), "DTO test");
    }

    @Test(expectedExceptions = {DtoException.class}, expectedExceptionsMessageRegExp = "Unknown DTO type '1002'")
    public void testJsonToDtoInvalidDto() throws Exception {
        // Invalid DTO type: 1002
        final String content = "{\"_type\":1002, \"message\":\"DTO test\"}";
        JsonDto.fromJson(content);
    }

    @Test
    public void testConvertDtoToJson() throws Exception {
        final TestDto testDto = new TestDto();
        testDto.setMessage("my message");
        final String json = JsonDto.toJson(testDto);
        System.out.println(json);
        // restore and check, if restore successful - serialization works well.
        JsonDto jsonDto = JsonDto.fromJson(json);
        Assert.assertEquals(jsonDto.getType(), 1001);
        TestDto object = jsonDto.cast();
        Assert.assertEquals(object.getMessage(), "my message");
    }

    @Test(expectedExceptions = {DtoException.class})
    public void testConvertInvalidDtoToJson() throws Exception {
        final InvalidTestDto testDto = new InvalidTestDto();
        testDto.setMessage("my message");
        JsonDto.toJson(testDto);
    }
}
