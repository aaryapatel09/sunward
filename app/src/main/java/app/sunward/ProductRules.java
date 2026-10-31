package app.sunward;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Barcode and label parsing without Android dependencies. Never infer protection duration. */
final class ProductRules {
    private static final Pattern SPF=Pattern.compile("(?i)\\bSPF\\s*[-:]?\\s*(\\d{1,3})(?:\\s*\\+)?(?!\\d)");
    static String cleanCode(String raw){return raw==null?"":raw.replaceAll("[\\s-]","");}
    static boolean validCode(String code){
        if(code==null || !code.matches("(?:[0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14})"))return false;
        if(code.matches("0+"))return false;
        int sum=0;boolean triple=true;
        for(int i=code.length()-2;i>=0;i--){sum+=(code.charAt(i)-'0')*(triple?3:1);triple=!triple;}
        return (10-sum%10)%10==code.charAt(code.length()-1)-'0';
    }
    static String expandUpce(String raw){
        if(raw==null || !raw.matches("[01][0-9]{7}"))return "";
        String d=raw.substring(1,7),body;
        char last=d.charAt(5);
        if(last<='2')body=d.substring(0,2)+last+"0000"+d.substring(2,5);
        else if(last=='3')body=d.substring(0,3)+"00000"+d.substring(3,5);
        else if(last=='4')body=d.substring(0,4)+"00000"+d.charAt(4);
        else body=d.substring(0,5)+"0000"+last;
        return raw.charAt(0)+body+raw.charAt(7);
    }
    static boolean sameCode(String a,String b){
        if(!validCode(a)||!validCode(b))return false;
        return String.format(java.util.Locale.ROOT,"%14s",a).replace(' ','0').equals(String.format(java.util.Locale.ROOT,"%14s",b).replace(' ','0'));
    }
    static String suggestedSpf(String label){
        Matcher m=SPF.matcher(label==null?"":label);String result="";
        while(m.find()){
            if(label.substring(m.end()).matches("^\\s*[/–-]\\s*\\d.*"))return "";
            int value=Integer.parseInt(m.group(1));if(value<1||value>150)return "";
            String found=String.valueOf(value);if(!result.isEmpty()&&!result.equals(found))return "";result=found;
        }
        return result;
    }
}
