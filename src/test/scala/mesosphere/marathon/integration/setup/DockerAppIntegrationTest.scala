package mesosphere.marathon
package integration.setup

import mesosphere.AkkaIntegrationFunTest
import mesosphere.marathon.integration.facades.MarathonFacade._
import mesosphere.marathon.raml.Resources
import mesosphere.marathon.state.{ AppDefinition, Container }

import scala.concurrent.duration._

@IntegrationTest
class DockerAppIntegrationTest extends AkkaIntegrationFunTest with EmbeddedMarathonTest {
  //clean up state before running the test case
  before(cleanUp())

  test("deploy a simple Docker app") {
    Given("a new Docker app")
    val app = AppDefinition(
      id = testBasePath / "dockerapp",
      cmd = Some("sleep 600"),
      container = Some(Container.Docker(image = "busybox")),
      resources = Resources(cpus = 0.2, mem = 16.0),
      instances = 1
    )

    When("The app is deployed")
    val result = marathon.createAppV2(app)

    Then("The app is created")
    result.code should be(201) // Created
    extractDeploymentIds(result) should have size 1
    waitForEvent("deployment_success")
    waitForTasks(app.id, 1) // The app has really started
  }

  test("create a simple docker app using http health checks with HOST networking") {
    Given("a new app")
    val app = dockerAppProxy(testBasePath / "docker-http-app", "v1", instances = 1, withHealth = true)
    val check = appProxyCheck(app.id, "v1", true)

    When("The app is deployed")
    val result = marathon.createAppV2(app)

    Then("The app is created")
    result.code should be(201) //Created
    extractDeploymentIds(result) should have size 1
    waitForEvent("deployment_success")
    check.pingSince(5.seconds) should be(true) //make sure, the app has really started
  }
}
