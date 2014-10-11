/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.EnvironmentId;
import com.codenvy.dto.server.DtoFactory;

import org.testng.annotations.Test;

/**
 * @author andrew00x
 */
public class UpdateTest {
    @Test
    public void main() throws Exception {
//        File f = new File("/home/andrew/development/work/codenvy/codenvy-templates");
//        for (File _f : f.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File pathname) {
//                return pathname.isDirectory();
//            }
//        })) {
//            System.err.println(_f);
//            File codenvy = new File(_f, ".codenvy");
//            File project = new File(codenvy, "project.json");
//            ProjectJson jsonOld;
//            try (FileInputStream fIn = new FileInputStream(project)) {
//                jsonOld = ProjectJson.load(fIn);
//            }
//            project.renameTo(new File(codenvy, "___project.json"));
//            ProjectJson2 jsonNew = new ProjectJson2();
//            jsonNew.withType(jsonOld.getProjectTypeId());
//            jsonNew.withBuilders(new Builders(jsonOld.getBuilder()));
//            jsonNew.withRunners(new Runners(jsonOld.getRunner()));
//            jsonNew.setDescription(jsonOld.getDescription());
//            Map<String, List<String>> attributes = jsonOld.getAttributes();
//            attributes.remove("runner.env_id");
//            attributes.remove("language.version");
//            attributes.remove("runner.JavaWeb.memsize");
//            attributes.remove("framework");
//            jsonNew.setAttributes(attributes);
//            final String jsonString = JsonHelper.toJson(jsonNew);
//            System.out.println(jsonString);
//            try (FileWriter fOut = new FileWriter(project)) {
//                fOut.write(jsonString);
//            }
//        }
    }
}
