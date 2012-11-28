package test.cli.cloudify.cloud.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.cloudifysource.esc.jclouds.WindowsServerEC2ReviseParsedImage;
import org.jclouds.aws.ec2.compute.strategy.AWSEC2ReviseParsedImage;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.logging.jdk.config.JDKLoggingModule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import framework.utils.LogUtils;

public abstract class JCloudsCloudService extends AbstractCloudService {

	protected ComputeServiceContext context;
	
	public abstract void addOverrides(Properties overridesProps);

	public JCloudsCloudService(String cloudName) {
		super(cloudName);
	}

	protected ComputeServiceContext createContext() {
		final Set<Module> wiring = new HashSet<Module>();
		wiring.add(new AbstractModule() {
	
			@Override
			protected void configure() {
				bind(
						AWSEC2ReviseParsedImage.class).to(
						WindowsServerEC2ReviseParsedImage.class);
			}
	
		});
	
		// Enable logging using gs_logging
		wiring.add(new JDKLoggingModule());
	
		Properties overrides = new Properties();
		
		Set<Entry<String, Object>> entries = getCloud().getTemplates().get(getCloud().getConfiguration().getManagementMachineTemplate()).getOverrides().entrySet();
		for (Entry<String, Object> entry : entries) {
			overrides.setProperty(entry.getKey(), (String)entry.getValue());
		}
		
		addOverrides(overrides);	
		
		final String provider = getCloud().getProvider().getProvider();
		final String user = getCloud().getUser().getUser();
		final String key = getCloud().getUser().getApiKey();
		LogUtils.log("Creating jclouds context, this may take a few seconds");
		return new ComputeServiceContextFactory().createContext(
				provider, user, key, wiring, overrides);
	}

	@Override
	public boolean scanLeakedAgentAndManagementNodes() {
	
		if (this.context == null) {
			this.context = createContext();
		}
		
		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();
		final String managerPrefix = getCloud().getProvider().getManagementGroup();
	
		final List<ComputeMetadata> leakedNodes = checkForLeakedNodesWithPrefix(agentPrefix, managerPrefix);
	
		return leakedNodes.isEmpty();
	
	}

	@Override
	public void teardownCloud() throws IOException, InterruptedException {
		super.teardownCloud();
		LogUtils.log("Closing jclouds ComputeServiceContext");
		this.context.close();
		this.context = null;
	}

	@Override
	public boolean scanLeakedAgentNodes() {
		
		if (this.context == null) {
			this.context = createContext();
		}
		
		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();
	
		final List<ComputeMetadata> leakedNodes = checkForLeakedNodesWithPrefix(agentPrefix);
	
		return leakedNodes.isEmpty();
	
	}

	private List<ComputeMetadata> checkForLeakedNodesWithPrefix(final String... prefixes) {
		List<ComputeMetadata> leakedNodes = null;
		int iteration = 0;
		while (iteration < 3) {
			leakedNodes = new LinkedList<ComputeMetadata>();
			LogUtils.log("Checking for leaked nodes with prefix: " + Arrays.toString(prefixes));
			final Set<? extends ComputeMetadata> allNodes = this.context.getComputeService().listNodes();
			
			boolean foundPendingNodes = scanNodes(leakedNodes, allNodes,
					prefixes);
	
			if (!foundPendingNodes) {
				break; // no need to re-run the scan
			}
			try {
				LogUtils.log("Sleeping for several seconds to give pending nodes time to reach their new state");
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// ignore.
			}
			++iteration;
		}
	
		if (leakedNodes.size() > 0) {
			LogUtils.log("Killing leaked nodes");
			for (final ComputeMetadata leakedNode : leakedNodes) {
	
				LogUtils.log("Killing node: " + leakedNode.getName() + ": " + leakedNode);
				try {
					this.context.getComputeService().destroyNode(leakedNode.getId());
				} catch (final Exception e) {
					LogUtils.log("Failed to kill a leaked node. This machine may be leaking!", e);
				}
			}
	
		}
		return leakedNodes;
	}

	private boolean scanNodes(final List<ComputeMetadata> leakedNodes, final Set<? extends ComputeMetadata> allNodes, final String... prefixes) {
		boolean foundPendingNodes = false;
		for (final ComputeMetadata computeMetadata : allNodes) {
			final String name = computeMetadata.getName();
			final NodeState state = ((NodeMetadata) computeMetadata).getState();
			switch (state) {
			case TERMINATED:
				// ignore - node is shut down
				break;
			case PENDING:
			case ERROR:
			case RUNNING:
			case UNRECOGNIZED:
			case SUSPENDED:
			default:
				if (name == null || name.length() == 0) {						
					LogUtils.log("WARNING! Found a non-terminated node with an empty name. " 
							+ "The test can't tell if this is a leaked node or not! Node details: "
							+ computeMetadata);						
				} else {
					for (String prefix : prefixes) {
						if (name.startsWith(prefix)) {
							if (state.equals(NodeState.PENDING)) {
								// node is transitioning from one state to another 
								// 		- might be shutting down, might be starting up!
								// Must try again later.
								foundPendingNodes = true;
								LogUtils.log("While scanning for leaked nodes, found a node in state PENDING. " 
										+ "Leak scan will be retried. Node in state pending was: "
										+ computeMetadata);
							} else {
								LogUtils.log("ERROR: Found a leaking node: " + computeMetadata);
								leakedNodes.add(computeMetadata);
							}
						}
					}
	
				}
	
				break;
			}
		}
		return foundPendingNodes;
	}

}
