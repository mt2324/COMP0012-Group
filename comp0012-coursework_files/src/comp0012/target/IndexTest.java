package comp0012.target;
public class IndexTest{
    public int indexmethod()
    {
        int a = 0;
        int b = 0;
        for (int i = 0; i <5; i++)
        {
            a = a + i;
            b = b + i*i;
        }
        return a+b;
    }

}
