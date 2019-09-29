package net.rvanasa.schoology.adapters;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.rvanasa.schoology.obj.courses.SchoologyGradeRangeEnum;

public class SchoologyGradeRangeAdapter implements JsonDeserializer<SchoologyGradeRangeEnum>
{

	@Override
	public SchoologyGradeRangeEnum deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
			throws JsonParseException {
		return SchoologyGradeRangeEnum.getGradeLevel(json.getAsInt());
	}

}
