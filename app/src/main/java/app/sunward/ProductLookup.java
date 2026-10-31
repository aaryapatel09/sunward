package app.sunward;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class ProductLookup {
    static final class Product {
        final String code,name,brand,spf;
        Product(String code,String name,String brand){this.code=code;this.name=name;this.brand=brand;this.spf=ProductRules.suggestedSpf(name);}
        String displayName(){return brand.isEmpty()||name.toLowerCase(java.util.Locale.ROOT).contains(brand.toLowerCase(java.util.Locale.ROOT))?name:brand+" · "+name;}
    }
    static final class LookupException extends Exception {
        LookupException(String message){super(message);}
    }
    static Product parse(String body,String requested) throws LookupException {
        try{
            JSONObject root=new JSONObject(body);
            if(root.optInt("status",0)!=1)throw new LookupException("No match in Open Beauty Facts. You can enter the sunscreen details manually.");
            JSONObject item=root.optJSONObject("product");
            if(item==null)throw new LookupException("The product record is incomplete. Please enter the details manually.");
            String code=item.optString("code",root.optString("code",""));
            if(!ProductRules.sameCode(code,requested))throw new LookupException("The returned barcode does not match. Please enter the details manually.");
            String name=string(item,"product_name");if(name.isEmpty())name=string(item,"product_name_en");
            if(name.isEmpty())throw new LookupException("This barcode has no product name yet. Please enter the details manually.");
            return new Product(code,name,string(item,"brands"));
        }catch(JSONException e){throw new LookupException("The lookup service returned an unreadable response. Try again later or enter details manually.");}
    }
    private static String string(JSONObject item,String key){
        if(item.isNull(key))return "";Object value=item.opt(key);if(!(value instanceof String))return "";
        String result=((String)value).trim();return result.length()>240?result.substring(0,240):result;
    }
    static Product fetch(String code) throws LookupException {
        if(!ProductRules.validCode(code))throw new LookupException("That barcode is invalid. Try scanning again or check the digits.");
        HttpURLConnection connection=null;
        try{
            URL url=new URL("https://world.openbeautyfacts.org/api/v2/product/"+code+".json?fields=code,product_name,product_name_en,brands");
            connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent","Sunward/1.3 (Android; https://github.com/aaryapatel09/sunward)");
            connection.setRequestProperty("Accept","application/json");
            int status=connection.getResponseCode();
            if(status==404)throw new LookupException("No match in Open Beauty Facts. You can enter the sunscreen details manually.");
            if(status==429)throw new LookupException("Too many lookups. Wait a minute and try again, or enter the details manually.");
            if(status!=200)throw new LookupException("Product lookup is temporarily unavailable. You can enter the details manually.");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()){
                byte[] buffer=new byte[4096];int count;
                while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException();out.write(buffer,0,count);if(out.size()>262144)throw new IOException();}
            }
            return parse(out.toString(StandardCharsets.UTF_8.name()),code);
        }catch(IOException e){throw new LookupException("Couldn’t reach the product database. Check your connection or enter the details manually.");}
        finally{if(connection!=null)connection.disconnect();}
    }
}
