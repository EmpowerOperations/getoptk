package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 2017-04-17.
 */

class HelpOptionFixture {

    val ` ` = " "

    @Test fun `when asking for --help on simple class should produce good message by default`(){
        val ex = assertThrows<HelpException> { arrayOf("--help").parsedAs("prog.exe") { SimpleHelpableOptionSet() } }
        
        //TBD: how do we specify what to do with outputs?
        // more importantly, if we dont use exception flow then we need a valid instance of the object
        // without having hit any options, required or not.
        // This means that every single option regardless of its required-status must have a valid default.

        //TBD: the exact format. some to consider below.
        assertThat(ex.message).isEqualTo("""
              |usage: prog.exe
              | -f,--first <decimal>         the first value that is to be tested
              | -sec,--second <int>          Lorem ipsum dolor sit amet, consectetur adipiscing${` `}
              |                                elit, sed do eiusmod tempor incididunt ut labore${` `}
              |                                et dolore magna aliqua. Ut enim ad minim veniam,${` `}
              |                                quis nostrud exercitation ullamco laboris nisi ut
              | -h,--help
            """.trimMargin().replace("\n", System.lineSeparator()).trim()
        )
    }

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

    @Test fun `when dealing with boolean options should still properly format`(){
        val ex = assertThrows<HelpException> { arrayOf("--help").parsedAs("prog.exe") { SimpleHelpableOptionSetWithBoolean() } }

        assertThat(ex.message).isEqualTo("""
              |usage: prog.exe
              | -f,--first <decimal>         the first value that is to be tested
              | -sec,--second                Lorem ipsum dolor sit amet, consectetur adipiscing${` `}
              |                                elit, sed do eiusmod tempor incididunt ut labore${` `}
              |                                et dolore magna aliqua. Ut enim ad minim veniam,${` `}
              |                                quis nostrud exercitation ullamco laboris nisi ut
              | -h,--help
            """.trimMargin().replace("\n", System.lineSeparator()).trim()
        )
    }

    class SimpleHelpableOptionSetWithBoolean: CLI(){

        val first: Double by getValueOpt {
            description = "the first value that is to be tested"
        }

        val second: Boolean by getFlagOpt {
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                    "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                    "nostrud exercitation ullamco laboris nisi ut"

            shortName = "sec"
        }
    }
    @Test fun `when dealing with long names should still properly format`(){
        val ex = assertThrows<HelpException> { arrayOf("--help").parsedAs("prog.exe") { HelpableOptionSetWithLongName() } }

        assertThat(ex.message).isEqualTo("""
              |usage: prog.exe
              | -f,--first <decimal>         the first value that is to be tested
              | -blargwargl,                 Lorem ipsum dolor sit amet, consectetur adipiscing${` `}
              | --super-deduper-long-name      elit, sed do eiusmod tempor incididunt ut labore${` `}
              | <decimal>                      et dolore magna aliqua. Ut enim ad minim veniam,${` `}
              |                                quis nostrud exercitation ullamco laboris nisi ut${` `}
              | -b,                          Surprisingly short description${` `}
              | --super-deduper-deduper-real
              | ly-long-name
              | <decimal>
              | -a,--a <int>
              | -h,--help
            """.trimMargin().replace("\n", System.lineSeparator()).trim()
        )
    }

    class HelpableOptionSetWithLongName: CLI(){

        val first: Double by getValueOpt {
            description = "the first value that is to be tested"
        }

        val superDeduperLongName: Double by getValueOpt {
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                    "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                    "nostrud exercitation ullamco laboris nisi ut"

            longName = "super-deduper-long-name"
            shortName = "blargwargl"
        }

        val anotherSuperDeduperLongName: Int by getValueOpt {
            description = "Surprisingly short description"
            shortName = "b"
            longName = "super-deduper-deduper-really-long-name"
        }

        val a: Int by getValueOpt()
    }
}


/*

 [ Apache CLI: https://commons.apache.org/proper/commons-cli/ ]

usage: prog.exe
 -A,--almost-all          do not list implied . and ..
 -a,--all                 do not hide entries starting with .
 -B,--ignore-backups      do not list implied entried ending with ~
 -b,--escape              print octal escapes for nongraphic characters
    --block-size <SIZE>   use SIZE-byte blocks
 -c                       with -lt: sort by, and show, ctime (time of last
                          modification of file status information) with
                          -l:show ctime and sort by name otherwise: sort
                          by ctime
 -C                       list entries by columns

 [ GNU ]
 ls --help
Usage: ls [OPTION]... [FILE]...
List information about the FILEs (the current directory by default).
Sort entries alphabetically if none of -cftuvSUX nor --sort.
Mandatory arguments to long options are mandatory for short options too.
  -a, --all                  do not ignore entries starting with .
  -A, --almost-all           do not list implied . and ..
      --author               with -l, print the author of each file
  -b, --escape               print octal escapes for nongraphic characters
      --block-size=SIZE      use SIZE-byte blocks.  See SIZE format below
  -B, --ignore-backups       do not list implied entries ending with ~
  -c                         with -lt: sort by, and show, ctime (time of last
                               modification of file status information)
                               with -l: show ctime and sort by name
                               otherwise: sort by ctime
  -C                         list entries by columns
      --color[=WHEN]         colorize the output.  WHEN defaults to `always'
                               or can be `never' or `auto'.  More info below
  -d, --directory            list directory entries instead of contents,
                               and do not dereference symbolic links
  -D, --dired                generate output designed for Emacs' dired mode
  -f                         do not sort, enable -aU, disable -ls --color
  -F, --classify             append indicator (one of *=>@|) to entries

 [ powershell ]


> powershell /?

PowerShell[.exe] [-PSConsoleFile <file> | -Version <version>]
    [-NoLogo] [-NoExit] [-Sta] [-Mta] [-NoProfile] [-NonInteractive]
    [-InputFormat {Text | XML}] [-OutputFormat {Text | XML}]
    [-WindowStyle <style>] [-EncodedCommand <Base64EncodedCommand>]
    [-ConfigurationName <string>]
    [-File <filePath> <args>] [-ExecutionPolicy <ExecutionPolicy>]
    [-Command { - | <script-block> [-args <arg-array>]
                  | <string> [<CommandParameters>] } ]

PowerShell[.exe] -Help | -? | /?

-PSConsoleFile
    Loads the specified Windows PowerShell console file. To create a console
    file, use Export-Console in Windows PowerShell.

-Version
    Starts the specified version of Windows PowerShell.
    Enter a version number with the parameter, such as "-version 2.0".

-NoLogo
    Hides the copyright banner at startup.

-NoExit
    Does not exit after running startup commands.


 [ JCommander ]
> kobaltw --help
_  __          _               _   _
| |/ /   ___   | |__     __ _  | | | |_
| ' /   / _ \  | '_ \   / _` | | | | __|
| . \  | (_) | | |_) | | (_| | | | | |_
|_|\_\  \___/  |_.__/   \__,_| |_|  \__|  1.0.68

Usage: <main class> [options]
  Options:
    -bf, --buildFile
      The build file
      Default: kobalt/src/Build.kt
    --checkVersions
      Check if there are any newer versions of the dependencies
      Default: false
    --client

      Default: false
    --dev
      Turn on dev mode, resulting in a more verbose log output
      Default: false
    --download
      Force a download from the downloadUrl in the wrapper
      Default: false


 [ msbuild ]
> msbuild /?
Microsoft (R) Build Engine version 12.0.31101.0
[Microsoft .NET Framework, version 4.0.30319.42000]
Copyright (C) Microsoft Corporation. All rights reserved.

Syntax:              MSBuild.exe [options] [project file]

Description:         Builds the specified targets in the project file. If
                     a project file is not specified, MSBuild searches the
                     current working directory for a file that has a file
                     extension that ends in "proj" and uses that file.

Switches:

  /target:<targets>  Build these targets in this project. Use a semicolon or a
                     comma to separate multiple targets, or specify each
                     target separately. (Short form: /t)
                     Example:
                       /target:Resources;Compile

  /property:<n>=<v>  Set or override these project-level properties. <n> is
                     the property name, and <v> is the property value. Use a
                     semicolon or a comma to separate multiple properties, or
                     specify each property separately. (Short form: /p)
                     Example:
                       /property:WarningLevel=2;OutDir=bin\Debug\



*/