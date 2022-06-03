public class MyPair
{
    private final String key;
    private  String value;

    public MyPair(String aKey, String aValue)
    {
        key   = aKey;
        value = aValue;
    }

    public String getKey()   { return key; }
    public String getValue() { return value; }
    public void setValue(String value) { 
    	this.value=value;
    			}
    public String toString(){
    	return this.key + " "+ this.value;
    }
    public String toStringColon(){
    	return this.key + " : "+ this.value;
    }
}