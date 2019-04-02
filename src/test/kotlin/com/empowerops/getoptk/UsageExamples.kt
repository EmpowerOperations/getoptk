package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 2016-10-26.
 */
class UsageExamples {

    @Test fun `when using usage example created initially should parse properly`(){
        //setup
        val args = arrayOf("--helloString", "Hello_getoptk!")

        //act
        val instance = args.parsedAs("prog") { SimpleImpl() }
        val result = instance.helloString

        //assert
        assertThat(result).isEqualTo("Hello_getoptk!")
    }
    class SimpleImpl: CLI(){
        val helloString by getValueOpt<String>()
    }

    @Test fun `when parsing thing with two options should properly parse`(){

        //setup
        val args = arrayOf("--helloString", "Hello_getoptk!", "-o", "weird")

        //act
        val instance = args.parsedAs("prog") { TwoFieldImpl() }
        val helloStringResult = instance.helloString
        val oddString = instance.anotherString

        //assert
        assertThat(helloStringResult).isEqualTo("Hello_getoptk!")
        assertThat(oddString).isEqualTo("weird")
    }
    class TwoFieldImpl: CLI(){
        val helloString: String by getValueOpt()
        val anotherString: String by getValueOpt {
            longName = "oddball"
            shortName = "o"
        }
    }

    @Test fun `when parsing two options with assignment syntax should properly parse`(){
        //setup
        val args = arrayOf("--helloString=Hello_getoptk!", "-o=weird")

        //act
        val instance = args.parsedAs("prog") { TwoFieldImpl() }
        val helloStringResult = instance.helloString
        val oddString = instance.anotherString

        //assert
        assertThat(helloStringResult).isEqualTo("Hello_getoptk!")
        assertThat(oddString).isEqualTo("weird")
    }

    @Test fun `when parsing lists should properly split and parse`(){
        //setup
        val args = arrayOf("/ints=1,2,3")

        //act
        val instance = args.parsedAs("prog") { ListImpl() }

        //assert
        assertThat(instance.ints).isEqualTo(listOf(1, 2, 3))
    }
    class ListImpl: CLI(){
        val ints: List<Int> by getListOpt {
            parseMode = csv()
        }
    }

    @Test fun `when using varargs should properly split`(){
        //setup
        val args = arrayOf("--items", "first", "second", "third", "fourth")

        //act
        val instance = args.parsedAs("prog") { AnotherListImpl() }

        //assert
        assertThat(instance.items).isEqualTo(listOf("first", "second", "third", "fourth"))
    }
    class AnotherListImpl: CLI(){
        val items: List<String> by getListOpt<String> {
            parseMode = varargs()
        }
    }

    @Test fun `when using flag options should properly set values`(){
        //setup
        val args = arrayOf("-f")

        //act
        val instance = args.parsedAs("prog") { FlagImpl() }

        //assert
        assertThat(instance.silent).isFalse()
        assertThat(instance.force).isTrue()
    }
    class FlagImpl: CLI(){
        val silent by getFlagOpt()
        val force by getFlagOpt()
    }

    @Test fun `when using data class embedded in command line should properly read`(){
        //setup
        val args = arrayOf("--thingy", "bob", "1.234")

        //act
        val instance = args.parsedAs("prog") { ObjectHolder() }

        //assert
        assertThat(instance.thingy).isEqualTo(Thingy("bob", 1.234))
    }
    class ObjectHolder: CLI(){
        val thingy: Thingy by getOpt()
    }
    data class Thingy(val name: String, val value: Double)

    @Test fun `when using data class nullably embedded in command line should properly read`(){
        //setup
        val args = arrayOf("--thingy", "bob", "1.234")

        //act
        val instance = args.parsedAs("prog") { ObjectHolder() }

        //assert
        assertThat(instance.thingy).isEqualTo(Thingy("bob", 1.234))
    }
    class NullableObjectHolder: CLI(){
        val thingy: Thingy? by getNullableOpt()
    }

    @Test fun `when using tree nested data classes should properly project and read`(){
        //setup
        val args = arrayOf("--thingyParent", "bob", "1.234", "2")

        //act
        val instance = args.parsedAs("prog") { TreeHolder() }

        //assert
        assertThat(instance.thingyParent).isEqualTo(ThingyParent(Thingy("bob", 1.234), 2))
    }

    class TreeHolder: CLI() {
        val thingyParent: ThingyParent by getOpt()
    }
    data class ThingyParent(val thingy: Thingy, val factor: Int)


    @Test fun `when using list of domain objects should properly project and read`(){
        //setup
        val args = arrayOf("--things", "frodo", "8000", "sam", "9000")

        //act
        val instance = args.parsedAs("prog") { ListOfObjectsImpl() }

        //assert
        assertThat(instance.things).isEqualTo(listOf(Thingy("frodo", 8000.0), Thingy("sam", 9000.0)))
    }
    class ListOfObjectsImpl : CLI(){
        val things: List<Thingy> by getListOpt {
            parseMode = ImplicitObjects()
        }
    }

    @Test fun `when using a list of tree like domain objects should properly parse`(){
        //setup
        val args = arrayOf("/things", "frodo", "8000", "1", "sam", "9000", "2")

        //act
        val instance = args.parsedAs("prog") { ListOfTreeObjectsImpl() }

        //assert
        assertThat(instance.things).isEqualTo(listOf(
                ThingyParent(Thingy("frodo", 8000.0), 1),
                ThingyParent(Thingy("sam", 9000.0), 2)
        ))
    }
    class ListOfTreeObjectsImpl: CLI(){
        val things: List<ThingyParent> by getListOpt {
            parseMode = ImplicitObjects()
        }
    }


    @Test fun `when parsing type with valueOf method`(){
        //setup
        val args = arrayOf("--name", "bob")

        //act
        val instance = args.parsedAs("prog") { ValueOfAbleCLI() }

        //assert
        assertThat(instance.name).isEqualTo(ValueOfAble("bob_thingy"))
    }
    class ValueOfAbleCLI : CLI(){
        val name: ValueOfAble by getValueOpt()
    }
    data class ValueOfAble(val nameImpl: String) {
        companion object {
            fun valueOf(str: String) = ValueOfAble(str + "_thingy")
        }
    }

    @Test fun `when parsing into statically available object should properly parse`(){
        //setup
        val args = arrayOf("-x", "1,2,3", "--something", "hello-objects")

        //act
        val instance = args.parsedAs("prog.exe") { StaticCLI }

        //assert
        assertThat(instance.something).isEqualTo("hello-objects")
        assertThat(instance.nums).containsExactly(1, 2, 3)
    }
    object StaticCLI: CLI(){
        val something: String by getValueOpt()

        val nums: List<Int> by getListOpt{
            shortName = "x"
            parseMode = csv()
        }
    }

    @Test fun `when parsing type with custom converter should properly convert`(){
        //setup
        val args = arrayOf("--name", "bob")

        //act
        val instance = args.parsedAs("prog") { ConvertableCLI() }

        //assert
        assertThat(instance.name).isEqualTo(ConvertableDataClass("got value 'bob'"))
    }
    class ConvertableCLI : CLI(){

        val name: ConvertableDataClass by getValueOpt {
            converter = { argText -> ConvertableDataClass("got value '$argText'") }
        }
    }
    data class ConvertableDataClass(val nameImpl: String)

    @Test fun `when parsing tee-shirt sizes with custom converter should properly convert`(){
        val args = arrayOf("--beta", "XL")

        //act
        val instance = args.parsedAs("prog") { DemoUseCaseCLI() }

        //assert
        assertThat(instance.betaFactor).isEqualTo(4)
    }

    class DemoUseCaseCLI: CLI(){

        val betaFactor: Int by getValueOpt {
            shortName = "e"
            longName = "beta"
            description = "the group's average tee-shirt size"
            converter = { argText ->
                when (argText.toUpperCase()) {
                    "S" -> 1
                    "M" -> 2
                    "L" -> 3
                    "XL" -> 4
                    "XXL" -> 5
                    else -> 0
                }
            }
        }
    }

    @Test fun `when using defaulted values should properly parse`(){
        //setup
        val args = emptyArray<String>()

        //act
        val instance = args.parsedAs("prog") { HeavilyDefaultedCLI() }

        //assert
        with(instance){
            assertThat(nullable).isNull()
            assertThat(nonNullable).isEqualTo(SimpleDTO(1, 2.0))
            assertThat(valueType).isEqualTo(0.0)
        }
    }
    class HeavilyDefaultedCLI: CLI(){
        val nullable: SimpleDTO? by getNullableOpt()
        val nonNullable: SimpleDTO by getOpt {
            default = SimpleDTO(1, 2.0)
            isRequired = false
            shortName = "no"
        }
        val valueType: Double by getValueOpt {
            isRequired = false
        }
    }
    data class SimpleDTO(val first: Int, val second: Double)


    @Test fun `when using non-requiured complex object should properly call constructor for default value`(){
        val args = emptyArray<String>()

        //act
        val result = args.parsedAs("prog") { NonDefaultedCLI() }

        //assert
        assertThat(result.nonnullable).isEqualTo(SimpleDTO(0, 0.0))
    }
    class NonDefaultedCLI: CLI(){
        val nonnullable: SimpleDTO by getOpt{
            isRequired = false
        }
    }

    @Test fun `when using git like component with sub commands should properly parse`(){
        val args = arrayOf("lfs", "--initialize")

        val command: GitCLI = args.parsedAs("git") { GitCLI() }

        assertThat(command.subCommand).isInstanceOf(GitSubCommand.Lfs::class.java)
        assertThat((command.subCommand as GitSubCommand.Lfs).initialize).isTrue()
    }
    class GitCLI: CLI() {
        val subCommand: GitSubCommand by getSubcommandOpt()
    }

    sealed class GitSubCommand: Subcommand(){

        class Lfs: GitSubCommand() {
            val initialize: Boolean by getFlagOpt()
        }
        class Checkout: GitSubCommand() {
//            val message: String by getOpt()
        }
    }

    @Test fun `when using git like component with bad commands should properly parse`(){
        val args = arrayOf("lfs", "--hogwarts", "9.75")

        //act
        val ex = assertThrows<ParseFailedException> { args.parsedAs("git") { GitCLI() } }

        //assert
        assertThat(ex.message).isEqualTo("""
            com.empowerops.getoptk.ParseFailedException: Failure in command line:

            unknown option 'hogwarts', expected 'initialize'
            git lfs --hogwarts 9.75
            at:       ~~~~~~~~

            usage: git lfs
             -i,--initialize
        """.trimIndent())
    }

    @Test fun `when using better git like component with sub commands should properly parse`(){
        val args = arrayOf("lfs", "--initialize")

        val command: BetterGitCLI = args.parsedAs("git") { BetterGitCLI() }

        assertThat(command.subCommand).isInstanceOf(GitSubCommand.Lfs::class.java)
        assertThat((command.subCommand as GitSubCommand.Lfs).initialize).isTrue()
    }

    class BetterGitCLI: CLI() {
        val subCommand: BetterGitSubCommand by getSubcommandOpt {
            registerCommand<BetterGitSubCommand.Lfs>("lfs")
            registerCommand<BetterGitSubCommand.Checkout>("checkout")
        }
    }

    sealed class BetterGitSubCommand: Subcommand(){
        data class Lfs(val initialize: Boolean): BetterGitSubCommand()
        data class Checkout(val branchName: String): BetterGitSubCommand()
    }

    @Test fun `when using better git like component with bad commands should properly parse`(){
        val args = arrayOf("lfs", "--hogwarts", "9.75")

        //act
        val ex = assertThrows<ParseFailedException> { args.parsedAs("git") { BetterGitCLI() } }

        //assert
        assertThat(ex.message).isEqualTo("""
            com.empowerops.getoptk.ParseFailedException: Failure in command line:

            unknown option 'hogwarts', expected 'initialize'
            git lfs --hogwarts 9.75
            at:       ~~~~~~~~

            usage: git lfs
             -i,--initialize
        """.trimIndent())
    }
}

