import java.io.File;
import java.rmi.*;
import java.util.*;
public class ClientRequest {
    public static void main(String args[])
    {
        String IP="10.0.0.245";
        String port="1234";
        String fileName="myFile.txt";
        String createAnswer;
        String writeAnswer;
        String readAnswer;
        try
        {
            // lookup method to find reference of remote object
            FileSystem access = (FileSystem) Naming.lookup("rmi://"+IP+":"+port+"/usingRMI");
            createAnswer= access.createFile(fileName);
            Scanner scan=new Scanner(System.in);
            System.out.println("Start Writing....");
            String ans=scan.nextLine();
            writeAnswer= access.writeFile(fileName,ans);
            readAnswer= access.readFile(fileName);
            System.out.println("createAnswer: "+createAnswer);
            System.out.println("writeAnswer: "+writeAnswer);
            System.out.println("readAnswer: "+readAnswer);


        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}
