package android.net;
public final class Uri {
    private final String value;private Uri(String v){value=v;}
    public static Uri parse(String v){return new Uri(v);}
    private int colon(){return value.indexOf(':');}
    public boolean isHierarchical(){int p=colon();return p<0||(p+1<value.length()&&value.charAt(p+1)=='/');}
    public String getScheme(){int p=colon();return p<0?null:value.substring(0,p);}
    private String authority(){if(!isHierarchical())return null;int p=colon()+1;if(!value.startsWith("//",p))return null;p+=2;int end=value.length();for(char c:new char[]{'/','?','#'}){int n=value.indexOf(c,p);if(n>=0)end=Math.min(end,n);}return value.substring(p,end);}
    public String getHost(){String a=authority();if(a==null)return null;int p=a.lastIndexOf('@');if(p>=0)a=a.substring(p+1);p=a.indexOf(':');return decode(p<0?a:a.substring(0,p));}
    public String getPath(){if(!isHierarchical())return null;int p=colon()+1;String a=authority();if(a!=null)p+=2+a.length();int end=value.length();for(char c:new char[]{'?','#'}){int n=value.indexOf(c,p);if(n>=0)end=Math.min(end,n);}return decode(value.substring(p,end));}
    public String getQueryParameter(String key){if(!isHierarchical())throw new UnsupportedOperationException();int p=value.indexOf('?');if(p<0)return null;int end=value.indexOf('#',p);String q=value.substring(p+1,end<0?value.length():end);for(String part:q.split("&",-1)){int n=part.indexOf('=');if(decode(n<0?part:part.substring(0,n)).equals(key))return decode(n<0?"":part.substring(n+1));}return null;}
    private static String decode(String value){try{return java.net.URLDecoder.decode(value,"UTF-8");}catch(Exception e){return value;}}
    @Override public String toString(){return value;}
}
