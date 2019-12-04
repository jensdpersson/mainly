package mainly.user;

import mainly.Flag;
import mainly.Main;

@Main("mainly.user.BothMain")
public class Both {

    @Flag(names={"--noop", "-n"}, help="just show, don't do")
    boolean name;

    @Flag(names={"--num"})
    int num;
    
    @Flag(order=1)
    String from;

    @Flag(order=2)
    String dest;

    public void run() {
        int fac = factorial(num, 1);
        System.out.println("Hello, " + name + "! The factorial of " + num + " is " + fac);
        System.out.println("Copying, or whatever, from " + from + " to " + dest + ".");
    }

    int factorial(int num, int result) {
        if (num == 0) {
            return result;
        }
        return factorial(num-1, result*num);
    }

}