package framework.utils;
import static org.testng.AssertJUnit.assertTrue;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;


import com.gigaspaces.cluster.activeelection.SpaceMode;

import framework.utils.AssertUtils.RepetitiveConditionProvider;

/**
 * Utility methods for asserting distribution after deployment.
 * 
 * @author Moran Avigdor
 */
public class DistributionUtils {
	/**
	 * assert that services are evenly distributed across machines/containers
	 */
	public static void assertEvenlyDistributed(Admin admin) {
		int numberOfServices = 0;
		for (ProcessingUnit pu : admin.getProcessingUnits()) {
			numberOfServices += pu.getTotalNumberOfInstances();
		}

		int numberOfContainers = admin.getGridServiceContainers().getSize();
		
		int nServicesPerContainer = (int) Math.ceil(1.0 * numberOfServices / numberOfContainers);
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		for (GridServiceContainer container : gridServiceContainers) {
			int nServices = container.getProcessingUnitInstances().length;
			assertTrue("Non evenly distributed, expected: maximum "
					+ nServicesPerContainer + " per container, actual: "
					+ nServices, nServicesPerContainer >= nServices);
		}
	}
	
	/**
	 * assert that primaries are evenly distributed across machines
	 */
	public static void assertEvenlyDistributedPrimaries(Admin admin) {
		int nPrimaries = 0;
		ProcessingUnit[] processingUnits = admin.getProcessingUnits().getProcessingUnits();
		for (ProcessingUnit pu : processingUnits) {
			nPrimaries += pu.getPartitions().length;
		}
		
		/*
		 * since there can be more machines which are not participating in the test,
		 * count only the machines with GSCs
		 */
		int nMachines = 0;
		for (Machine machine : admin.getMachines()) {
			if (!machine.getGridServiceContainers().isEmpty()) {
				++nMachines;
			}
		}

		int nPrimariesPerMachine = (int) Math.ceil(1.0 * nPrimaries / nMachines);
		
		for (Machine machine : admin.getMachines()) {
			int nCurrentPrimariesInMachine = 0;
			ProcessingUnitInstance[] processingUnitInstances = machine.getProcessingUnitInstances();
			for (ProcessingUnitInstance puInstance : processingUnitInstances) {
                puInstance.waitForSpaceInstance();
				if (puInstance.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) {
					++nCurrentPrimariesInMachine;
				}
			}
			
			assertTrue("Non evenly distributed primaries, expected: maximum "
					+ nPrimariesPerMachine + " per machine, actual: "
					+ nCurrentPrimariesInMachine,
					nCurrentPrimariesInMachine <= nPrimariesPerMachine);
		}
	}
	
	/**
	 * assert that the number of primaries matches the expected
	 */
	public static void assertPrimaries(final Admin admin, final int expected) {
		assertProcessingUnitInstancesSpaceMode(admin, expected, SpaceMode.PRIMARY);
    }
	
	public static void assertBackups(final Admin admin, final int expected) {
		assertProcessingUnitInstancesSpaceMode(admin, expected, SpaceMode.BACKUP);
    }

	private static void assertProcessingUnitInstancesSpaceMode(
			final Admin admin, final int expected, final SpaceMode spaceMode) {
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			public boolean getCondition() {
				int actual = 0;
		        for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
		        	for (ProcessingUnitInstance puInstance : gsc.getProcessingUnitInstances()){
		        		if (puInstance.getSpaceInstance() == null) {
		        			LogUtils.log("Waiting for " + ToStringUtils.puInstanceToString(puInstance) + " space instance to be discovered");
		        		} else if (spaceMode.equals(puInstance.getSpaceInstance().getMode())) {
		        			actual++;
		        		}
		        	}
		        }
		        LogUtils.log("Waiting for " + expected +" " + spaceMode+". Actual = " + actual);
		        return expected == actual;
		        
			}
		};
		
		AssertUtils.repetitiveAssertTrue("Expected " + expected +" "+spaceMode +" ", condition, 60*1000);
	}
}
