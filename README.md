# getoptk #
the most expressive command line parsing utility for kotlin

_getoptk_ a command line parsing library for kotlin. It leverages standard conventions and a few kotlin-specific language features to reduce the amount of CLI boilerplate.

_getoptk is still very much in flux, and looking for feedback. All APIs subject to change_

## Getting Started ##
 
A simple program using getoptk would be:
 
```kotlin
class CustomerConfig: CLI(){
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

### Default Error Messages ###

`getoptk` generates nice error messages out of the gate:

```kt
class SimpleHelpableOptionSet: CLI(){

    val first: Double by getValueOpt {
        description = "the first value that is to be tested"
    }

    val second: Int by getValueOpt {
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                "nostrud exercitation ullamco laboris nisi ut"

        shortName = "sec"
    }
}
```

Running the standard `--help` (or `-h` or `/?`) flag will generate a standard Apache-CLI 80-column 
usage description: 

```cmd
> prog.exe --help

usage: prog.exe
 -f,--first <decimal>         the first value that is to be tested
 -sec,--second <int>          Lorem ipsum dolor sit amet, consectetur adipiscing
                                elit, sed do eiusmod tempor incididunt ut labore
                                et dolore magna aliqua. Ut enim ad minim veniam,
                                quis nostrud exercitation ullamco laboris nisi ut
```

By default, all problems are thrown as exceptions, be it a configuration error (eg two options with the same name), parse error (eg "unrecognized option"), and help message request. This can be

### Advanced Configuration ###

Using the configuration & customization components of getoptk, we can accept more complicated command line arguments with more complicated parsing and converting techniques:
 
```kotlin
class CLIConfig: CLI(){
  val alphaFactor: Double by getValueOpt()
    //defaults are "-a" and "--alphaFactor", a default of 0.0, and a description that summarizes this  

  val betaFactor: Int by getValueOpt {
    shortName = "e"      
    longName = "beta"
    description = "the group's average tee-shirt size"
    default = 1
    converter = { argText ->
      when(argText.toUpperCase()){
        "S" -> 1
        "M" -> 2
        "L" -> 3
        "XL" -> 4
        else -> 0
      }
    } 
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

get it from [JCenter](https://bintray.com/empower-operations-team/maven/getoptk)

```groovy
compile "com.empowerops:getoptk:0.1"
```

