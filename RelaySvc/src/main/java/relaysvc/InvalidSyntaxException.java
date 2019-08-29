package relaysvc;

public class InvalidSyntaxException
  extends NLException
{
  public InvalidSyntaxException(String paramString)
  {
    super(paramString);
  }
  
  public InvalidSyntaxException(String paramString, Object paramObject)
  {
    super(paramString, paramObject);
  }
  
  public InvalidSyntaxException(String paramString, Object[] paramArrayOfObject)
  {
    super(paramString, paramArrayOfObject);
  }
}


/* Location:              E:\JAVA\ojdbc6.jar!\oracle\net\jdbc\nl\InvalidSyntaxException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */