package mainly.user;

import mainly.Flag;
import mainly.Main;

@Main("mainly.user.OrderedMain")
public class Ordered {

    @Flag(order=1)
    String from;

    @Flag(order=2)
    String dest;

    public void run() {
        System.out.println("Copying, or whatever, from " + from + " to " + dest + ".");
    }

}