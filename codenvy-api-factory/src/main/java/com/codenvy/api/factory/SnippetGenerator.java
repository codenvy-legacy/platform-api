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
package com.codenvy.api.factory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Formatter;
import java.util.Set;

/** Helper for snippet generating. */

public class SnippetGenerator {

    public static String generateUrlSnippet(String id, URI baseUri) {
        return UriBuilder.fromUri(baseUri).replacePath("factory").queryParam("id", id).build().toString();
    }

    public static String generateHtmlSnippet(String id, URI baseUri) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format("<script type=\"text/javascript\" language=\"javascript\" " +
                         "src=\"%1$s/factory/resources/embed.js?%2$s\"></script>",
                         UriBuilder.fromUri(baseUri).replacePath("").build(), id);
        return sb.toString();
    }


    public static String generateMarkdownSnippet(String id, Set<FactoryImage> factoryImages, String style,
                                                 URI baseUri) {
        String factoryURL =
                UriBuilder.fromUri(baseUri).replacePath("factory").queryParam("id", id).build().toString();
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        switch (style) {
            case "Advanced":
            case "Advanced with Counter":
                formatter.format("[![alt](%1$s/api/factory/%2$s/image?imgId=%3$s)](%4$s)",
                                 UriBuilder.fromUri(baseUri).replacePath("").build(), id,
                                 factoryImages.iterator().next().getName(), factoryURL);
                break;

            case "White":
            case "Horizontal,White":
            case "Vertical,White":
                formatter.format("[![alt](%1$s/factory/resources/factory-white.png)](%2$s)",
                                 UriBuilder.fromUri(baseUri).replacePath("").build(), factoryURL);
                break;
            case "Dark":
            case "Horizontal,Dark":
            case "Vertical,Dark":
                formatter.format("[![alt](%1$s/factory/resources/factory-dark.png)](%2$s)",
                                 UriBuilder.fromUri(baseUri).replacePath("").build(), factoryURL);
                break;

            default:
                throw new IllegalArgumentException("Invalid factory style.");
        }
        return sb.toString();
    }

}
