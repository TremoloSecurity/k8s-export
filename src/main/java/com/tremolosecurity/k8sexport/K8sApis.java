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

package com.tremolosecurity.k8sexport;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class K8sApis {
    String url;
    List<K8sApiGroup> apiGroups;
    private K8sApiGroupVersion v1;
    public K8sApis(String url) {
        this.url = url;
    }

    public void loadApis() throws URISyntaxException, IOException, InterruptedException, ParseException {
        // first get all the APIs
        HttpRequest req = HttpRequest.newBuilder()
         .uri(new URI(this.url + "/apis"))
         .GET()
         .version(Version.HTTP_1_1)
         .build();

         this.apiGroups = new ArrayList<K8sApiGroup>();


        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, BodyHandlers.ofString());

        String json = resp.body();

        System.out.println(json);

        JSONObject root = (JSONObject) new JSONParser().parse(json);

        JSONArray groups = (JSONArray) root.get("groups");

        for (Object o : groups) {
            JSONObject group = (JSONObject) o;
            String name = (String) group.get("name");
            
            JSONArray versions = (JSONArray) group.get("versions");

            K8sApiGroup apiGroup = new K8sApiGroup(name);
            this.apiGroups.add(apiGroup);


            for (Object oo : versions) {
                JSONObject version = (JSONObject) oo;

                K8sApiGroupVersion apiGroupVersion = new K8sApiGroupVersion((String)version.get("groupVersion"));

                apiGroup.getVersions().add(apiGroupVersion);

                System.out.println(version.get("groupVersion"));

                req = HttpRequest.newBuilder()
                .uri(new URI(this.url + "/apis/" + version.get("groupVersion")))
                .GET()
                .version(Version.HTTP_1_1)
                .build();

                resp = HttpClient.newHttpClient().send(req, BodyHandlers.ofString());

                String groupJson = resp.body();
                System.out.println(groupJson);
                JSONObject groupRoot = (JSONObject) new JSONParser().parse(groupJson);

                JSONArray resources = (JSONArray) groupRoot.get("resources");

                for (Object r : resources) {
                    JSONObject resource = (JSONObject) r;
                    if (resource.get("singularName") != null && ! resource.get("name").toString().contains("/")) {
                        K8sApi api = new K8sApi((String)resource.get("name"),(String)resource.get("singularName"),(String)resource.get("kind"),(Boolean)resource.get("namespaced"),apiGroupVersion);
                        apiGroupVersion.getApis().add(api);
                    }
                    
                }

                
            }

        }

        System.out.println("here");

        req = HttpRequest.newBuilder()
         .uri(new URI(this.url + "/api/v1"))
         .GET()
         .version(Version.HTTP_1_1)
         .build();

        resp = HttpClient.newHttpClient().send(req, BodyHandlers.ofString());

        json = resp.body();

        System.out.println(json);

        this.v1 = new K8sApiGroupVersion();

        JSONObject groupRoot = (JSONObject) new JSONParser().parse(json);

        JSONArray resources = (JSONArray) groupRoot.get("resources");

        for (Object r : resources) {
            JSONObject resource = (JSONObject) r;
            if (resource.get("singularName") != null && ! resource.get("name").toString().contains("/")) {
                K8sApi api = new K8sApi((String)resource.get("name"),(String)resource.get("singularName"),(String)resource.get("kind"),(Boolean)resource.get("namespaced"),this.v1);
                this.v1.getApis().add(api);
            }
            
        }
        

    }

    public Map<String,Map<String,JSONObject>> loadObjectsFromNamespace(String namespace) throws URISyntaxException, IOException, InterruptedException, ParseException {
        Map<String,Map<String,JSONObject>> objects = new HashMap<String,Map<String,JSONObject>>();
        
        for (K8sApiGroup apiGroup : this.apiGroups) {
            for (K8sApiGroupVersion apiGroupVersion : apiGroup.getVersions()) {
                for (K8sApi api : apiGroupVersion.getApis()) {
                    if (api.isNamespaced()) {
                        api.loadObjectsForNamespace(namespace, url,objects);
                    }
                }
            }
        }

        for (K8sApi api : this.v1.getApis()) {
            api.loadObjectsForNamespace(namespace, url,objects);
        }

        return objects;
        
    }


}
