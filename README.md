# getoptk
the most expressive command line parsing utility for kotlin

_getoptk_ a command line parsing library for kotlin. It leverages standard conventions and a few kotlin-specific language features to reduce the amount of CLI boilerplate to near-zero.
 
A simple program using getoptk would be:
 
```kotlin
fun main(args: Array<String>){
  val config = args.parsedAs { CLIConfig() }
  
  //...
}

class CLIConfig: CLI {
  val alphaFactor: Double by getOpt()
    //defaults are "-a" and "--alphaFactor", a default of 0.0, and a description that summarizes this  
    
  val betaFactor: Int by getOpt {
    shortName = "e"      
    longName = "beta"
    description = "the number of people shouting from the rooftops"
    default = 1
  }
  
  val customerNames: List<String> by getListOpt {
    listStructure = ParseMode.varargs //default is CSV
    longName = "customers"    
  }
}
```

which could then be used from the invoked from the command like this:

```sh
program -a 1.3 --beta 42 --customers Jane Mary
```

TODO:
- [ ] help printing (steal JCommander'errorMessage?)
- [ ] error reporting
- [ ] `required` fields
- [ ] thread safety & removal of nasty static registry `...getoptk.RegisteredOptions`
- [ ] data-class destructuring
- [ ] `@configFile.txt` style load-arguments-from-file
- [ ] parser recovery (allign with next SupertokenSeparator boundary)