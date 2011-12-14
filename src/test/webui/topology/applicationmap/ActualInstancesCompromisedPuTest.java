package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class ActualInstancesCompromisedPuTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private ProcessingUnit pu;

	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(2, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void antualInstancesTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		pu.waitFor(2);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			public boolean getCondition() {
				ApplicationNode testNode = appMap.getApplicationNode("Test");
				return (testNode.getActualInstances() == 2);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
	}
	

}
