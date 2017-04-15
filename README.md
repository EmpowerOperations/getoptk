# getoptk
the most expressive command line parsing utility for kotlin

_getoptk_ a command line parsing library for kotlin. It leverages standard conventions and a few kotlin-specific language features to reduce the amount of CLI boilerplate.

## Getting Started
 
A simple program using getoptk would be:
 
```kotlin
class CustomerConfig: CLI {
  val firstName: String by getValueOpt()
  val lastName: String by getValueOpt()
  val age: Int by getValueOpt() 
}

fun main(args: Array<String>){
  val config = args.parsedAs("customer.exe") { CustomerConfig() }
  
  println("first name :${config.firstName}, last name: ${config.lastName}, age; ${config.age}")
}
```

which can then be called like
 
```bash
customer.exe --firstName Bob -l Smith -a 27
```

which would produce the output

```
first name: Bob, last name: Smith, age: 27
```

Using the configuration & customization components of getoptk, we can accept more complicated command line arguments with more complicated parsing and converting techniques:
 
```kotlin
class CLIConfig: CLI {
  val alphaFactor: Double by getValueOpt()
    //defaults are "-a" and "--alphaFactor", a default of 0.0, and a description that summarizes this  

  val betaFactor: Int by getValueOpt {
    shortName = "e"      
    longName = "beta"
    description = "the number of people shouting from the rooftops"
    default = 1
    converter = MyCustomConverter() //(String) -> Int, arity must be exactly 1. 
  }

  val customerNames: List<String> by getListOpt {
    parseMode = varargs()
    longName = "customers"    
  }
  
  //getoptk can typically parse simpler classes (namely data classes) directly from the command line
  data class Address(val addressNo: Int, val street: String, val postalCode: String)
  
  val addresses: List<Address> by getListOpt {
    parseMode = ImplicitObjects()
  }
}
```    
     
which can then be called like
 
```bash
customer.exe -a 2.3 --beta 42 --addresses 221 Baker-St NW16XE 10 Downing-St SW12AA
```

which would produce an instance of CLIConfig with :
- an alphaFactor of 2.3
- a betaFactor of 42
- and a list of two addresses, one at 221 Baker-St, the other at 10 Downing-St.

you can find more examples in the [Usage Examples](https://github.com/EmpowerOperations/getoptk/blob/master/src/test/kotlin/com/empowerops/getoptk/UsageExample.kt) test suite. 

## adding getoptk

TBD: link to sonatype/maven central 

```groovy
compile "com.empowerops:getoptk:0.1"
```

