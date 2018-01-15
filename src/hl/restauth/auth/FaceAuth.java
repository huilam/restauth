package hl.restauth.auth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import hl.common.ImgUtil;
import hl.common.http.HttpResp;
import hl.common.http.RestApiUtil;

public class FaceAuth {

    public JsonUser faceMatching(JsonUser aJsonUser, String aUserAttemptPwd, JSONObject aJsonConfig) throws IOException
    {
    	StringBuffer sbPOIMatchingInfo = new StringBuffer();
    	String sUserID = aJsonUser.getUserID();
    	
		String sCompareRestApiUrl 		= aJsonConfig.getString(AuthConfig._FACEAPI_COMPARE_URL);
		String sCompareContentType 		= aJsonConfig.getString(AuthConfig._FACEAPI_COMPARE_POST_CONTENTTYPE);
		String sCompareContentTemplate 	= aJsonConfig.getString(AuthConfig._FACEAPI_COMPARE_POST_TEMPLATE);	
		String sCompareTargetTemplatePath = aJsonConfig.getString(AuthConfig._FACEAPI_COMPARE_TARGET);
		
    	aUserAttemptPwd = ImgUtil.removeBase64Header(AuthMgr.deobfuscate(aUserAttemptPwd));    	
    	String sAttemptFaceFeature = extractFaceFeature(aUserAttemptPwd, aJsonConfig);
    	if(sAttemptFaceFeature!=null)
    	{
    		aUserAttemptPwd = sAttemptFaceFeature;
    	}
		
		List<String> listCompareTargetDataPaths = new ArrayList<String>();
		
		if(sCompareTargetTemplatePath!=null)
		{
			sCompareTargetTemplatePath = sCompareTargetTemplatePath.replaceAll(
					AuthMgr.RegexEscapedParam(AuthMgr.KEY_USERID), sUserID);
			
			sCompareContentTemplate = sCompareContentTemplate.replaceAll(
					AuthMgr.RegexEscapedParam(AuthMgr.KEY_PASSWORD), 
					aUserAttemptPwd);
			
			File f = new File(sCompareTargetTemplatePath);
			
			if(f.isDirectory())
			{
				for(File fileFace : f.listFiles())
				{
					if(fileFace.getName().toLowerCase().endsWith(".base64"))
						listCompareTargetDataPaths.add(fileFace.getAbsolutePath());
				}
			}
			else
			{
				listCompareTargetDataPaths.add(f.getAbsolutePath());
			}
		
		}
		
		String sFaceMatchedTargetPath = null;
		boolean isRetryWIthFlippedImage = aJsonConfig.getBoolean(AuthConfig._FACEAPI_COMPARE_RETRY_HFLIP);
		double iCompareThreshold 		= aJsonConfig.getDouble(AuthConfig._FACEAPI_COMPARE_THRESHOLD);
		
		boolean isMatched 	= false;
		for(String sCompareTargetPath : listCompareTargetDataPaths)
		{
			if(!isMatched)
			{
				
				String sTargetFeature0 = getFaceFeature(sCompareTargetPath, aJsonConfig, false);
				
				String[] sCompareTargetDatas = new String[] {sTargetFeature0, null};
				
				boolean isHFlipped 	= false;
				for(String sCompareTargetData : sCompareTargetDatas)
				{
					if(sCompareTargetData!=null)
					{
	    				sCompareTargetData = ImgUtil.removeBase64Header(sCompareTargetData);
	    				
	    				String sContentBody = sCompareContentTemplate.replaceAll(
	    							AuthMgr.RegexEscapedParam(AuthConfig._FACEAPI_COMPARE_TARGET), 
	    							sCompareTargetData);
	    				
	    				if(sContentBody.indexOf(AuthMgr.PARAM_PREFIX)>-1)
	    				{
	    					throw new IOException("Invalid post content : "+sContentBody);
	    				}
	    				
	    				try {
	    					long lStartTime = System.currentTimeMillis();
							HttpResp httpReq = RestApiUtil.httpPost(sCompareRestApiUrl, sCompareContentType, sContentBody);
							long lElapsedTime = System.currentTimeMillis()-lStartTime;
							
							if(httpReq.getHttp_status()>=200 && httpReq.getHttp_status()<=299)
							{
								JSONObject json = new JSONObject(httpReq.getContent_data());
								double iCompareResultScore 	= json.getDouble(AuthConfig._FACEAPI_COMPARE_RESULT_SCORE);
								if(iCompareResultScore>=iCompareThreshold)
								{
									sFaceMatchedTargetPath = sCompareTargetPath;
									isMatched = true;
								}
								
								if(isMatched)
								{
									sbPOIMatchingInfo.append("* [matched]: ");
								}
								else
								{
									sbPOIMatchingInfo.append("* [failed-skip]: ");
								}
								
								sbPOIMatchingInfo.append(new File(sCompareTargetPath).getName());
								if(isHFlipped)
								{
									sbPOIMatchingInfo.append(" (hflip)");
								}
								sbPOIMatchingInfo.append(" : actual:").append(iCompareResultScore).append(" -vs- threhold:").append(iCompareThreshold);
								sbPOIMatchingInfo.append("  (elapsed:").append(lElapsedTime).append("ms)");
								sbPOIMatchingInfo.append("\n");
								
								if(isMatched)
									break;
							}
							
		    				if(isRetryWIthFlippedImage)
		    				{
		    					isHFlipped = true;	
		    					String sTargetFeature1 = getFaceFeature(sCompareTargetPath, aJsonConfig, true);
		    					if(sTargetFeature1==null)
		    					{
		    						sCompareTargetDatas[1] = sTargetFeature1;
		    					}
		    					
		    				}
							
						} catch (Exception e) {
							
							JSONObject json = null;
							try{
								json = new JSONObject(sContentBody);
								String srcImg = json.getString("SourceFeature");
								String tarImg = json.getString("TargetFeature");
								json.put("SourceFeature", srcImg.substring(0, 30)+" (more ...)");
								json.put("TargetFeature", tarImg.substring(0, 30)+" (more ...)");
								sContentBody = json.toString();
							}catch(Exception ex) { ex.printStackTrace();}
							
								throw new IOException("Error occur when comsuming "+sCompareRestApiUrl+"\n"+sCompareContentType+"\n"+sContentBody, e);
						}
					}
				}
			}
		}
		
		System.out.println(sbPOIMatchingInfo.toString());
		
		if(sFaceMatchedTargetPath==null)
		{
			if(sbPOIMatchingInfo.length()>0)
			{
				throw new IOException(sbPOIMatchingInfo.toString());
			}
			return null;
		}
		else
		{
			if(!aJsonUser.has(JsonUser._ROLES))
			{
				sFaceMatchedTargetPath = new File(sFaceMatchedTargetPath).getName();
				int iExtPos = sFaceMatchedTargetPath.lastIndexOf(".");
				if(iExtPos>-1)
				{
					String sMatchedUserID = sFaceMatchedTargetPath.substring(0, iExtPos);
					JsonUser jsonMatchedUser = AuthMgr.getUser(sMatchedUserID);
					if(jsonMatchedUser!=null && jsonMatchedUser.has(JsonUser._ROLES))
					{
						jsonMatchedUser.setUserAgent(aJsonUser.getUserName());
						jsonMatchedUser.setAuthType(aJsonUser.getAuthType());
						aJsonUser = jsonMatchedUser;
					}
				}
			}
		}
	
		return aJsonUser;
    }
    
    private String getFaceFeature(String aImageBase64Path, JSONObject aJsonConfig, boolean isFlipped) throws IOException
    {
    	File fileImgBase64 = new File(aImageBase64Path);
    	File fileCache = new File(fileImgBase64.getParentFile().getAbsolutePath()+"/.cache/"+fileImgBase64.getName()+"."+(isFlipped?"1":"0"));
    	
    	if(fileCache.exists() && fileCache.lastModified() < fileImgBase64.lastModified())
    	{
    		fileCache.delete();
    	}
    		
    	if(!fileCache.exists())
    	{
    		fileCache.getParentFile().mkdirs();
			String sExtractedFeature = extractFaceFeature(ImgUtil.getBase64FromFile(fileImgBase64), aJsonConfig);
			if(sExtractedFeature!=null)
			{
				ImgUtil.writeBase64ToFile(fileCache, sExtractedFeature);
			}
			return sExtractedFeature;
    	}
    	return ImgUtil.getBase64FromFile(fileCache);
    }
    
    private String extractFaceFeature(String aImgBase64, JSONObject aJsonConfig)
    {
    	if(!aJsonConfig.has(AuthConfig._FACEAPI_EXTRACT_URL)
    		|| !aJsonConfig.has(AuthConfig._FACEAPI_EXTRACT_POST_RETURN_ATTR)
    		|| !aJsonConfig.has(AuthConfig._FACEAPI_EXTRACT_POST_CONTENTTYPE)
    		|| !aJsonConfig.has(AuthConfig._FACEAPI_EXTRACT_POST_TEMPLATE))
    		return null;
    	
		String sExtractRestApiUrl 		= aJsonConfig.getString(AuthConfig._FACEAPI_EXTRACT_URL);
		String sExtractContentType 		= aJsonConfig.getString(AuthConfig._FACEAPI_EXTRACT_POST_CONTENTTYPE);
		String sExtractContentTemplate 	= aJsonConfig.getString(AuthConfig._FACEAPI_EXTRACT_POST_TEMPLATE);
    	String sExtractReturnAttr 		= aJsonConfig.getString(AuthConfig._FACEAPI_EXTRACT_POST_RETURN_ATTR);

    	if(sExtractRestApiUrl.trim().length()==0 
    			|| sExtractContentTemplate.trim().length()==0 
    			|| sExtractReturnAttr.trim().length()==0)
    		return null;
    	
    	aImgBase64 = ImgUtil.removeBase64Header(aImgBase64);
    	
		String sExtractContentBody = sExtractContentTemplate.replaceAll(
				AuthMgr.RegexEscapedParam(AuthMgr.KEY_PASSWORD), aImgBase64);
		try {
			long lStartTime = System.currentTimeMillis();
			HttpResp httpReq = RestApiUtil.httpPost(sExtractRestApiUrl, sExtractContentType, sExtractContentBody);
			long lElapsedTime = System.currentTimeMillis()-lStartTime;

			if(httpReq.getHttp_status()>=200 && httpReq.getHttp_status()<=299)
			{
				String sData = httpReq.getContent_data().trim();
				System.out.println(" [extract-feature] "+lElapsedTime+"ms : "+sData);

				if(sData.startsWith("["))
				{
					JSONArray jsonArr = new JSONArray(sData);
					sData = jsonArr.getJSONObject(0).toString();
				}
				JSONObject json = new JSONObject(sData);
				sData = json.getString(sExtractReturnAttr);
				if(sData!=null)
					return sData;
				
			}
		}catch(Exception ex)
		{
			System.err.println(ex.getMessage());
		}
			
    	return null;
    }
}
