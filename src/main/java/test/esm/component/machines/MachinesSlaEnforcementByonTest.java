package test.esm.component.machines;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.grid.gsm.machines.CapacityMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcement;
import org.openspaces.grid.gsm.machines.exceptions.GridServiceAgentSlaEnforcementPendingContainerDeallocationException;
import org.openspaces.grid.gsm.machines.exceptions.MachinesSlaEnforcementInProgressException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.esm.component.SlaEnforcementTestUtils;
import framework.utils.ByonMachinesUtils;
import framework.utils.GsmTestUtils;

public class MachinesSlaEnforcementByonTest extends AbstractMachinesSlaEnforcementTest {
	

	@BeforeMethod(alwaysRun=true)
	public void beforeTest() {
		super.beforeTestInit();

		updateMachineProvisioningConfig(getMachineProvisioningConfig());
		machinesSlaEnforcement = new MachinesSlaEnforcement();
	}

	@BeforeClass(alwaysRun=true)
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}

	@AfterMethod(alwaysRun=true)
	public void afterTest() {
		machinesSlaEnforcement.destroyEndpoint(pu);
		pu.undeploy();

		try {
			machinesSlaEnforcement.destroy();
			stopMachines();
		} catch (Exception e) {
			Assert.fail("Failed to destroy machinesSlaEnforcement",e);
		}

		try {
			machineProvisioning.destroy();
		} catch (Exception e) {
			Assert.fail("Failed to destroy machineProvisioning",e);
		}
		super.afterTest();
	}

	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void oneMachineTest() throws InterruptedException  {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		// the first GSA is already started in BeginTest
		GridServiceAgent[] agentsBeforeEnforce = admin.getGridServiceAgents().getAgents();
		Assert.assertEquals(agentsBeforeEnforce.length,1);
		/*assertEquals("GS-10750 Expected GIGASPACES_TEST defined in XenUtils to be cached by admin API since it starts with JVMDetails#ENV_PREFIX",
				"ok",
				agentsBeforeEnforce[0].getVirtualMachine().getDetails().getEnvironmentVariables().get("GIGASPACES_TEST"));
		 */

		enforceNumberOfMachines(1);

		// there was already one GSA running
		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);

		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
		repetitiveAssertNumberOfGSAsRemoved(1, OPERATION_TIMEOUT);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void oneMachineNonDedicatedManagementMachinesTest() throws InterruptedException  {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));

		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		// the first GSA is already started in BeginTest
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);



		CapacityMachinesSlaPolicy sla = createSla(1);
		SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);

		// there was already one GSA running
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);

		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void oneMachineTestWithContainerWithWrongZone() throws Exception  {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));

		// the first GSA is already started in BeginTest
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		GridServiceAgent agent2 = ByonMachinesUtils.startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		GridServiceContainer container2 = agent2.startGridServiceAndWait(new GridServiceContainerOptions().
				vmInputArgument("-Dcom.gs.zones=" + WRONG_ZONE));

		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		// the first GSA is already started in BeginTest
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);

		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceNumberOfMachines(1);

		// there was already one non-management GSA running, but its dedicated to management
		// and one machine with wrong zone
		// so a new machine was started
		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);

		GsmTestUtils.killContainer(container2);
		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		// ESM does not shutdown machines it hasn't started. And it started only one machine
		repetitiveAssertNumberOfGSAsRemoved(1, OPERATION_TIMEOUT);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void oneMachineTestWithContainerWithZone() throws Exception  {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
		// the first GSA is already started in BeginTest
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		GridServiceAgent agent2 = ByonMachinesUtils.startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		GridServiceContainer container = agent2.startGridServiceAndWait(new GridServiceContainerOptions().
				vmInputArgument("-Dcom.gs.zones=" + pu.getRequiredZones()[0]));


		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		Assert.assertEquals(admin.getGridServiceContainers().getContainers().length,1);
		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceNumberOfMachines(1);

		// there was already one GSA running with a container with the correct zone.
		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);

		GsmTestUtils.killContainer(container);
		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		// ESM shutdown machines it hasnt started since marked with autoshutdown
		repetitiveAssertNumberOfGSAsRemoved(1, OPERATION_TIMEOUT);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = false)
	public void minimumNumberOfMachinesTest() throws InterruptedException  {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
    	
		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		/*
    	CapacityMachinesSlaPolicy sla = createSla(0);
    	sla.setMinimumNumberOfMachines(1);
		SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla);
    	// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
    	repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		 */
		enforceUndeploy();
		/*
        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        repetitiveAssertNumberOfGSAsRemoved(1, OPERATION_TIMEOUT);
		 */
	}


	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void twoMachinesTest() throws InterruptedException  {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
    	
		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceNumberOfMachines(2);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
		repetitiveAssertNumberOfGSAsRemoved(2, OPERATION_TIMEOUT);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void scaleOutMachinesTest() throws InterruptedException {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceNumberOfMachines(2);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceNumberOfMachines(3);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,4);
		repetitiveAssertNumberOfGSAsAdded(4, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(4, OPERATION_TIMEOUT);
		// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
		repetitiveAssertNumberOfGSAsRemoved(3, OPERATION_TIMEOUT);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void scaleInMachinesTest() throws InterruptedException, TimeoutException {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
    	
		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		enforceNumberOfMachines(2);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		enforceNumberOfMachines(1);

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(1, OPERATION_TIMEOUT);

		 GridServiceAgent[] discoveredMachines = machineProvisioning.getDiscoveredMachines(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		 Assert.assertEquals(discoveredMachines.length, 2);
		
		enforceUndeploy();

		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
		repetitiveAssertNumberOfGSAsRemoved(2, OPERATION_TIMEOUT);
	}



	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void scaleInMachinesWithContainersTest() throws InterruptedException, TimeoutException {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(10,1).addZone(ZONE));
    	
		endpoint = createEndpoint(pu, machinesSlaEnforcement);

		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		enforceNumberOfMachines(2);

		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

		// must set correct zone or machine is restricted for PU

		for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
			startContainerOnAgent(gsa);
		}

		//scale in
		final CapacityMachinesSlaPolicy sla = createSla(1);
		final AtomicBoolean evacuated = new AtomicBoolean(false);
		final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
		final CountDownLatch latch = new CountDownLatch(1);
		ScheduledFuture<?> scheduledTask = 
				((InternalAdmin)admin).scheduleWithFixedDelayNonBlockingStateChange(
						new Runnable() {

							public void run() {


								try {
									SlaEnforcementTestUtils.updateSlaWithProvisionedAgents(admin, sla, machineProvisioning);
									endpoint.enforceSla(sla);
									latch.countDown();
								}
								catch (GridServiceAgentSlaEnforcementPendingContainerDeallocationException e) {
									if(!evacuated.get()) {

										repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
										repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

										Collection<String> allocatedAgentsUids = null;
										allocatedAgentsUids = endpoint.getAllocatedCapacity(sla).getAgentUids();
										Assert.assertEquals(allocatedAgentsUids.size(),1);

										for (final GridServiceContainer container : admin.getGridServiceContainers()) {
											if (!allocatedAgentsUids.contains(container.getGridServiceAgent().getUid())) {
												((InternalAdmin)admin).scheduleAdminOperation(new Runnable() {

													public void run() {
														container.kill();
													}
												});
											}
										}
										evacuated.set(true);
									}
								}
								catch (MachinesSlaEnforcementInProgressException e) {
									//try again next time
								}
								catch (Throwable e) {
									ex.set(e);
									latch.countDown();
								}
							}
						}, 

						0, 10, TimeUnit.SECONDS);

		try {
			latch.await();
			if (ex.get() != null) {
				if (ex.get() instanceof java.lang.AssertionError) {
					throw (java.lang.AssertionError)ex.get();
				}
				Assert.fail("Exception in enforceSla",ex.get());
			}
		}
		finally {
			scheduledTask.cancel(false);
		}

		Assert.assertTrue(evacuated.get());
		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(1, OPERATION_TIMEOUT);
		machineProvisioning.getDiscoveredMachines(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		while(admin.getGridServiceContainers().getSize() > 0) {
			GridServiceContainer container = admin.getGridServiceContainers().iterator().next();
			GsmTestUtils.killContainer(container);
		}

		enforceUndeploy();

		Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);

		repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
		// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
		repetitiveAssertNumberOfGSAsRemoved(2, OPERATION_TIMEOUT);
	}
	
}
