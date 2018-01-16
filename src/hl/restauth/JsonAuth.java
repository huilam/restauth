package hl.restauth;
import org.json.JSONObject;

import hl.restauth.accessctrl.AccessConfig;
import hl.restauth.auth.JsonUser;

public class JsonAuth extends JSONObject{
	
	public static final String _AUTH_INDENTITY	= "auth.ident";
	public static final String _CONSUMER 		= "consumer";
	public static final String _PROVIDER 		= "provider";
	public static final String _RESOURCE 		= "resource";
	
	public static final String _AUTHTOKEN 		= JsonUser._AUTHTOKEN;
	public static final String _IP 		= AccessConfig._CFG_IP;
	public static final String _UID 	= AccessConfig._CFG_UID;
	public static final String _ROLES 	= AccessConfig._CFG_ROLE;
	//
	public static final String _HTTP_METHOD		= AccessConfig._CFG_HTTP_METHOD;
	public static final String _ENDPOINT_URL	= AccessConfig._CFG_ENDPOINT_URL;
	//
	
	private JSONObject jsonConsumer = null;
	private JSONObject jsonProvider = null;
	
	private JSONObject jsonResource = null;
	
	public JsonAuth()
	{
		super();
		init();
	}
	
	public JsonAuth(String aJsonString)
	{
		super(aJsonString);
	}
	//
	private void init()
	{
		if(jsonConsumer==null)
		{
			jsonConsumer = new JSONObject();
			jsonConsumer.put(_AUTH_INDENTITY, _CONSUMER);
		}
		
		if(jsonProvider==null)
		{
			jsonProvider = new JSONObject();
			jsonProvider.put(_AUTH_INDENTITY, _PROVIDER);
		}
		
		if(jsonResource==null)
		{
			jsonResource = new JSONObject();
			jsonResource.put(_AUTH_INDENTITY, _RESOURCE);
		}
		
		put(_PROVIDER, jsonProvider);
		put(_CONSUMER, jsonConsumer);
		put(_RESOURCE, jsonResource);
	}
	//
	private String getAttr(JSONObject aJson, String aAttrName)
	{
		JSONObject json = aJson;
		
		if(json.has(aAttrName))
			return json.getString(aAttrName);
		else
			return null;
	}

	private void setAttr(JSONObject aJson, String aAttrName, Object aAttrObj)
	{
		if(aAttrObj==null)
			aAttrObj = JSONObject.NULL;
		aJson.put(aAttrName, aAttrObj);
	}	
	//////////////////////////////////////
	private String getResourceAttr(String aAttrName)
	{
		return getAttr(jsonResource, aAttrName);
	}
	
	private void setResourceAttr(String aAttrName, Object aAttrObj)
	{
		setAttr(jsonResource, aAttrName, aAttrObj);
	}
	//
	private String getProviderAttr(String aAttrName)
	{
		return getAttr(jsonProvider, aAttrName);
	}
	
	private void setProviderAttr(String aAttrName, Object aAttrObj)
	{
		setAttr(jsonProvider, aAttrName, aAttrObj);
	}
	//
	private String getConsumerAttr(String aAttrName)
	{
		return getAttr(jsonConsumer, aAttrName);
	}
	
	private void setConsumerAttr(String aAttrName, Object aAttrObj)
	{
		setAttr(jsonConsumer, aAttrName, aAttrObj);
	}
	//
	public JSONObject getConsumer()
	{
		return jsonConsumer;
	}

	public JSONObject getProvider()
	{
		return jsonProvider;
	}	
	//
	
	public String getConsumerIP()
	{
		return getConsumerAttr(_IP);
	}
	
	public String getConsumerAuthToken()
	{
		return getConsumerAttr(_AUTHTOKEN);
	}
	
	public String getConsumerUID()
	{
		return getConsumerAttr(_UID);
	}
	
	public String getConsumerRoles()
	{
		return getConsumerAttr(_ROLES);
	}
	
	public void setConsumerIP(Object aObject)
	{
		setConsumerAttr(_IP, aObject);
	}
	
	public void setConsumerAuthToken(Object aObject)
	{
		setConsumerAttr(_AUTHTOKEN, aObject);
	}
	
	public void setConsumerUID(Object aObject)
	{
		setConsumerAttr(_UID, aObject);
	}
	
	public void setConsumerRoles(Object aObject)
	{
		setConsumerAttr(_ROLES, aObject);
	}		
	//----------------------------
	
	public String getProviderIP()
	{
		return getProviderAttr(_IP);
	}
	
	public String getProviderUID()
	{
		return getProviderAttr(_UID);
	}
	
	public String getProviderRoles()
	{
		return getProviderAttr(_ROLES);
	}
	
	//
	public void setProviderIP(Object aObject)
	{
		setProviderAttr(_IP, aObject);
	}
	
	public void setProviderUID(Object aObject)
	{
		setProviderAttr(_UID, aObject);
	}
	
	public void setProviderRoles(Object aObject)
	{
		setProviderAttr(_ROLES, aObject);
	}
	//
	
	public void setResourceEndpointURL(Object aObject)
	{
		setResourceAttr(_ENDPOINT_URL, aObject);
	}
	
	public void setResourceHttpMethod(Object aObject)
	{
		setResourceAttr(_HTTP_METHOD, aObject);
	}
	
	public String getResourceEndpointURL()
	{
		return getResourceAttr(_ENDPOINT_URL);
	}
	
	public String getResourceHttpMethod()
	{
		return getResourceAttr(_HTTP_METHOD);
	}	
}
