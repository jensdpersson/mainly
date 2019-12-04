package mainly.user;

import mainly.Flag;
import mainly.Main;

@Main("mainly.user.NamedMain")
public class Named {

    @Flag(names={"--name", "-n"}, help="the name of the helloee")
    String name;

    @Flag(names={"--num"})
    int num;

    public void run() {
        int fac = factorial(num, 1);
        System.out.println("Hello, " + name + "! The factorial of " + num + " is " + fac);
    }

    int factorial(int num, int result) {
        if (num == 0) {
            return result;
        }
        return factorial(num-1, result*num);
    }

}