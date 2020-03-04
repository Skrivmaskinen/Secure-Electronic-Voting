//package CLA;

public class ValidationNumber {
	protected int n;
	private boolean valid;
	
	public ValidationNumber(int in_n)
	{
		n = in_n;
		valid = true;
	}
	
	public boolean Validate()
	{
		boolean temp = valid;
		valid = false;
		return temp;
	}
}
