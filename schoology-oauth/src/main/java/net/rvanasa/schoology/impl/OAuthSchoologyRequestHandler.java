package net.rvanasa.schoology.impl;

import java.util.Date;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.rvanasa.schoology.SchoologyContentType;
import net.rvanasa.schoology.SchoologyRequestHandler;
import net.rvanasa.schoology.SchoologyResponse;
import net.rvanasa.schoology.adapters.SchoologyBooleanAdapter;
import net.rvanasa.schoology.adapters.SchoologyCourseSubjectAreaAdapter;
import net.rvanasa.schoology.adapters.SchoologyGenderAdapter;
import net.rvanasa.schoology.adapters.SchoologyGradeRangeAdapter;
import net.rvanasa.schoology.adapters.SchoologyUnixTimestampAdapter;
import net.rvanasa.schoology.obj.courses.SchoologyCourse;
import net.rvanasa.schoology.obj.courses.SchoologyCourseSubjectAreaEnum;
import net.rvanasa.schoology.obj.courses.SchoologyGradeRangeEnum;
import net.rvanasa.schoology.obj.groups.SchoologyGroup;
import net.rvanasa.schoology.obj.schools.SchoologySchool;
import net.rvanasa.schoology.obj.schools.buildings.SchoologyBuilding;
import net.rvanasa.schoology.obj.sections.SchoologyCourseSection;
import net.rvanasa.schoology.obj.users.SchoologyGenderEnum;
import net.rvanasa.schoology.obj.users.SchoologyUser;

public class OAuthSchoologyRequestHandler implements SchoologyRequestHandler
{
	public static OAuthService createService(SchoologyResourceLocator locator, String key, String secret)
	{
		return new ServiceBuilder()
				.provider(new OAuthSchoologyApi(locator))
				.apiKey(key)
				.apiSecret(secret)
				.signatureType(SignatureType.Header)
				.build();
	}
	
	private final SchoologyResourceLocator resourceLocator;
	
	private final OAuthService service;
	
	private Gson gson;
	
	private SchoologyContentType contentType = SchoologyContentTypeEnum.JSON;
	
	private Token accessToken;
	
	public OAuthSchoologyRequestHandler(SchoologyResourceLocator locator, String key, String secret)
	{
		this(locator, createService(locator, key, secret));
	}
	
	public OAuthSchoologyRequestHandler(String domain, String key, String secret)
	{
		this(new SchoologyResourceLocator(domain), key, secret);
	}
	
	public OAuthSchoologyRequestHandler(SchoologyResourceLocator locator, OAuthService service)
	{
		this.resourceLocator = locator;
		this.service = service;
		
		//TODO: Reflection to auto register all classes from adapter package?
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(boolean.class, new SchoologyBooleanAdapter());
		builder.registerTypeAdapter(SchoologyCourseSubjectAreaEnum.class, new SchoologyCourseSubjectAreaAdapter());
		builder.registerTypeAdapter(SchoologyGradeRangeEnum.class, new SchoologyGradeRangeAdapter());
		builder.registerTypeAdapter(SchoologyGenderEnum.class, new SchoologyGenderAdapter());
		builder.registerTypeAdapter(Date.class, new SchoologyUnixTimestampAdapter());
		gson = builder.create();
	}
	
	public SchoologyResourceLocator getResourceLocator()
	{
		return resourceLocator;
	}
	
	public OAuthService getOAuthService()
	{
		return service;
	}
	
	public SchoologyContentType getContentType()
	{
		return contentType;
	}
	
	public void setContentType(SchoologyContentType contentType)
	{
		this.contentType = contentType;
	}
	
	public Token getAccessToken()
	{
		return accessToken;
	}
	
	public void setAccessToken(Token accessToken)
	{
		this.accessToken = accessToken;
	}
	
	public OAuthRequest prepareRequest(Verb verb, String resource)
	{
		OAuthRequest request = new OAuthRequest(verb, getResourceLocator().getRequestUrl(resource));
		getOAuthService().signRequest(getAccessToken() != null ? getAccessToken() : Token.empty(), request);
		request.addHeader("Accept", getContentType().getID());
		request.addHeader("Content-Type", getContentType().getID());
		return request;
	}
	
	public SchoologyResponse prepareResponse(Response response)
	{
		return new BasicSchoologyResponse(
				SchoologyResponseStatusEnum.getStatus(response.getCode()),
				new BasicSchoologyResponseBody(getContentType(), response.getBody()),
				new BasicSchoologyResponseHeaders(response.getHeaders()));
	}
	
	@Override
	public SchoologyResponse get(String resource)
	{
		OAuthRequest request = prepareRequest(Verb.GET, resource);
		Response response = request.send();
		
		return prepareResponse(response);
	}
	
	@Override
	public SchoologyResponse multiget(String... resources)
	{
		String payload = "<?xml version='1.0' encoding='utf-8'?><requests>";
		for(String resource : resources)
		{
			payload += "<request>/v1/" + resource + "</request>";
		}
		payload += "</requests>";
		
		OAuthRequest request = prepareRequest(Verb.POST, "multiget");
		request.addHeader("Content-Type", "text/xml");
		request.addPayload(payload);
		
		Response response = request.send();
		
		return prepareResponse(response);
	}
	
	@Override
	public SchoologyResponse post(String resource, String body)
	{
		OAuthRequest request = prepareRequest(Verb.POST, resource);
		request.addPayload(body);
		
		Response response = request.send();
		
		return prepareResponse(response);
	}
	
	@Override
	public SchoologyResponse put(String resource, String body)
	{
		OAuthRequest request = prepareRequest(Verb.PUT, resource);
		request.addPayload(body);
		
		Response response = request.send();
		
		return prepareResponse(response);
	}
	
	@Override
	public SchoologyResponse delete(String resource)
	{
		OAuthRequest request = prepareRequest(Verb.DELETE, resource);
		
		Response response = request.send();
		
		return prepareResponse(response);
	}
	
	@Override
	public SchoologyResponse options(String resource)
	{
		OAuthRequest request = prepareRequest(Verb.OPTIONS, resource);
		Response response = request.send();
		
		return prepareResponse(response);
	}
	
	/*
	 * Java object implementations
	 */
	@Override
	public SchoologyUser[] getUsers()
	{
		//TODO:
		return null;
	}
	
	@Override
	public SchoologyUser getUser(String uid)
	{
		SchoologyResponse response = get("users/" + uid).requireSuccess();
		
		return gson.fromJson(response.getBody().getRawData(), SchoologyUser.class);
	}
	
	@Override
	public SchoologyGroup[] getGroups()
	{
		SchoologyResponse response = get("groups").requireSuccess();
		
		return gson.fromJson(response.getBody().parse().get("group").asRawData(), SchoologyGroup[].class);
	}
			
	@Override
	public SchoologyGroup getGroup(String group_id)
	{
		SchoologyResponse response = get("groups/" + group_id).requireSuccess();
		
		return gson.fromJson(response.getBody().getRawData(), SchoologyGroup.class);
	}
	
	@Override
	public SchoologyCourse[] getCourses()
	{
		SchoologyResponse response = get("courses").requireSuccess();
		
		return gson.fromJson(response.getBody().parse().get("course").asRawData(), SchoologyCourse[].class);
	}
	
	@Override
	public SchoologyCourse getCourse(String course_id)
	{
		SchoologyResponse response = get("courses/" + course_id).requireSuccess();
		
		return gson.fromJson(response.getBody().getRawData(), SchoologyCourse.class);
	}
	
	@Override
	public SchoologyCourseSection[] getCourseSections(String course_id)
	{
		//TODO:
		return null;
	}
	
	@Override
	public SchoologyCourseSection getCourseSection(String section_id)
	{
		SchoologyResponse response = get("sections/" + section_id).requireSuccess();
		
		return gson.fromJson(response.getBody().getRawData(), SchoologyCourseSection.class);
	}
	
	@Override
	public SchoologySchool[] getSchools()
	{
		SchoologyResponse response = get("schools").requireSuccess();
		
		return gson.fromJson(response.getBody().parse().get("school").asRawData(), SchoologySchool[].class);
	}
	
	@Override
	public SchoologySchool getSchool(String school_id)
	{
		SchoologyResponse response = get("schools/" + school_id).requireSuccess();
		
		return gson.fromJson(response.getBody().getRawData(), SchoologySchool.class);
	}
	
	@Override
	public SchoologyBuilding[] getBuildings(String school_id)
	{
		SchoologyResponse response = get("schools/" + school_id + "/buildings").requireSuccess();
		
		return gson.fromJson(response.getBody().parse().get("building").asRawData(), SchoologyBuilding[].class);
	}
	
}
