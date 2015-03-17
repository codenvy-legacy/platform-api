package org.eclipse.che.api.auth;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

@Listeners(MockitoTestNGListener.class)
public class QueryParameterTokenExtractorTest {

    @Mock
    HttpServletRequest request;


    @Test
    public void shouldExtractFromSingleQueryParam() {
        //given
        QueryParameterTokenExtractor extractor = new QueryParameterTokenExtractor();
        when(request.getQueryString()).thenReturn("token=0239845");

        //when
        String actual = extractor.getToken(request);
        //then
        assertEquals(actual, "0239845");
    }


    @Test
    public void shouldExtractFromMultipleQueryParam() {
        //given
        QueryParameterTokenExtractor extractor = new QueryParameterTokenExtractor();
        when(request.getQueryString()).thenReturn("par=345&token=567845");

        //when
        String actual = extractor.getToken(request);
        //then
        assertEquals(actual, "567845");
    }

    @Test
    public void shouldFindNothing() {
        //given
        QueryParameterTokenExtractor extractor = new QueryParameterTokenExtractor();
        when(request.getQueryString()).thenReturn("par=345");

        //when
        String actual = extractor.getToken(request);
        //then
        assertNull(actual);
    }

    @Test
    public void shouldFindNothingWithEmptyParameter() {
        //given
        QueryParameterTokenExtractor extractor = new QueryParameterTokenExtractor();
        when(request.getQueryString()).thenReturn("par=345&token=");

        //when
        String actual = extractor.getToken(request);
        //then
        assertNull(actual);
    }

}