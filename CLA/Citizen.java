//package CLA;

public class Citizen {
	protected String usr;
	protected String psw;
	private boolean canVote;
	
	public Citizen(String in_usr, String in_psw)
	{
		this.usr = in_usr;
		this.psw = in_psw;
		this.canVote = true;
	}
	
	public boolean Validate()
	{
		boolean temp = canVote;
		canVote = false;
		return temp;
	}
	public String toString()
	{
		String status = "x";
		if (canVote)
		{
			status = "_";
		}
		return "[" + status + "] : " + usr;
	}
}
