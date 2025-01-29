# ATM

## Problem Statement

You are asked to develop a Command Line Interface (CLI) to simulate an interaction of an ATM with a retail bank.The submission must include an executable `start.sh` file located at the root. When executed, this file should start the application and provide a CLI as demonstrated below. The `start.sh` is executed in an environment fully configured for Java (with the `java` executable in the `PATH` and `JAVA_HOME` environment variable pointing to the `JDK`), Node.js (with the `npm` and `node` executables in the `PATH`), and Dart (with the Dart SDK available and its executables in the `PATH`). All SDKs are set to their latest versions. The environment also has internet access, allowing package management tools to download additional dependencies as needed (e.g., `./gradlew build`, `./mvnw package`, `npm install`, `dart pub get`).

Each time `start.sh` is executed, it must create a new environment without reusing any data from previous runs. For instance, if `start.sh` is executed, and new users are created via the CLI, then the process is stopped and `start.sh` is executed again, the application should start fresh with no users registered.

## Commands

* `login [name]` - Logs in as this customer and creates the customer if not exist
* `deposit [amount]` - Deposits this amount to the logged in customer
* `withdraw [amount]` - Withdraws this amount from the logged in customer
* `transfer [target] [amount]` - Transfers this amount from the logged in customer to the target customer
* `logout` - Logs out of the current customer

## Example Session

Your console output should contain at least the following output depending on the scenario and commands. But feel free 
to add extra output as you see fit.

```bash
$ login Alice
Hello, Alice!
Your balance is $0

$ deposit 100
Your balance is $100

$ logout
Goodbye, Alice!

$ login Bob
Hello, Bob!
Your balance is $0

$ deposit 80
Your balance is $80

$ transfer Alice 50
Transferred $50 to Alice
your balance is $30

$ transfer Alice 100
Transferred $30 to Alice
Your balance is $0
Owed $70 to Alice

$ deposit 30
Transferred $30 to Alice
Your balance is $0
Owed $40 to Alice

$ logout
Goodbye, Bob!

$ login Alice
Hello, Alice!
Your balance is $210
Owed $40 from Bob

$ transfer Bob 30
Your balance is $210
Owed $10 from Bob

$ logout
Goodbye, Alice!

$ login Bob
Hello, Bob!
Your balance is $0
Owed $10 to Alice

$ deposit 100
Transferred $10 to Alice
Your balance is $90

$ logout
Goodbye, Bob!
```
