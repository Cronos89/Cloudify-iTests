package test.esm.stateful.manual.memory;

import java.io.File;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;


import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.DeploymentUtils;
import framework.utils.GsmTestUtils;

public class DedicatedManualXenStatefulFailoverTest extends AbstractFromXenToByonGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testElasticStatefulProcessingUnitDeploymentWithKillGSC() {       
		// make sure no gscs yet created
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

		//get pu dir
		File puDir = DeploymentUtils.getArchive("processorPU.jar"); 
		
		final ProcessingUnit pu = super.deploy(
				new ElasticStatefulProcessingUnitDeployment(puDir)
				.maxMemoryCapacity(1024*4, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
				.scale(new ManualCapacityScaleConfigurer()
				.memoryCapacity(1024, MemoryUnit.MEGABYTES)
				.create()) 
				.dedicatedMachineProvisioning(getMachineProvisioningConfig())
				);

		int expectedNumberOfMachines = 2;
		int expectedNumberOfContainers = 4;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu,expectedNumberOfContainers,expectedNumberOfMachines,OPERATION_TIMEOUT);

		repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);


		final int NUM_OF_POJOS = 1000;
		GsmTestUtils.writeData(pu, NUM_OF_POJOS);

		int expectedNumberOfMachinesRemoved = 0;
		int expectedNumberOfContainersRemoved = 1;

		GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
		GsmTestUtils.killContainer(container);
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu,expectedNumberOfContainers,expectedNumberOfMachines,OPERATION_TIMEOUT);

		repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachinesRemoved+expectedNumberOfMachines, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSAsRemoved(expectedNumberOfMachinesRemoved, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainersRemoved+expectedNumberOfContainers, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsRemoved(expectedNumberOfContainersRemoved, OPERATION_TIMEOUT);       

		// make sure all pojos handled by PUs
		assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));

		assertUndeployAndWait(pu);
	}

}
