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


/** Helper for snippet generating. */

public class SnippetGenerator {


    public static String generateHtmlSnippet(String id, String style, String baseUrl) {
        return format("<script type=\"text/javascript\" style=\"%s\" " +
                      "src=\"%s/factory/resources/factory.js?%s\"></script>",
                      style, baseUrl, id);
    }

    public static String generateNonEncodedHtmlSnippet(String factoryURL, String style, String baseUrl) {
        return format("<script type=\"text/javascript\" style=\"%s\" " +
                      "src=\"%s/factory/resources/factory.js\" url=\"%s\"></script>",
                      style, baseUrl, factoryURL);
    }

    public static String generateiFrameSnippet(String factoryURL) {
        return format("<iframe src=\"%s\" width=\"800\" height=\"480\"></iframe>", factoryURL);
    }

    public static String generateMarkdownSnippet(String factoryURL, String id, String imageId, String style,
                                                 String baseUrl) {
        switch (style) {
            case "Advanced":
            case "Advanced with Counter":
                if (imageId != null && id != null) {
                    return format("[![alt](%s/api/factory/%s/image?imgId=%s)](%s)",
                                  baseUrl, id,
                                  imageId, factoryURL);
                } else {
                    throw new IllegalArgumentException("Factory with advanced style MUST have at leas one image.");
                }
            case "White":
            case "Horizontal,White":
            case "Vertical,White":
                return format("[![alt](%s/factory/resources/factory-white.png)](%s)",
                              baseUrl, factoryURL);
            case "Dark":
            case "Horizontal,Dark":
            case "Vertical,Dark":
                return format("[![alt](%s/factory/resources/factory-dark.png)](%s)",
                              baseUrl, factoryURL);
            default:
                throw new IllegalArgumentException("Invalid factory style.");
        }
    }

    /**
     * Formats the input string filling the {@link String} arguments.
     * 
     * @param format string format
     * @param args {@link String} arguments
     * @return {@link String} formatted string
     */
    public static String format(final String format, final String... args) {
        String[] split = format.split("%s");
        final StringBuilder msg = new StringBuilder();
        for (int pos = 0; pos < split.length - 1; pos += 1) {
            msg.append(split[pos]);
            msg.append(args[pos]);
        }
        msg.append(split[split.length - 1]);
        return msg.toString();
    }
}
