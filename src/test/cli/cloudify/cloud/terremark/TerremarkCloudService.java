package test.cli.cloudify.cloud.terremark;

import java.io.IOException;

import test.cli.cloudify.cloud.AbstractCloudService;

public class TerremarkCloudService extends AbstractCloudService {

	private static TerremarkCloudService self = null;

	private TerremarkCloudService() {};

	public static TerremarkCloudService getService() {
		if (self == null) {
			self = new TerremarkCloudService();
		}
		return self;	
	}
	
	@Override
	public String getCloudName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getApiKey() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void injectAuthenticationDetails() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	
}