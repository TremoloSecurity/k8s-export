/*
 Copyright 2022 Tremolo Security, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.tremolosecurity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import com.tremolosecurity.k8sexport.K8sApis;

import org.json.simple.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Starting export of namespace " + args[0] + " to " + args[1] );

        K8sApis apis = new K8sApis("http://localhost:8001");
        apis.loadApis();

        Map<String,Map<String,JSONObject>> objects = apis.loadObjectsFromNamespace(args[0]);


        objects.remove("secrets");
        objects.remove("events");
        objects.remove("endpoints");
        objects.remove("pods");
        objects.remove("replicasets");
        objects.remove("endpointslices");

        for (String objectType : objects.keySet()) {
            Map<String,JSONObject> objs = objects.get(objectType);
            if (! objs.isEmpty()) {
                new File(args[1] + "/" + objectType).mkdirs(); 

                for (String objName : objs.keySet()) {

                    if (objectType.equalsIgnoreCase("serviceaccounts") && objName.equalsIgnoreCase("default")) {
                        continue;
                    } 

                    JSONObject obj = objs.get(objName);

                    JSONObject metadata = (JSONObject) obj.get("metadata");

                    if (metadata != null) {
                        JSONObject annotations = (JSONObject) metadata.get("annotations");

                        if (annotations != null) {
                            annotations.remove("meta.helm.sh/release-name");
                            annotations.remove("meta.helm.sh/release-namespace");
                        }

                        JSONObject labels = (JSONObject) metadata.get("labels");
                        if (labels != null) {
                            labels.remove("app.kubernetes.io/managed-by");
                        }
                    }

                    JsonNode jsonNodeTree = new ObjectMapper().readTree(obj.toString());
                    String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);

                    FileOutputStream out = new FileOutputStream(args[1] + "/" + objectType + "/" + objName + ".yaml");
                    out.write(jsonAsYaml.getBytes("UTF-8"));
                    out.flush();
                    out.close();
                    
                }
            }
        }

        File tmpdir = new File(args[1]);
        tmpdir.mkdir();
    }
}
