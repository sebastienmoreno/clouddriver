/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.oort

import com.netflix.spinnaker.oort.controllers.ApplicationsController
import com.netflix.spinnaker.oort.model.ApplicationProvider
import com.netflix.spinnaker.oort.model.Cluster
import com.netflix.spinnaker.oort.model.ClusterProvider
import com.netflix.spinnaker.oort.model.LoadBalancerProvider
import com.netflix.spinnaker.oort.model.ServerGroup
import com.netflix.spinnaker.oort.model.aws.AmazonApplication
import spock.lang.Shared
import spock.lang.Specification

import java.lang.Void as Should

class ApplicationControllerSpec extends Specification {

  @Shared
  ApplicationsController applicationsController

  def setup() {
    applicationsController = new ApplicationsController()
  }

  Should "call all application providers on listing"() {
    setup:
    def appProvider1 = Mock(ApplicationProvider)
    def appProvider2 = Mock(ApplicationProvider)
    applicationsController.applicationProviders = [appProvider1, appProvider2]

    when:
    applicationsController.list()

    then:
    1 * appProvider1.getApplications()
    1 * appProvider2.getApplications()
  }

  Should "merge clusterNames and attributes when multiple apps are found"() {
    setup:
    def appProvider1 = Mock(ApplicationProvider)
    def appProvider2 = Mock(ApplicationProvider)
    def cluProvider1 = Mock(ClusterProvider)
    def elbProvider1 = Mock(LoadBalancerProvider)
    applicationsController.applicationProviders = [appProvider1, appProvider2]
    applicationsController.clusterProviders = [cluProvider1]
    applicationsController.loadBalancerProviders = [elbProvider1]
    def app1 = new AmazonApplication(name: "foo", clusterNames: [test: ["bar"] as Set], attributes: [tag: "val"])
    def app2 = new AmazonApplication(name: "foo", clusterNames: [test: ["baz"] as Set], attributes: [:])
    def cluster = Mock(Cluster)
    cluster.getAccountName() >> "test"
    cluster.getName() >> "foo"
    def sg1 = Mock(ServerGroup)
    sg1.getName() >> "bar"
    def sg2 = Mock(ServerGroup)
    sg2.getName() >> "baz"
    cluster.getServerGroups() >> [sg1, sg2]

    when:
    def result = applicationsController.get("foo")

    then:
    2 * elbProvider1.getLoadBalancers(_, _)
    2 * cluProvider1.getClusters("foo") >> [test: cluster]
    1 * appProvider1.getApplication("foo") >> app1
    1 * appProvider2.getApplication("foo") >> app2
    result.name == "foo"
    result.clusters.test*.serverGroups.flatten() == ["bar", "baz"]
    result.attributes == [tag: "val"]
  }
}
