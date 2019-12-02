# mainly

A set of annotations and an annotation processor to generate main classes with built-in argument handling.

```java
@Main("example.ThisMainClassWillBeGenerated")
class HelloWorld {

  //The generated main class will pick apart the arg vector and set the flagged fields
  @Flag(names={"--name", "-n"}, help="whom to greet")
  String name;

  // And then run this method to let the app do its business.
  public void run() {
    System.out.println("Hello, " + this.name + "!");
  }

}
```
