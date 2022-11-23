package org.pygisserver.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
* json转xml工具类、xml转json(value封装一层content)
*
* */
@Slf4j
public class JsonUtil {

    public static String json2Xml(String json){
        try {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            JSONObject jsonObject = JSON.parseObject(json);
            json2Xmlstr(jsonObject, buffer);
            return buffer.toString();
        }catch (Exception e){
            log.error("JsonUtil json2Xml has error {}",e.getLocalizedMessage());
            throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"JSON转换为XML");
        }
    }


    private static String json2Xmlstr(JSONObject jsonObject,StringBuffer buffer){
        Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
        for(Iterator<Map.Entry<String, Object>> iterator = entries.iterator();iterator.hasNext();){
            Map.Entry<String, Object> next = iterator.next();
            if (("com.alibaba.fastjson.JSONObject").equals(next.getValue().getClass().getName())){
                buffer.append("<"+next.getKey()+">");
                JSONObject jo = jsonObject.getJSONObject(next.getKey());
                json2Xmlstr(jo,buffer);
                buffer.append("</"+next.getKey()+">");
            }else if (("com.alibaba.fastjson.JSONArray").equals(next.getValue().getClass().getName())){
                JSONArray jsonArray = jsonObject.getJSONArray(next.getKey());
                for (int i = 0; i < jsonArray.size() ; i++) {
                    buffer.append("<"+next.getKey()+">");
                    Object jo = jsonArray.get(i);
                    if (jo instanceof JSONObject){
                        json2Xmlstr((JSONObject)jo,buffer);
                        buffer.append("</"+next.getKey()+">");
                    }else if (jo instanceof String){
                        buffer.append(jo);
                        buffer.append("</"+next.getKey()+">");
                    }
                }
            }else if (("java.lang.String").equals(next.getValue().getClass().getName())){
                buffer.append("<"+next.getKey()+">"+next.getValue());
                buffer.append("</"+next.getKey()+">");
            }
        }
        return buffer.toString();
    }

    public static JSONObject xml2Json(Object xmlObj){
        try {
            JSONObject result = new JSONObject(true);
            SAXReader xmlReader = new SAXReader();
            Document document = null;
            if (xmlObj instanceof String){
                document = xmlReader.read(new StringReader(xmlObj.toString()));
            }else if (xmlObj instanceof Reader){
                document = xmlReader.read((Reader) xmlObj);
            }
            if (document != null){
                Element element = document.getRootElement();
                return xml2Json(element,result);
            }else {
                return null;
            }
        }catch (Exception e){
            log.error("JsonUtil xml2Json has error {}",e.getLocalizedMessage());
            throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"XML转换为JSON");
        }
    }

    private static JSONObject xml2Json(Element element, JSONObject object){
        List<Element> elements = element.elements();
        for (Element child : elements){
            Object value = object.get(child.getName());
            Object newValue;
            if (child.elements().size() > 0){
                JSONObject jsonObject = xml2Json(child, new JSONObject(true));
                if (!jsonObject.isEmpty()){
                    newValue = jsonObject;
                }else {
                    newValue = child.getText();
                }
            }else {
                newValue = child.getText();
            }
            List<Attribute> attributes = child.attributes();
            if (!attributes.isEmpty()){
                JSONObject attrJsonObject = new JSONObject();
                for (Attribute attribute : attributes){
                    attrJsonObject.put(attribute.getName(),attribute.getText());
                    //子级命名为content
                    attrJsonObject.put("content",newValue);
                }
                newValue = attrJsonObject;
            }
            if (newValue != null){
                if (value != null){
                    if (value instanceof JSONArray){
                        ((JSONArray)value).add(newValue);
                    }else {
                        JSONArray array = new JSONArray();
                        array.add(value);
                        array.add(newValue);
                        object.put(child.getName(),array);
                    }
                }else {
                    object.put(child.getName(),newValue);
                }
            }
        }
        return object;
    }

}
