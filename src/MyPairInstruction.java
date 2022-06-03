public class MyPairInstruction
{
    private final String key;
    private  String[] value;

    public MyPairInstruction(String aKey, String[] aValue)
    {
        key   = aKey;
        value = aValue;
    }

    public String getKey()   { return key; }
    public String[] getValue() { return value; }
    public void setValue(String[] value) { 
    	this.value=value;
    }

    public String toStringColon(){
    	String inst = "";
    	for(int i = 0;i<this.value.length;i++)
    		inst+=this.value[i] +" ";
    	return this.key + " : "+ inst;
    }
}